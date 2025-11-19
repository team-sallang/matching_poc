package com.salang.matching_poc.service;

import com.salang.matching_poc.exception.AlreadyInQueueException;
import com.salang.matching_poc.exception.CannotLeaveMatchedException;
import com.salang.matching_poc.exception.TooManyRequestsException;
import com.salang.matching_poc.model.dto.*;
import com.salang.matching_poc.model.enums.Status;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchingService {

    private static final long DEBOUNCE_INTERVAL_MS = 1000L; // 1초
    private static final String IDLE_STATUS = Status.IDLE.name();
    private static final String WAITING_STATUS = Status.WAITING.name();
    private static final String MATCHED_STATUS = Status.MATCHED.name();

    private final RedisService redisService;

    @Transactional
    public JoinQueueResponse joinQueue(JoinQueueRequest request) {
        String userId = request.getUserId().toString();
        String gender = request.getGender();

        // 상태 확인
        String currentStatus = redisService.getStatus(userId);
        if (currentStatus != null && (WAITING_STATUS.equals(currentStatus) || MATCHED_STATUS.equals(currentStatus))) {
            throw new AlreadyInQueueException();
        }

        // Debounce 체크 (1초 미만 재요청 차단)
        Long lastJoinAt = redisService.getLastJoinAt(userId);
        long now = System.currentTimeMillis();
        if (lastJoinAt != null && (now - lastJoinAt) < DEBOUNCE_INTERVAL_MS) {
            throw new TooManyRequestsException();
        }

        // 상태 등록
        redisService.setStatus(userId, WAITING_STATUS);
        redisService.setLastJoinAt(userId, now);
        redisService.setGender(userId, gender);

        // 대기열 등록 (ZADD matching:queue score=now value=userId)
        redisService.addToQueue(userId, now);

        log.debug("User {} joined queue with gender {}", userId, gender);
        return JoinQueueResponse.builder()
                .status(WAITING_STATUS)
                .build();
    }

    @Transactional
    public LeaveQueueResponse leaveQueue(LeaveQueueRequest request) {
        String userId = request.getUserId().toString();

        // MATCHED 상태인 경우 Leave 불가
        String currentStatus = redisService.getStatus(userId);
        if (MATCHED_STATUS.equals(currentStatus)) {
            throw new CannotLeaveMatchedException();
        }

        // 큐에서 제거
        redisService.removeFromQueue(userId);
        // 상태를 IDLE로 변경
        redisService.setStatus(userId, IDLE_STATUS);
        // matchedWith 삭제 (있을 경우)
        redisService.deleteMatchedWith(userId);

        log.debug("User {} left queue", userId);
        return LeaveQueueResponse.builder()
                .status(IDLE_STATUS)
                .build();
    }

    public StatusResponse getStatus(UUID userId) {
        String userIdStr = userId.toString();
        String status = redisService.getStatus(userIdStr);

        // 상태가 없으면 IDLE로 간주
        if (status == null) {
            status = IDLE_STATUS;
        }

        StatusResponse.StatusResponseBuilder builder = StatusResponse.builder()
                .status(status);

        // MATCHED 상태일 때만 matchedWith 포함
        if (MATCHED_STATUS.equals(status)) {
            String matchedWith = redisService.getMatchedWith(userIdStr);
            if (matchedWith != null) {
                builder.matchedWith(UUID.fromString(matchedWith));
            }
        }

        return builder.build();
    }

    @Transactional
    public AckResponse acknowledgeMatch(AckRequest request) {
        String userId = request.getUserId().toString();

        // 상태를 IDLE로 변경
        redisService.setStatus(userId, IDLE_STATUS);
        // matchedWith 삭제
        redisService.deleteMatchedWith(userId);

        log.debug("User {} acknowledged match", userId);
        return AckResponse.builder()
                .status(IDLE_STATUS)
                .build();
    }
}
