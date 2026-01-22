package com.salang.matching_poc.worker;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.salang.matching_poc.model.entity.MatchQueue;
import com.salang.matching_poc.model.enums.Gender;
import com.salang.matching_poc.model.enums.MatchStatus;
import com.salang.matching_poc.model.enums.Region;
import com.salang.matching_poc.model.enums.Tier;
import com.salang.matching_poc.repository.MatchQueueRepository;
import com.salang.matching_poc.service.MatchQueueMatchFinder;
import com.salang.matching_poc.service.MatchService;

@ExtendWith(MockitoExtension.class)
@DisplayName("MatchScheduler 단위 테스트")
@SuppressWarnings({ "null", "unused" })
class MatchSchedulerTest {

        @Mock
        private MatchQueueRepository matchQueueRepository;

        @Mock
        private MatchService matchService;

        @Mock
        private MatchQueueMatchFinder matchQueueMatchFinder;

        @InjectMocks
        private MatchScheduler matchScheduler;

        private MatchQueue requester;

        @BeforeEach
        void setUp() {
                requester = MatchQueue.builder()
                                .userId(UUID.randomUUID())
                                .hobbyIds(new Integer[] { 1, 2, 3 })
                                .tier(Tier.SPROUT)
                                .region(Region.SEOUL)
                                .birthYear(1995)
                                .gender(Gender.MALE)
                                .build();
                requester.setStatus(MatchStatus.WAITING);
        }

        private void setCreatedAt(MatchQueue queue, LocalDateTime createdAt) throws Exception {
                java.lang.reflect.Field createdAtField = MatchQueue.class.getDeclaredField("createdAt");
                createdAtField.setAccessible(true);
                createdAtField.set(queue, createdAt);
        }

        @Test
        @DisplayName("대기열이 비어있으면 매칭 처리하지 않음")
        void runMatchingLoop_EmptyQueue_NoProcessing() {
                when(matchQueueRepository.findByStatus(MatchStatus.WAITING))
                                .thenReturn(Collections.emptyList());

                matchScheduler.runMatchingLoop();

                verify(matchService, never()).confirmMatch(any(), any());
        }

        @Test
        @DisplayName("대기 시간 0~4초: Phase1 매칭 시도")
        void runMatchingLoop_Phase1_AttemptsPhase1Match() throws Exception {
                requester = MatchQueue.builder()
                                .userId(UUID.randomUUID())
                                .hobbyIds(new Integer[] { 1, 2 })
                                .tier(Tier.SPROUT)
                                .region(Region.SEOUL)
                                .birthYear(1995)
                                .gender(Gender.MALE)
                                .build();
                requester.setStatus(MatchStatus.WAITING);
                setCreatedAt(requester, LocalDateTime.now().minusSeconds(2));

                UUID partnerUserId = UUID.randomUUID();
                MatchQueue partner = MatchQueue.builder()
                                .userId(partnerUserId)
                                .build();

                when(matchQueueRepository.findByStatus(MatchStatus.WAITING))
                                .thenReturn(List.of(requester));
                when(matchQueueMatchFinder.findPhase1Match(
                                eq(requester.getUserId()), eq("MALE"), eq("SEOUL"), any(), any(), any(), any(), any()))
                                .thenReturn(Optional.of(partner));

                matchScheduler.runMatchingLoop();

                verify(matchService).confirmMatch(requester.getUserId(), partnerUserId);
        }

        @Test
        @DisplayName("대기 시간 5~9초: Phase2 매칭 시도")
        void runMatchingLoop_Phase2_AttemptsPhase2Match() throws Exception {
                requester = MatchQueue.builder()
                                .userId(UUID.randomUUID())
                                .tier(Tier.SPROUT)
                                .region(Region.SEOUL)
                                .birthYear(1995)
                                .gender(Gender.MALE)
                                .build();
                requester.setStatus(MatchStatus.WAITING);
                setCreatedAt(requester, LocalDateTime.now().minusSeconds(7));

                UUID partnerUserId = UUID.randomUUID();
                MatchQueue partner = MatchQueue.builder()
                                .userId(partnerUserId)
                                .build();

                when(matchQueueRepository.findByStatus(MatchStatus.WAITING))
                                .thenReturn(List.of(requester));
                when(matchQueueMatchFinder.findPhase2Match(
                                eq(requester.getUserId()), eq("MALE"), eq("SEOUL"), any(), any(), any(), any()))
                                .thenReturn(Optional.of(partner));

                matchScheduler.runMatchingLoop();

                verify(matchService).confirmMatch(requester.getUserId(), partnerUserId);
        }

        @Test
        @DisplayName("대기 시간 10~19초: Phase3 매칭 시도")
        void runMatchingLoop_Phase3_AttemptsPhase3Match() throws Exception {
                requester = MatchQueue.builder()
                                .userId(UUID.randomUUID())
                                .tier(Tier.SPROUT)
                                .birthYear(1995)
                                .gender(Gender.MALE)
                                .build();
                requester.setStatus(MatchStatus.WAITING);
                setCreatedAt(requester, LocalDateTime.now().minusSeconds(15));

                UUID partnerUserId = UUID.randomUUID();
                MatchQueue partner = MatchQueue.builder()
                                .userId(partnerUserId)
                                .build();

                when(matchQueueRepository.findByStatus(MatchStatus.WAITING))
                                .thenReturn(List.of(requester));
                when(matchQueueMatchFinder.findPhase3Match(
                                eq(requester.getUserId()), eq("MALE"), any(), any(), any(), any()))
                                .thenReturn(Optional.of(partner));

                matchScheduler.runMatchingLoop();

                verify(matchService).confirmMatch(requester.getUserId(), partnerUserId);
        }

        @Test
        @DisplayName("대기 시간 20~29초: Phase4 매칭 시도")
        void runMatchingLoop_Phase4_AttemptsPhase4Match() throws Exception {
                requester = MatchQueue.builder()
                                .userId(UUID.randomUUID())
                                .tier(Tier.SPROUT)
                                .gender(Gender.MALE)
                                .build();
                requester.setStatus(MatchStatus.WAITING);
                setCreatedAt(requester, LocalDateTime.now().minusSeconds(25));

                UUID partnerUserId = UUID.randomUUID();
                MatchQueue partner = MatchQueue.builder()
                                .userId(partnerUserId)
                                .build();

                when(matchQueueRepository.findByStatus(MatchStatus.WAITING))
                                .thenReturn(List.of(requester));
                when(matchQueueMatchFinder.findPhase4Match(
                                eq(requester.getUserId()), eq("MALE"), any(), any()))
                                .thenReturn(Optional.of(partner));

                matchScheduler.runMatchingLoop();

                verify(matchService).confirmMatch(requester.getUserId(), partnerUserId);
        }

        @Test
        @DisplayName("대기 시간 30초 이상: Phase5 매칭 시도")
        void runMatchingLoop_Phase5_AttemptsPhase5Match() throws Exception {
                requester = MatchQueue.builder()
                                .userId(UUID.randomUUID())
                                .build();
                requester.setStatus(MatchStatus.WAITING);
                setCreatedAt(requester, LocalDateTime.now().minusSeconds(35));

                UUID partnerUserId = UUID.randomUUID();
                MatchQueue partner = MatchQueue.builder()
                                .userId(partnerUserId)
                                .build();

                when(matchQueueRepository.findByStatus(MatchStatus.WAITING))
                                .thenReturn(List.of(requester));
                when(matchQueueMatchFinder.findPhase5Match(
                                eq(requester.getUserId()), any()))
                                .thenReturn(Optional.of(partner));

                matchScheduler.runMatchingLoop();

                verify(matchService).confirmMatch(requester.getUserId(), partnerUserId);
        }

        @Test
        @DisplayName("파트너를 찾지 못하면 매칭 처리하지 않음")
        void runMatchingLoop_NoPartner_NoMatch() throws Exception {
                setCreatedAt(requester, LocalDateTime.now().minusSeconds(2));
                when(matchQueueRepository.findByStatus(MatchStatus.WAITING))
                                .thenReturn(List.of(requester));
                when(matchQueueMatchFinder.findPhase1Match(any(), any(), any(), any(), any(), any(), any(), any()))
                                .thenReturn(Optional.empty());

                matchScheduler.runMatchingLoop();

                verify(matchService, never()).confirmMatch(any(), any());
        }

        @Test
        @DisplayName("이미 MATCHED 상태인 사용자는 스킵")
        void runMatchingLoop_AlreadyMatched_Skips() {
                requester.setStatus(MatchStatus.MATCHED);

                when(matchQueueRepository.findByStatus(MatchStatus.WAITING))
                                .thenReturn(List.of(requester));

                matchScheduler.runMatchingLoop();

                verify(matchService, never()).confirmMatch(any(), any());
        }

        @Test
        @DisplayName("매칭 처리 중 예외 발생 시 로그만 남기고 계속 진행")
        void runMatchingLoop_ExceptionDuringProcessing_Continues() throws Exception {
                UUID partnerUserId = UUID.randomUUID();
                MatchQueue partner = MatchQueue.builder()
                                .userId(partnerUserId)
                                .build();

                setCreatedAt(requester, LocalDateTime.now().minusSeconds(2));
                when(matchQueueRepository.findByStatus(MatchStatus.WAITING))
                                .thenReturn(List.of(requester));
                when(matchQueueMatchFinder.findPhase1Match(any(), any(), any(), any(), any(), any(), any(), any()))
                                .thenReturn(Optional.of(partner));
                doThrow(new RuntimeException("Test exception"))
                                .when(matchService).confirmMatch(requester.getUserId(), partnerUserId);

                matchScheduler.runMatchingLoop();

                verify(matchService).confirmMatch(requester.getUserId(), partnerUserId);
        }
}
