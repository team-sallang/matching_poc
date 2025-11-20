package com.salang.matching_poc.service;

import com.salang.matching_poc.model.RedisKeys;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisService {

    private final StringRedisTemplate redisTemplate;
    private final MeterRegistry meterRegistry;
    private DefaultRedisScript<Long> matchScript;

    private Timer redisZrangeLatencyTimer;
    private Timer redisLuaLatencyTimer;

    @PostConstruct
    public void init() {
        try {
            ClassPathResource resource = new ClassPathResource("match.lua");
            java.nio.charset.Charset charset = Objects.requireNonNull(StandardCharsets.UTF_8);
            String script = StreamUtils.copyToString(resource.getInputStream(), charset);
            matchScript = new DefaultRedisScript<>(script, Long.class);
            log.info("Lua script loaded successfully");
        } catch (Exception e) {
            log.error("Failed to load Lua script", e);
            throw new RuntimeException("Failed to load Lua script", e);
        }

        // 메트릭 초기화
        redisZrangeLatencyTimer = Timer.builder("matching_redis_zrange_latency")
                .description("Top-50 조회(ZRANGE) 수행 시간")
                .register(meterRegistry);

        redisLuaLatencyTimer = Timer.builder("matching_redis_lua_latency")
                .description("atomic match Lua Script 실행 시간")
                .register(meterRegistry);
    }

    // String operations - Status
    public void setStatus(@NonNull String userId, @NonNull String status) {
        redisTemplate.opsForValue().set(RedisKeys.getUserStatusKey(userId), status);
    }

    @Nullable
    public String getStatus(@NonNull String userId) {
        return redisTemplate.opsForValue().get(RedisKeys.getUserStatusKey(userId));
    }

    // String operations - LastJoinAt
    public void setLastJoinAt(@NonNull String userId, long epochMillis) {
        String value = Objects.requireNonNull(String.valueOf(epochMillis));
        redisTemplate.opsForValue().set(RedisKeys.getUserLastJoinAtKey(userId), value);
    }

    public Long getLastJoinAt(String userId) {
        String value = redisTemplate.opsForValue().get(RedisKeys.getUserLastJoinAtKey(userId));
        return value != null ? Long.parseLong(value) : null;
    }

    // String operations - MatchedWith
    public void setMatchedWith(@NonNull String userId, @NonNull String matchedWithUserId) {
        redisTemplate.opsForValue().set(RedisKeys.getUserMatchedWithKey(userId), matchedWithUserId);
    }

    @Nullable
    public String getMatchedWith(@NonNull String userId) {
        return redisTemplate.opsForValue().get(RedisKeys.getUserMatchedWithKey(userId));
    }

    public void deleteMatchedWith(@NonNull String userId) {
        redisTemplate.delete(RedisKeys.getUserMatchedWithKey(userId));
    }

    // String operations - Gender
    public void setGender(@NonNull String userId, @NonNull String gender) {
        redisTemplate.opsForValue().set(RedisKeys.getUserGenderKey(userId), gender);
    }

    @Nullable
    public String getGender(@NonNull String userId) {
        return redisTemplate.opsForValue().get(RedisKeys.getUserGenderKey(userId));
    }

    // ZSET operations - Queue
    public void addToQueue(@NonNull String userId, long score) {
        redisTemplate.opsForZSet().add(RedisKeys.MATCHING_QUEUE_KEY, userId, score);
    }

    public List<String> getTopCandidates(int count) {
        try {
            return redisZrangeLatencyTimer.recordCallable(() -> {
                Set<String> candidates = redisTemplate.opsForZSet().range(RedisKeys.MATCHING_QUEUE_KEY, 0, count - 1);
                return candidates != null ? List.copyOf(candidates) : Collections.emptyList();
            });
        } catch (Exception e) {
            log.error("Error in getTopCandidates", e);
            return Collections.emptyList();
        }
    }

    public void removeFromQueue(@NonNull String userId) {
        redisTemplate.opsForZSet().remove(RedisKeys.MATCHING_QUEUE_KEY, userId);
    }

    public void removeFromQueue(@NonNull String... userIds) {
        redisTemplate.opsForZSet().remove(RedisKeys.MATCHING_QUEUE_KEY, (Object[]) userIds);
    }

    public Long getQueueLength() {
        return redisTemplate.opsForZSet().zCard(RedisKeys.MATCHING_QUEUE_KEY);
    }

    public void clearQueue() {
        redisTemplate.delete(RedisKeys.MATCHING_QUEUE_KEY);
    }

    // Lua Script execution
    public Long executeMatchScript(@NonNull String userA, @NonNull String userB) {
        try {
            return redisLuaLatencyTimer.recordCallable(() -> {
                List<String> keys = Objects.requireNonNull(List.of());
                return redisTemplate.execute(matchScript, keys, userA, userB);
            });
        } catch (Exception e) {
            log.error("Error in executeMatchScript", e);
            return null;
        }
    }
}
