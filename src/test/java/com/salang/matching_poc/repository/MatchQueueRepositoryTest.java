package com.salang.matching_poc.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.salang.matching_poc.model.entity.MatchQueue;
import com.salang.matching_poc.model.enums.Gender;
import com.salang.matching_poc.model.enums.MatchStatus;
import com.salang.matching_poc.model.enums.Region;
import com.salang.matching_poc.model.enums.Tier;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("MatchQueueRepository 슬라이스 테스트")
@SuppressWarnings("null")
class MatchQueueRepositoryTest {

        @Autowired
        private MatchQueueRepository matchQueueRepository;

        @Test
        @DisplayName("findByStatus: 상태로 조회 성공")
        void findByStatus_Success() {
                UUID userId1 = UUID.randomUUID();
                UUID userId2 = UUID.randomUUID();

                MatchQueue queue1 = MatchQueue.builder()
                                .userId(userId1)
                                .tier(Tier.SPROUT)
                                .region(Region.SEOUL)
                                .birthYear(1995)
                                .gender(Gender.MALE)
                                .hobbyIds(new Integer[] { 1, 2 })
                                .build();

                MatchQueue queue2 = MatchQueue.builder()
                                .userId(userId2)
                                .tier(Tier.SPROUT)
                                .region(Region.BUSAN)
                                .birthYear(1996)
                                .gender(Gender.FEMALE)
                                .hobbyIds(new Integer[] { 3, 4 })
                                .build();
                queue2.setStatus(MatchStatus.MATCHED);

                matchQueueRepository.save(queue1);
                matchQueueRepository.save(queue2);

                List<MatchQueue> waitingQueues = matchQueueRepository.findByStatus(MatchStatus.WAITING);

                assertThat(waitingQueues).hasSize(1);
                assertThat(waitingQueues.get(0).getUserId()).isEqualTo(userId1);
                assertThat(waitingQueues.get(0).getStatus()).isEqualTo(MatchStatus.WAITING);
        }

        @Test
        @DisplayName("findByUserId: 사용자 ID로 조회 성공")
        void findByUserId_Success() {
                UUID userId = UUID.randomUUID();
                MatchQueue queue = MatchQueue.builder()
                                .userId(userId)
                                .tier(Tier.SPROUT)
                                .region(Region.SEOUL)
                                .birthYear(1995)
                                .gender(Gender.MALE)
                                .hobbyIds(new Integer[] { 1, 2 })
                                .build();

                matchQueueRepository.save(queue);

                Optional<MatchQueue> found = matchQueueRepository.findByUserId(userId);

                assertThat(found).isPresent();
                assertThat(found.get().getUserId()).isEqualTo(userId);
        }

        @Test
        @DisplayName("findByUserId: 존재하지 않는 사용자 ID면 Optional.empty()")
        void findByUserId_NotFound_ReturnsEmpty() {
                UUID nonExistentUserId = UUID.randomUUID();

                Optional<MatchQueue> found = matchQueueRepository.findByUserId(nonExistentUserId);

                assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("저장 시 createdAt 자동 설정")
        void save_AutoSetsCreatedAt() {
                UUID userId = UUID.randomUUID();
                MatchQueue queue = MatchQueue.builder()
                                .userId(userId)
                                .tier(Tier.SPROUT)
                                .region(Region.SEOUL)
                                .birthYear(1995)
                                .gender(Gender.MALE)
                                .hobbyIds(new Integer[] { 1, 2 })
                                .build();

                MatchQueue saved = matchQueueRepository.save(queue);

                assertThat(saved.getCreatedAt()).isNotNull();
                assertThat(saved.getCreatedAt()).isBefore(LocalDateTime.now().plusSeconds(1));
        }

        @Test
        @DisplayName("updateStatusIf: 조건에 맞는 레코드 상태 업데이트")
        void updateStatusIf_Success() {
                UUID userId1 = UUID.randomUUID();
                UUID userId2 = UUID.randomUUID();

                MatchQueue queue1 = MatchQueue.builder()
                                .userId(userId1)
                                .tier(Tier.SPROUT)
                                .region(Region.SEOUL)
                                .birthYear(1995)
                                .gender(Gender.MALE)
                                .hobbyIds(new Integer[] { 1 })
                                .build();

                MatchQueue queue2 = MatchQueue.builder()
                                .userId(userId2)
                                .tier(Tier.SPROUT)
                                .region(Region.BUSAN)
                                .birthYear(1996)
                                .gender(Gender.FEMALE)
                                .hobbyIds(new Integer[] { 2 })
                                .build();

                matchQueueRepository.save(queue1);
                matchQueueRepository.save(queue2);

                int updated = matchQueueRepository.updateStatusIf(
                                List.of(userId1, userId2),
                                MatchStatus.WAITING,
                                MatchStatus.MATCHED);

                assertThat(updated).isEqualTo(2);

                Optional<MatchQueue> updatedQueue1 = matchQueueRepository.findByUserId(userId1);
                Optional<MatchQueue> updatedQueue2 = matchQueueRepository.findByUserId(userId2);

                assertThat(updatedQueue1).isPresent();
                assertThat(updatedQueue1.get().getStatus()).isEqualTo(MatchStatus.MATCHED);
                assertThat(updatedQueue2).isPresent();
                assertThat(updatedQueue2.get().getStatus()).isEqualTo(MatchStatus.MATCHED);
        }

        @Test
        @DisplayName("updateStatusIf: 조건에 맞지 않으면 업데이트되지 않음")
        void updateStatusIf_NoMatch_ReturnsZero() {
                UUID userId = UUID.randomUUID();

                MatchQueue queue = MatchQueue.builder()
                                .userId(userId)
                                .tier(Tier.SPROUT)
                                .region(Region.SEOUL)
                                .birthYear(1995)
                                .gender(Gender.MALE)
                                .hobbyIds(new Integer[] { 1 })
                                .build();
                queue.setStatus(MatchStatus.MATCHED);

                matchQueueRepository.save(queue);

                int updated = matchQueueRepository.updateStatusIf(
                                List.of(userId),
                                MatchStatus.WAITING,
                                MatchStatus.MATCHED);

                assertThat(updated).isEqualTo(0);
        }
}
