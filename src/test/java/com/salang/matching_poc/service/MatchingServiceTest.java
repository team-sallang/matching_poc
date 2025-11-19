package com.salang.matching_poc.service;

import com.salang.matching_poc.exception.AlreadyInQueueException;
import com.salang.matching_poc.exception.CannotLeaveMatchedException;
import com.salang.matching_poc.exception.TooManyRequestsException;
import com.salang.matching_poc.model.dto.*;
import com.salang.matching_poc.model.enums.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchingServiceTest {

    @Mock
    private RedisService redisService;

    @InjectMocks
    private MatchingService matchingService;

    private UUID userId1;
    private UUID userId2;

    @BeforeEach
    void setUp() {
        userId1 = UUID.randomUUID();
        userId2 = UUID.randomUUID();
    }

    @Test
    @DisplayName("큐에 정상적으로 참여할 수 있다")
    void joinQueue_success() {
        // given
        JoinQueueRequest request = JoinQueueRequest.builder()
                .userId(userId1)
                .gender("male")
                .build();

        when(redisService.getStatus(userId1.toString())).thenReturn(null);
        when(redisService.getLastJoinAt(userId1.toString())).thenReturn(null);

        // when
        JoinQueueResponse response = matchingService.joinQueue(request);

        // then
        assertThat(response.getStatus()).isEqualTo(Status.WAITING.name());
        verify(redisService).setStatus(userId1.toString(), Status.WAITING.name());
        verify(redisService).setGender(userId1.toString(), "male");
        verify(redisService).setLastJoinAt(eq(userId1.toString()), anyLong());
        verify(redisService).addToQueue(eq(userId1.toString()), anyLong());
    }

    @Test
    @DisplayName("이미 WAITING 상태인 경우 큐 참여 불가")
    void joinQueue_alreadyWaiting() {
        // given
        JoinQueueRequest request = JoinQueueRequest.builder()
                .userId(userId1)
                .gender("male")
                .build();

        when(redisService.getStatus(userId1.toString()))
                .thenReturn(Status.WAITING.name());

        // when & then
        assertThatThrownBy(() -> matchingService.joinQueue(request))
                .isInstanceOf(AlreadyInQueueException.class);

        verify(redisService, never()).addToQueue(anyString(), anyLong());
        verify(redisService, never()).setStatus(anyString(), anyString());
    }

    @Test
    @DisplayName("1초 미만 재요청 시 TOO_MANY_REQUESTS 예외 발생")
    void joinQueue_tooManyRequests() {
        // given
        JoinQueueRequest request = JoinQueueRequest.builder()
                .userId(userId1)
                .gender("male")
                .build();

        long now = System.currentTimeMillis();
        when(redisService.getStatus(userId1.toString())).thenReturn(Status.IDLE.name());
        when(redisService.getLastJoinAt(userId1.toString()))
                .thenReturn(now - 500); // 500ms 전에 참여했음 (1초 미만)

        // when & then - 즉시 재요청
        assertThatThrownBy(() -> matchingService.joinQueue(request))
                .isInstanceOf(TooManyRequestsException.class);

        verify(redisService, never()).addToQueue(anyString(), anyLong());
        verify(redisService, never()).setStatus(anyString(), eq(Status.WAITING.name()));
    }

    @Test
    @DisplayName("큐에서 정상적으로 나갈 수 있다")
    void leaveQueue_success() {
        // given
        LeaveQueueRequest request = LeaveQueueRequest.builder()
                .userId(userId1)
                .build();

        when(redisService.getStatus(userId1.toString())).thenReturn(Status.WAITING.name());

        // when
        LeaveQueueResponse response = matchingService.leaveQueue(request);

        // then
        assertThat(response.getStatus()).isEqualTo(Status.IDLE.name());
        verify(redisService).removeFromQueue(userId1.toString());
        verify(redisService).setStatus(userId1.toString(), Status.IDLE.name());
        verify(redisService).deleteMatchedWith(userId1.toString());
    }

    @Test
    @DisplayName("MATCHED 상태인 경우 큐에서 나갈 수 없다")
    void leaveQueue_cannotLeaveMatched() {
        // given
        LeaveQueueRequest request = LeaveQueueRequest.builder()
                .userId(userId1)
                .build();

        when(redisService.getStatus(userId1.toString()))
                .thenReturn(Status.MATCHED.name());

        // when & then
        assertThatThrownBy(() -> matchingService.leaveQueue(request))
                .isInstanceOf(CannotLeaveMatchedException.class);

        verify(redisService, never()).removeFromQueue(anyString());
        verify(redisService, never()).setStatus(anyString(), eq(Status.IDLE.name()));
    }

    @Test
    @DisplayName("상태를 정상적으로 조회할 수 있다")
    void getStatus_success() {
        // given
        when(redisService.getStatus(userId1.toString()))
                .thenReturn(Status.WAITING.name());

        // when
        StatusResponse response = matchingService.getStatus(userId1);

        // then
        assertThat(response.getStatus()).isEqualTo(Status.WAITING.name());
        assertThat(response.getMatchedWith()).isNull();
        verify(redisService).getStatus(userId1.toString());
        verify(redisService, never()).getMatchedWith(anyString());
    }

    @Test
    @DisplayName("MATCHED 상태일 때 matchedWith를 조회할 수 있다")
    void getStatus_matched() {
        // given
        when(redisService.getStatus(userId1.toString()))
                .thenReturn(Status.MATCHED.name());
        when(redisService.getMatchedWith(userId1.toString()))
                .thenReturn(userId2.toString());

        // when
        StatusResponse response = matchingService.getStatus(userId1);

        // then
        assertThat(response.getStatus()).isEqualTo(Status.MATCHED.name());
        assertThat(response.getMatchedWith()).isEqualTo(userId2);
        verify(redisService).getStatus(userId1.toString());
        verify(redisService).getMatchedWith(userId1.toString());
    }

    @Test
    @DisplayName("매칭 완료를 정상적으로 확인할 수 있다")
    void acknowledgeMatch_success() {
        // given
        AckRequest request = AckRequest.builder()
                .userId(userId1)
                .build();

        // when
        AckResponse response = matchingService.acknowledgeMatch(request);

        // then
        assertThat(response.getStatus()).isEqualTo(Status.IDLE.name());
        verify(redisService).setStatus(userId1.toString(), Status.IDLE.name());
        verify(redisService).deleteMatchedWith(userId1.toString());
    }
}
