package com.salang.matching_poc.service;

import com.salang.matching_poc.worker.MatchingWorker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Tag("integration")
@SuppressWarnings("null") // 테스트에서 userId 변수들은 null이 아님을 보장
@ComponentScan(basePackages = "com.salang.matching_poc", excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = MatchingWorker.class))
class RedisServiceTest {

    @TestConfiguration
    static class TestConfig {
        // MatchingWorker는 @ComponentScan에서 제외되어 여기서 등록되지 않음
    }

    @Autowired
    private RedisService redisService;

    private String userId1;
    private String userId2;

    @BeforeEach
    void setUp() {
        userId1 = UUID.randomUUID().toString();
        userId2 = UUID.randomUUID().toString();

        // 큐 전체 초기화 (테스트 격리 보장)
        redisService.clearQueue();

        // Redis 초기화
        cleanupUser(userId1);
        cleanupUser(userId2);
    }

    private void cleanupUser(String userId) {
        redisService.setStatus(userId, "IDLE");
        redisService.removeFromQueue(userId);
        redisService.deleteMatchedWith(userId);
    }

    @Test
    @DisplayName("상태를 설정하고 조회할 수 있다")
    void setAndGetStatus() {
        // when
        redisService.setStatus(userId1, "WAITING");

        // then
        assertThat(redisService.getStatus(userId1)).isEqualTo("WAITING");
    }

    @Test
    @DisplayName("lastJoinAt을 설정하고 조회할 수 있다")
    void setAndGetLastJoinAt() {
        // given
        long now = System.currentTimeMillis();

        // when
        redisService.setLastJoinAt(userId1, now);

        // then
        assertThat(redisService.getLastJoinAt(userId1)).isEqualTo(now);
    }

    @Test
    @DisplayName("matchedWith를 설정하고 조회할 수 있다")
    void setAndGetMatchedWith() {
        // when
        redisService.setMatchedWith(userId1, userId2);

        // then
        assertThat(redisService.getMatchedWith(userId1)).isEqualTo(userId2);
    }

    @Test
    @DisplayName("gender를 설정하고 조회할 수 있다")
    void setAndGetGender() {
        // when
        redisService.setGender(userId1, "male");

        // then
        assertThat(redisService.getGender(userId1)).isEqualTo("male");
    }

    @Test
    @DisplayName("큐에 추가하고 조회할 수 있다")
    void addToQueueAndGetTopCandidates() {
        // given
        long now = System.currentTimeMillis();

        // when
        redisService.addToQueue(userId1, now);
        redisService.addToQueue(userId2, now + 1000);

        // then
        List<String> candidates = redisService.getTopCandidates(10);
        assertThat(candidates).contains(userId1, userId2);
        assertThat(candidates.get(0)).isEqualTo(userId1); // 먼저 추가된 것이 앞에
    }

    @Test
    @DisplayName("큐에서 제거할 수 있다")
    void removeFromQueue() {
        // given
        long now = System.currentTimeMillis();
        redisService.addToQueue(userId1, now);
        redisService.addToQueue(userId2, now + 1000);

        // when
        redisService.removeFromQueue(userId1);

        // then
        List<String> candidates = redisService.getTopCandidates(10);
        assertThat(candidates).doesNotContain(userId1);
        assertThat(candidates).contains(userId2);
    }

    @Test
    @DisplayName("Lua Script로 매칭을 실행할 수 있다")
    void executeMatchScript() {
        // given
        redisService.setStatus(userId1, "WAITING");
        redisService.setStatus(userId2, "WAITING");
        redisService.addToQueue(userId1, System.currentTimeMillis());
        redisService.addToQueue(userId2, System.currentTimeMillis() + 1000);

        // when
        Long result = redisService.executeMatchScript(userId1, userId2);

        // then
        assertThat(result).isEqualTo(1L);
        assertThat(redisService.getStatus(userId1)).isEqualTo("MATCHED");
        assertThat(redisService.getStatus(userId2)).isEqualTo("MATCHED");
        assertThat(redisService.getMatchedWith(userId1)).isEqualTo(userId2);
        assertThat(redisService.getMatchedWith(userId2)).isEqualTo(userId1);
        assertThat(redisService.getQueueLength()).isLessThan(2);
    }

    @Test
    @DisplayName("WAITING 상태가 아닌 경우 매칭 실패")
    void executeMatchScript_failWhenNotWaiting() {
        // given
        redisService.setStatus(userId1, "IDLE");
        redisService.setStatus(userId2, "WAITING");

        // when
        Long result = redisService.executeMatchScript(userId1, userId2);

        // then
        assertThat(result).isEqualTo(0L);
        assertThat(redisService.getStatus(userId1)).isEqualTo("IDLE");
        assertThat(redisService.getStatus(userId2)).isEqualTo("WAITING");
    }
}
