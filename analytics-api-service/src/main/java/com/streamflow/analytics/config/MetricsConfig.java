package com.streamflow.analytics.config;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import jakarta.annotation.PostConstruct;

/**
 * Exposes custom Micrometer gauges for the analytics service.
 * Prometheus scrapes these via /actuator/prometheus.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class MetricsConfig {

    private static final String ACTIVE_USERS_KEY = "active_users:current";

    private final MeterRegistry meterRegistry;
    private final StringRedisTemplate stringRedisTemplate;

    @PostConstruct
    public void registerGauges() {
        Gauge.builder("streamflow.active_users", this, MetricsConfig::readActiveUsers)
                .description("Current number of active users (read from Redis)")
                .register(meterRegistry);

        log.info("Analytics service custom gauges registered");
    }

    private double readActiveUsers() {
        try {
            String raw = stringRedisTemplate.opsForValue().get(ACTIVE_USERS_KEY);
            return raw != null ? Double.parseDouble(raw) : 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }
}
