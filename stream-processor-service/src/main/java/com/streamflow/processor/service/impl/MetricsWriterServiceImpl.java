package com.streamflow.processor.service.impl;

import com.streamflow.processor.model.event.ProductMetricEvent;
import com.streamflow.processor.model.event.SessionMetricEvent;
import com.streamflow.processor.service.DatabaseMetricsWriter;
import com.streamflow.processor.service.MetricsWriterService;
import com.streamflow.processor.service.RedisMetricsWriter;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/**
 * Orchestrates Redis and Database writes.
 * Redis failure does NOT block the DB write — resilience by design.
 * Records Prometheus counters and timers for each write operation.
 */
@Slf4j
@Service
public class MetricsWriterServiceImpl implements MetricsWriterService {

    private final RedisMetricsWriter redisMetricsWriter;
    private final DatabaseMetricsWriter databaseMetricsWriter;
    private final MeterRegistry meterRegistry;

    private final Counter windowsProcessedCounter;
    private final Counter sessionsProcessedCounter;
    private final Counter redisFailureCounter;

    public MetricsWriterServiceImpl(
            RedisMetricsWriter redisMetricsWriter,
            DatabaseMetricsWriter databaseMetricsWriter,
            MeterRegistry meterRegistry) {
        this.redisMetricsWriter    = redisMetricsWriter;
        this.databaseMetricsWriter = databaseMetricsWriter;
        this.meterRegistry         = meterRegistry;

        this.windowsProcessedCounter = Counter.builder("streamflow.windows.processed.total")
                .description("Total windowed aggregations written")
                .register(meterRegistry);

        this.sessionsProcessedCounter = Counter.builder("streamflow.sessions.processed.total")
                .description("Total session windows written")
                .register(meterRegistry);

        this.redisFailureCounter = Counter.builder("streamflow.redis.failures.total")
                .description("Total Redis write failures (DB write continued)")
                .register(meterRegistry);
    }

    @Override
    public void writeProductMetric(ProductMetricEvent event) {
        // Per window-type counter — hangi pencere tipinin ne kadar işlendiği
        Counter.builder("streamflow.windows.processed.by_type.total")
                .description("Windowed aggregations written per window type")
                .tag("window_type", event.windowType().name())
                .register(meterRegistry)
                .increment();

        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            redisMetricsWriter.writeProductMetricToRedis(event);
        } catch (Exception e) {
            log.warn("Redis product write failed — continuing to DB. productId={}", event.productId(), e);
            redisFailureCounter.increment();
        }

        databaseMetricsWriter.writeProductMetricToDatabase(event);

        sample.stop(Timer.builder("streamflow.db.write.duration")
                .description("Time taken to write product metric to DB")
                .tag("window_type", event.windowType().name())
                .register(meterRegistry));

        windowsProcessedCounter.increment();

        log.debug("Metric written: productId={} windowType={} clicks={}",
                event.productId(), event.windowType(), event.clickCount());
    }

    @Override
    public void writeSessionMetric(SessionMetricEvent event) {
        try {
            redisMetricsWriter.writeSessionMetricToRedis(event);
        } catch (Exception e) {
            log.warn("Redis session write failed — continuing to DB. sessionId={}", event.sessionId(), e);
            redisFailureCounter.increment();
        }

        databaseMetricsWriter.writeSessionMetricToDatabase(event);
        sessionsProcessedCounter.increment();
    }
}
