package com.salang.matching_poc.worker;

import com.salang.matching_poc.model.entity.MatchHistory;
import com.salang.matching_poc.model.enums.Status;
import com.salang.matching_poc.repository.MatchHistoryRepository;
import com.salang.matching_poc.service.RedisService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class MatchingWorker {

    private static final long TICK_INTERVAL_MS = 50L; // 50ms
    private static final int TOP_CANDIDATES_COUNT = 50;
    private static final int MAX_MATCHES_PER_TICK = 10; // 한 tick에서 최대 매칭 수
    private static final long MATCH_TIMEOUT_MS = 20_000L; // 20초 - 매칭 불가능한 사용자 자동 제거 (k6 TIMEOUT보다 짧게)
    private static final String WAITING_STATUS = Status.WAITING.name();

    private final RedisService redisService;
    private final MatchHistoryRepository matchHistoryRepository;
    private final MeterRegistry meterRegistry;

    private Thread workerThread;
    private volatile boolean running = false;

    // 메트릭
    private Timer workerTickLatencyTimer;
    private Counter matchSuccessCount;
    private Counter matchFailCount;
    @SuppressWarnings("unused") // Gauge는 자동으로 메트릭에 등록되므로 사용되지 않는다는 경고 무시
    private Gauge matchQueueLengthGauge;

    @PostConstruct
    public void start() {
        // 메트릭 초기화
        workerTickLatencyTimer = Timer.builder("matching_worker_tick_latency")
                .description("Worker 1 루프(50ms tick) 수행 시간")
                .register(meterRegistry);

        matchSuccessCount = Counter.builder("matching_match_success_count")
                .description("매칭 성공 횟수")
                .register(meterRegistry);

        matchFailCount = Counter.builder("matching_match_fail_count")
                .description("매칭 실패 횟수")
                .register(meterRegistry);

        matchQueueLengthGauge = Gauge.builder("matching_match_queue_length",
                () -> redisService.getQueueLength() != null ? redisService.getQueueLength().doubleValue() : 0.0)
                .description("Redis ZSET의 현재 큐 길이")
                .register(meterRegistry);

        running = true;
        workerThread = new Thread(this::run, "MatchingWorker");
        workerThread.setDaemon(true);
        workerThread.start();
        log.info("MatchingWorker started");
    }

    @PreDestroy
    public void stop() {
        running = false;
        if (workerThread != null) {
            try {
                workerThread.interrupt();
                workerThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        log.info("MatchingWorker stopped");
    }

    private void run() {
        while (running && !Thread.currentThread().isInterrupted()) {
            long startTime = System.nanoTime();
            try {
                processMatching();
            } catch (Exception e) {
                log.error("Error in matching worker tick", e);
            } finally {
                // Worker tick latency 측정
                long elapsed = System.nanoTime() - startTime;
                workerTickLatencyTimer.record(elapsed, TimeUnit.NANOSECONDS);
            }

            // 50ms 간격 유지
            long elapsedMs = (System.nanoTime() - startTime) / 1_000_000;
            long sleepTime = Math.max(0, TICK_INTERVAL_MS - elapsedMs);
            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    @SuppressWarnings("null") // userA와 userB는 null 체크 후 사용
    private void processMatching() {
        // Step 1: Top-50 후보 조회
        List<String> candidates = redisService.getTopCandidates(TOP_CANDIDATES_COUNT);
        if (candidates.isEmpty()) {
            matchFailCount.increment();
            return;
        }

        int matchCount = 0;

        // Step 2: 후보 필터링 및 매칭 시도 (한 tick에서 여러 쌍 매칭)
        for (int i = 0; i < candidates.size() && matchCount < MAX_MATCHES_PER_TICK; i++) {
            String userA = candidates.get(i);

            // 자기 자신 제외, WAITING 상태 확인
            if (!isWaiting(userA)) {
                continue;
            }

            // @SuppressWarnings("null"): getGender returns @Nullable but we check for null
            // immediately
            @SuppressWarnings("null")
            String genderA = redisService.getGender(userA);
            if (genderA == null) {
                continue;
            }

            // Step 3: 매칭 조건 검사 (성별이 다르면 매칭: 남자-여자)
            for (int j = i + 1; j < candidates.size() && matchCount < MAX_MATCHES_PER_TICK; j++) {
                String userB = candidates.get(j);

                // 자기 자신 제외, WAITING 상태 확인
                if (!isWaiting(userB)) {
                    continue;
                }

                // @SuppressWarnings("null"): getGender returns @Nullable but we check for null
                // immediately
                @SuppressWarnings("null")
                String genderB = redisService.getGender(userB);
                if (genderB == null) {
                    continue;
                }

                // 성별이 다르면 매칭 (남자-여자)
                if (genderA.equals(genderB)) {
                    continue; // 성별이 같으면 스킵
                }

                // Step 4: Lua Script로 atomic match
                // userA와 userB는 null 체크 후 사용
                @SuppressWarnings("null")
                String userAForScript = userA;
                @SuppressWarnings("null")
                String userBForScript = userB;
                Long result = redisService.executeMatchScript(userAForScript, userBForScript);
                if (result != null && result == 1L) {
                    // 매칭 성공
                    log.info("Matched users: {} and {}", userA, userB);
                    matchSuccessCount.increment();
                    matchCount++;

                    // Step 5: Postgres에 매칭 이력 저장
                    saveMatchHistory(userA, userB);
                    break; // 이번 userA에 대한 매칭 완료, 다음 userA로
                }
            }
        }

        // 매칭 실패 (후보는 있지만 조건 불충족)
        if (matchCount == 0) {
            matchFailCount.increment();
        }

        // 타임아웃된 사용자 자동 제거 (매칭 성공/실패와 관계없이 항상 실행)
        // 성별 불균형 등으로 인해 매칭 불가능한 사용자를 정리
        cleanupTimeoutUsers(candidates);
    }

    /**
     * 일정 시간 동안 매칭되지 못한 사용자를 큐에서 자동으로 제거
     * 성별 불균형 등으로 인해 매칭 불가능한 사용자를 정리
     */
    @SuppressWarnings("null") // userId는 candidates 리스트에서 가져온 것이므로 null이 아님
    private void cleanupTimeoutUsers(List<String> candidates) {
        long now = System.currentTimeMillis();

        for (String userId : candidates) {
            // WAITING 상태인 사용자만 확인
            if (!isWaiting(userId)) {
                continue;
            }

            // lastJoinAt 확인
            Long lastJoinAt = redisService.getLastJoinAt(userId);
            if (lastJoinAt == null) {
                continue;
            }

            // 타임아웃된 사용자 (30초 이상 대기)
            long waitingTime = now - lastJoinAt;
            if (waitingTime >= MATCH_TIMEOUT_MS) {
                // 큐에서 제거하고 상태를 IDLE로 변경
                redisService.removeFromQueue(userId);
                redisService.setStatus(userId, Status.IDLE.name());
                log.warn("Auto-removed timeout user from queue: {} (waited {}ms)", userId, waitingTime);
            }
        }
    }

    private boolean isWaiting(String userId) {
        @SuppressWarnings("null") // null 체크 후 사용 (WAITING_STATUS.equals가 null-safe)
        String status = redisService.getStatus(userId);
        return WAITING_STATUS.equals(status);
    }

    @SuppressWarnings("null") // builder().build()는 null을 반환하지 않음
    private void saveMatchHistory(String userA, String userB) {
        try {
            UUID userAUuid = UUID.fromString(userA);
            UUID userBUuid = UUID.fromString(userB);
            // builder().build()는 null을 반환하지 않음
            @SuppressWarnings("null")
            MatchHistory matchHistory = MatchHistory.builder().userAId(userAUuid).userBId(userBUuid).build();
            matchHistoryRepository.save(matchHistory);
            log.debug("Saved match history for users {} and {}", userA, userB);
            // matchedAt은 @CreationTimestamp가 자동으로 설정됨
        } catch (IllegalArgumentException e) {
            // UUID 포맷 오류
            log.error("Invalid UUID format for users {} and {}", userA, userB, e);
        } catch (Exception e) {
            log.error("Failed to save match history for users {} and {}", userA, userB, e);
        }
    }
}
