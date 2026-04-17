package com.streamflow.processor.config;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import jakarta.annotation.PostConstruct;

/**
 * Registers custom Micrometer gauges for Prometheus scraping.
 *
 * <p>active_users — reads live count from Redis so Prometheus always
 * gets the current value without a separate scrape endpoint.</p>
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class MetricsConfig {

    private static final String ACTIVE_USERS_KEY = "active_users:current";

    private final MeterRegistry meterRegistry;
    private final StringRedisTemplate redisTemplate;

    @PostConstruct
    public void registerGauges() {
        // Active users gauge — Grafana "Active Users" paneli için
        Gauge.builder("streamflow.active_users", this, MetricsConfig::readActiveUsers)
                .description("Current number of active users tracked in Redis")
                .register(meterRegistry);

        log.info("Custom Micrometer gauges registered");
    }

    private double readActiveUsers() {
        try {
            String raw = redisTemplate.opsForValue().get(ACTIVE_USERS_KEY);
            return raw != null ? Double.parseDouble(raw) : 0.0;
        } catch (Exception e) {
            log.warn("Could not read active_users from Redis", e);
            return 0.0;
        }
    }
}
