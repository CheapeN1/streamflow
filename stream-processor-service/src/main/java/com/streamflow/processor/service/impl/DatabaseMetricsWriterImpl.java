package com.streamflow.processor.service.impl;

import com.streamflow.processor.exception.MetricsWriteException;
import com.streamflow.processor.model.entity.ProductWindowMetricsEntity;
import com.streamflow.processor.model.entity.SessionMetricsEntity;
import com.streamflow.processor.model.event.ProductMetricEvent;
import com.streamflow.processor.model.event.SessionMetricEvent;
import com.streamflow.processor.repository.ProductWindowMetricsRepository;
import com.streamflow.processor.repository.SessionMetricsRepository;
import com.streamflow.processor.service.DatabaseMetricsWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DatabaseMetricsWriterImpl implements DatabaseMetricsWriter {

    private final ProductWindowMetricsRepository productMetricsRepo;
    private final SessionMetricsRepository sessionMetricsRepo;

    @Override
    @Transactional
    @Retryable(
        retryFor = MetricsWriteException.class,
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void writeProductMetricToDatabase(ProductMetricEvent event) {
        try {
            // Upsert: update existing window record or insert new one
            productMetricsRepo
                    .findByProductIdAndWindowStartAndWindowType(
                            event.productId(), event.windowStart(), event.windowType())
                    .ifPresentOrElse(
                            existing -> {
                                existing.setClickCount(event.clickCount());
                                existing.setCartCount(event.cartCount());
                                existing.setPurchaseCount(event.purchaseCount());
                                existing.setExitCount(event.exitCount());
                                existing.setConversionRate(event.conversionRate());
                                productMetricsRepo.save(existing);
                            },
                            () -> productMetricsRepo.save(
                                    ProductWindowMetricsEntity.builder()
                                            .productId(event.productId())
                                            .windowStart(event.windowStart())
                                            .windowEnd(event.windowEnd())
                                            .windowType(event.windowType())
                                            .clickCount(event.clickCount())
                                            .cartCount(event.cartCount())
                                            .purchaseCount(event.purchaseCount())
                                            .exitCount(event.exitCount())
                                            .conversionRate(event.conversionRate())
                                            .build()
                            )
                    );

            log.debug("DB write OK — product={} type={} window={}",
                    event.productId(), event.windowType(), event.windowStart());

        } catch (Exception e) {
            log.error("DB write failed for productId={} windowType={}",
                    event.productId(), event.windowType(), e);
            throw new MetricsWriteException("DB write failed: " + event.productId(), e);
        }
    }

    @Override
    @Transactional
    @Retryable(
        retryFor = MetricsWriteException.class,
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void writeSessionMetricToDatabase(SessionMetricEvent event) {
        try {
            sessionMetricsRepo
                    .findById(event.sessionId())
                    .ifPresentOrElse(
                            existing -> {
                                existing.setEndTime(event.windowEnd());
                                existing.setTotalEvents((int) event.totalEvents());
                                existing.setPurchased(event.hasPurchase());
                                sessionMetricsRepo.save(existing);
                            },
                            () -> sessionMetricsRepo.save(
                                    SessionMetricsEntity.builder()
                                            .sessionId(event.sessionId())
                                            .userId(event.userId())
                                            .startTime(event.windowStart())
                                            .endTime(event.windowEnd())
                                            .totalEvents((int) event.totalEvents())
                                            .purchased(event.hasPurchase())
                                            .build()
                            )
                    );
        } catch (Exception e) {
            log.error("DB session write failed for sessionId={}", event.sessionId(), e);
            throw new MetricsWriteException("DB session write failed: " + event.sessionId(), e);
        }
    }
}
