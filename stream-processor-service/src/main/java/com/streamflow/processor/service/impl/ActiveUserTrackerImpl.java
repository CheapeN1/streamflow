package com.streamflow.processor.service.impl;

import com.streamflow.processor.service.ActiveUserTracker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Redis-backed active user tracker.
 *
 * <p>Strategy: each active user gets a key "active_user:{userId}" with 30-minute TTL.
 * A separate counter key "active_users:current" is updated atomically.
 * The Micrometer gauge in MetricsConfig reads this counter for Prometheus.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActiveUserTrackerImpl implements ActiveUserTracker {

    private static final String USER_KEY_PREFIX  = "active_user:";
    private static final String ACTIVE_COUNT_KEY = "active_users:current";
    private static final Duration USER_TTL       = Duration.ofMinutes(30);

    private final StringRedisTemplate redisTemplate;

    @Override
    public void recordActivity(Long userId) {
        if (userId == null) return;
        try {
            String userKey = USER_KEY_PREFIX + userId;
            Boolean isNew = redisTemplate.opsForValue().setIfAbsent(userKey, "1", USER_TTL);

            if (Boolean.TRUE.equals(isNew)) {
                // First time this user is seen in the window — increment counter
                redisTemplate.opsForValue().increment(ACTIVE_COUNT_KEY);
                log.debug("New active user: userId={}", userId);
            } else {
                // Already active — just refresh TTL
                redisTemplate.expire(userKey, USER_TTL);
            }
        } catch (Exception e) {
            log.warn("Failed to record activity for userId={}", userId, e);
        }
    }

    @Override
    public long getActiveUserCount() {
        try {
            String raw = redisTemplate.opsForValue().get(ACTIVE_COUNT_KEY);
            return raw != null ? Long.parseLong(raw) : 0L;
        } catch (Exception e) {
            return 0L;
        }
    }
}
