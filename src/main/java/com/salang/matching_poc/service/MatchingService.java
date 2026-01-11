package com.salang.matching_poc.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.salang.matching_poc.exception.AlreadyInQueueException;
import com.salang.matching_poc.exception.CannotLeaveMatchedException;
import com.salang.matching_poc.exception.TooManyRequestsException;
import com.salang.matching_poc.model.dto.AckRequest;
import com.salang.matching_poc.model.dto.AckResponse;
import com.salang.matching_poc.model.dto.JoinQueueRequest;
import com.salang.matching_poc.model.dto.JoinQueueResponse;
import com.salang.matching_poc.model.dto.LeaveQueueRequest;
import com.salang.matching_poc.model.dto.LeaveQueueResponse;
import com.salang.matching_poc.model.dto.StatusResponse;
import com.salang.matching_poc.model.enums.Status;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchingService {

    private static final long DEBOUNCE_INTERVAL_MS = 1000L; // 1초
    private static final String IDLE_STATUS = Status.IDLE.name();
    private static final String WAITING_STATUS = Status.WAITING.name();
    private static final String MATCHED_STATUS = Status.MATCHED.name();

    private final RedisService redisService;
    private final MeterRegistry meterRegistry;

    private Timer joinQueueLatencyTimer;
    private Timer statusLatencyTimer;

    @PostConstruct
    public void init() {
        joinQueueLatencyTimer = Timer.builder("matching_join_queue_latency")
                .description("JoinQueue API 응답 시간")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);

        statusLatencyTimer = Timer.builder("matching_status_latency")
                .description("Status API 응답 시간")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);
    }

    @Transactional
    @SuppressWarnings("null") // userId와 gender는 null이 아니며, toString()과 request에서 검증된 값
    public JoinQueueResponse joinQueue(JoinQueueRequest request) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            String userId = request.getUserId().toString();
            String gender = request.getGender();

            // 상태 확인
            @SuppressWarnings("null") // null 체크 후 사용
            String currentStatus = redisService.getStatus(userId);
            if (currentStatus != null
                    && (WAITING_STATUS.equals(currentStatus) || MATCHED_STATUS.equals(currentStatus))) {
                throw new AlreadyInQueueException();
            }

            // Debounce 체크 (1초 미만 재요청 차단)
            Long lastJoinAt = redisService.getLastJoinAt(userId);
            long now = System.currentTimeMillis();
            if (lastJoinAt != null && (now - lastJoinAt) < DEBOUNCE_INTERVAL_MS) {
                throw new TooManyRequestsException();
            }

            // 상태 등록
            // userId와 gender는 null이 아니며, toString()과 request에서 검증된 값
            @SuppressWarnings("null")
            String userIdForRedis = userId;
            @SuppressWarnings("null")
            String genderForRedis = gender;
            redisService.setStatus(userIdForRedis, WAITING_STATUS);
            redisService.setLastJoinAt(userIdForRedis, now);
            redisService.setGender(userIdForRedis, genderForRedis);

            // 대기열 등록 (ZADD matching:queue score=now value=userId)
            @SuppressWarnings("null")
            String userIdForQueue = userId;
            redisService.addToQueue(userIdForQueue, now);

            log.debug("User {} joined queue with gender {}", userId, gender);
            return JoinQueueResponse.builder()
                    .status(WAITING_STATUS)
                    .build();
        } finally {
            sample.stop(joinQueueLatencyTimer);
        }
    }

    @Transactional
    @SuppressWarnings("null") // userId는 toString() 결과이므로 null이 아님
    public LeaveQueueResponse leaveQueue(LeaveQueueRequest request) {
        String userId = request.getUserId().toString();

        // MATCHED 상태인 경우 Leave 불가
        @SuppressWarnings("null") // null 체크 후 사용
        String currentStatus = redisService.getStatus(userId);
        if (MATCHED_STATUS.equals(currentStatus)) {
            throw new CannotLeaveMatchedException();
        }

        // 큐에서 제거
        // userId는 toString() 결과이므로 null이 아님
        @SuppressWarnings("null")
        String userIdForRedis = userId;
        redisService.removeFromQueue(userIdForRedis);
        // 상태를 IDLE로 변경
        redisService.setStatus(userIdForRedis, IDLE_STATUS);
        // matchedWith 삭제 (있을 경우)
        redisService.deleteMatchedWith(userIdForRedis);

        log.debug("User {} left queue", userId);
        return LeaveQueueResponse.builder()
                .status(IDLE_STATUS)
                .build();
    }

    public StatusResponse getStatus(UUID userId) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            String userIdStr = userId.toString();
            @SuppressWarnings("null") // null 체크 후 사용
            String status = redisService.getStatus(userIdStr);

            // 상태가 없으면 IDLE로 간주
            if (status == null) {
                status = IDLE_STATUS;
            }

            StatusResponse.StatusResponseBuilder builder = StatusResponse.builder()
                    .status(status);

            // MATCHED 상태일 때만 matchedWith 포함
            if (MATCHED_STATUS.equals(status)) {
                @SuppressWarnings("null") // null 체크 후 사용
                String matchedWith = redisService.getMatchedWith(userIdStr);
                if (matchedWith != null) {
                    builder.matchedWith(UUID.fromString(matchedWith));
                }
                // lastJoinAt 추가
                Long lastJoinAt = redisService.getLastJoinAt(userIdStr);
                if (lastJoinAt != null) {
                    builder.lastJoinAt(lastJoinAt);
                }
            }

            return builder.build();
        } finally {
            sample.stop(statusLatencyTimer);
        }
    }

    @Transactional
    @SuppressWarnings("null") // userId는 toString() 결과이므로 null이 아님
    public AckResponse acknowledgeMatch(AckRequest request) {
        String userId = request.getUserId().toString();

        // 상태를 IDLE로 변경
        // userId는 toString() 결과이므로 null이 아님
        @SuppressWarnings("null")
        String userIdForRedis = userId;
        redisService.setStatus(userIdForRedis, IDLE_STATUS);
        // matchedWith 삭제
        redisService.deleteMatchedWith(userIdForRedis);

        log.debug("User {} acknowledged match", userId);
        return AckResponse.builder()
                .status(IDLE_STATUS)
                .build();
    }
}
