package com.streamflow.processor.service.impl;

import com.streamflow.processor.exception.MetricsWriteException;
import com.streamflow.processor.model.event.ProductMetricEvent;
import com.streamflow.processor.model.event.SessionMetricEvent;
import com.streamflow.processor.service.RedisMetricsWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisMetricsWriterImpl implements RedisMetricsWriter {

    private static final String PRODUCT_METRICS_PREFIX = "metrics:product:";
    private static final String LEADERBOARD_CLICKS     = "leaderboard:clicks:5min";
    private static final String ACTIVE_USERS_KEY       = "active_users:current";
    private static final String WINDOW_PREFIX          = "metrics:window:";

    private final StringRedisTemplate redisTemplate;

    @Override
    @Retryable(
        retryFor = MetricsWriteException.class,
        maxAttempts = 3,
        backoff = @Backoff(delay = 500, multiplier = 2)
    )
    public void writeProductMetricToRedis(ProductMetricEvent event) {
        try {
            String hashKey = PRODUCT_METRICS_PREFIX + event.productId();

            Map<String, String> fields = new HashMap<>();
            fields.put("click_count",    String.valueOf(event.clickCount()));
            fields.put("cart_count",     String.valueOf(event.cartCount()));
            fields.put("purchase_count", String.valueOf(event.purchaseCount()));
            fields.put("exit_count",     String.valueOf(event.exitCount()));
            fields.put("window_type",    event.windowType().name());
            fields.put("window_start",   event.windowStart().toString());
            fields.put("window_end",     event.windowEnd().toString());
            if (event.conversionRate() != null) {
                fields.put("conversion_rate", event.conversionRate().toPlainString());
            }

            redisTemplate.opsForHash().putAll(hashKey, fields);

            // Update click leaderboard only for tumbling window (canonical ranking)
            if (event.windowType().name().startsWith("TUMBLING")) {
                redisTemplate.opsForZSet().add(
                        LEADERBOARD_CLICKS,
                        String.valueOf(event.productId()),
                        event.clickCount()
                );
            }

            // Window snapshot
            String windowKey = WINDOW_PREFIX + event.windowStart().toEpochMilli();
            redisTemplate.opsForHash().put(
                    windowKey,
                    "product:" + event.productId(),
                    String.valueOf(event.clickCount())
            );

            log.debug("Redis write OK — product={} type={} clicks={}",
                    event.productId(), event.windowType(), event.clickCount());

        } catch (Exception e) {
            log.error("Redis write failed for productId={}", event.productId(), e);
            throw new MetricsWriteException("Redis write failed for product: " + event.productId(), e);
        }
    }

    @Override
    @Retryable(
        retryFor = MetricsWriteException.class,
        maxAttempts = 3,
        backoff = @Backoff(delay = 500, multiplier = 2)
    )
    public void writeSessionMetricToRedis(SessionMetricEvent event) {
        try {
            String hashKey = "metrics:session:" + event.sessionId();

            Map<String, String> fields = new HashMap<>();
            fields.put("total_events",  String.valueOf(event.totalEvents()));
            fields.put("has_purchase",  String.valueOf(event.hasPurchase()));
            fields.put("window_start",  event.windowStart().toString());
            fields.put("window_end",    event.windowEnd().toString());

            redisTemplate.opsForHash().putAll(hashKey, fields);

        } catch (Exception e) {
            log.error("Redis session write failed for sessionId={}", event.sessionId(), e);
            throw new MetricsWriteException("Redis write failed for session: " + event.sessionId(), e);
        }
    }

    @Override
    public void incrementActiveUsers(Long userId) {
        redisTemplate.opsForValue().increment(ACTIVE_USERS_KEY);
    }

    @Override
    public void decrementActiveUsers(Long userId) {
        redisTemplate.opsForValue().decrement(ACTIVE_USERS_KEY);
    }
}
