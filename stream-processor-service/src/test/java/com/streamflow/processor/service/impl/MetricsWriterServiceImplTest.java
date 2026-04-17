package com.streamflow.processor.service.impl;

import com.streamflow.processor.exception.MetricsWriteException;
import com.streamflow.processor.model.enums.WindowType;
import com.streamflow.processor.model.event.ProductMetricEvent;
import com.streamflow.processor.service.DatabaseMetricsWriter;
import com.streamflow.processor.service.RedisMetricsWriter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MetricsWriterServiceImplTest {

    @Mock
    private RedisMetricsWriter redisWriter;

    @Mock
    private DatabaseMetricsWriter dbWriter;

    private MetricsWriterServiceImpl service;

    @BeforeEach
    void setUp() {
        // SimpleMeterRegistry is a real in-memory registry — no broker or external dependency
        MeterRegistry registry = new SimpleMeterRegistry();
        service = new MetricsWriterServiceImpl(redisWriter, dbWriter, registry);
    }

    @Test
    void givenValidMetric_whenWritten_thenBothStoragesReceiveWrite() {
        ProductMetricEvent event = buildMetricEvent();

        service.writeProductMetric(event);

        verify(redisWriter, times(1)).writeProductMetricToRedis(event);
        verify(dbWriter, times(1)).writeProductMetricToDatabase(event);
    }

    @Test
    void givenRedisFailure_whenWritten_thenDbWriteStillProceeds() {
        ProductMetricEvent event = buildMetricEvent();
        doThrow(new MetricsWriteException("Redis down")).when(redisWriter)
                .writeProductMetricToRedis(event);

        service.writeProductMetric(event);

        // Redis failed but DB write must still happen
        verify(dbWriter, times(1)).writeProductMetricToDatabase(event);
    }

    private ProductMetricEvent buildMetricEvent() {
        return ProductMetricEvent.builder()
                .productId(42L)
                .windowType(WindowType.TUMBLING_5M)
                .windowStart(Instant.now().minusSeconds(300))
                .windowEnd(Instant.now())
                .clickCount(10)
                .cartCount(3)
                .purchaseCount(1)
                .exitCount(2)
                .computedAt(Instant.now())
                .build();
    }
}
