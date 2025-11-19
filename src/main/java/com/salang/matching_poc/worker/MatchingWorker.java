package com.salang.matching_poc.worker;

import com.salang.matching_poc.model.entity.MatchHistory;
import com.salang.matching_poc.model.enums.Status;
import com.salang.matching_poc.repository.MatchHistoryRepository;
import com.salang.matching_poc.service.RedisService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class MatchingWorker {

    private static final long TICK_INTERVAL_MS = 50L; // 50ms
    private static final int TOP_CANDIDATES_COUNT = 50;
    private static final String WAITING_STATUS = Status.WAITING.name();

    private final RedisService redisService;
    private final MatchHistoryRepository matchHistoryRepository;

    private Thread workerThread;
    private volatile boolean running = false;

    @PostConstruct
    public void start() {
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
            long startTime = System.currentTimeMillis();
            try {
                processMatching();
            } catch (Exception e) {
                log.error("Error in matching worker tick", e);
            }

            // 50ms 간격 유지
            long elapsed = System.currentTimeMillis() - startTime;
            long sleepTime = Math.max(0, TICK_INTERVAL_MS - elapsed);
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

    private void processMatching() {
        // Step 1: Top-50 후보 조회
        List<String> candidates = redisService.getTopCandidates(TOP_CANDIDATES_COUNT);
        if (candidates.isEmpty()) {
            return;
        }

        // Step 2: 후보 필터링 및 매칭 시도
        for (int i = 0; i < candidates.size(); i++) {
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

            // Step 3: 매칭 조건 검사 (성별 동일)
            for (int j = i + 1; j < candidates.size(); j++) {
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

                // 성별 동일 조건 확인
                if (!genderA.equals(genderB)) {
                    continue;
                }

                // Step 4: Lua Script로 atomic match
                Long result = redisService.executeMatchScript(userA, userB);
                if (result != null && result == 1L) {
                    // 매칭 성공
                    log.info("Matched users: {} and {}", userA, userB);

                    // Step 5: Postgres에 매칭 이력 저장
                    saveMatchHistory(userA, userB);
                    return; // 이번 tick에서 한 쌍만 매칭
                }
            }
        }
    }

    private boolean isWaiting(String userId) {
        String status = redisService.getStatus(userId);
        return WAITING_STATUS.equals(status);
    }

    private void saveMatchHistory(String userA, String userB) {
        try {
            MatchHistory matchHistory = MatchHistory.builder().userAId(UUID.fromString(userA))
                    .userBId(UUID.fromString(userB)).build();
            matchHistoryRepository.save(matchHistory);
            // matchedAt은 @CreationTimestamp가 자동으로 설정됨
        } catch (Exception e) {
            log.error("Failed to save match history for users {} and {}", userA, userB, e);
        }
    }
}
