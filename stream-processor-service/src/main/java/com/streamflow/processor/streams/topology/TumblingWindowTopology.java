package com.streamflow.processor.streams.topology;

import com.streamflow.processor.model.enums.WindowType;
import com.streamflow.processor.model.event.ProductMetricEvent;
import com.streamflow.processor.model.event.UserEvent;
import com.streamflow.processor.service.ActiveUserTracker;
import com.streamflow.processor.service.MetricsWriterService;
import com.streamflow.processor.streams.processor.EventAggregate;
import com.streamflow.processor.streams.serde.SerdeFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;

/**
 * Tumbling window topology: non-overlapping 5-minute windows.
 * Use case: "Most clicked products in the last 5 minutes"
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TumblingWindowTopology {

    private final SerdeFactory serdeFactory;
    private final MetricsWriterService metricsWriterService;
    private final ActiveUserTracker activeUserTracker;

    @Value("${streamflow.kafka.topics.user-events}")
    private String userEventsTopic;

    @Value("${streamflow.kafka.topics.product-metrics}")
    private String productMetricsTopic;

    @Value("${streamflow.streams.tumbling-window-minutes}")
    private int windowMinutes;

    @Value("${streamflow.streams.grace-period-minutes}")
    private int gracePeriodMinutes;

    @Bean
    public KStream<String, UserEvent> tumblingWindowStream(StreamsBuilder streamsBuilder) {
        KStream<String, UserEvent> rawStream = streamsBuilder
                .stream(userEventsTopic, Consumed.with(Serdes.String(), serdeFactory.userEventSerde()));

        // Her event için active user kaydı — userId TTL'li Redis key'e işlenir
        rawStream
                .filter((key, event) -> event != null && event.userId() != null)
                .foreach((key, event) -> activeUserTracker.recordActivity(event.userId()));

        // Filter invalid events and extract product-level events
        KStream<String, UserEvent> productEvents = rawStream
                .filter((key, event) -> event != null && event.eventType() != null)
                .filter((key, event) -> event.isProductEvent())
                .selectKey((key, event) -> String.valueOf(event.productId()));

        // Tumbling window with grace period for late arrivals
        TimeWindows tumblingWindow = TimeWindows.ofSizeAndGrace(
                Duration.ofMinutes(windowMinutes),
                Duration.ofMinutes(gracePeriodMinutes)
        );

        productEvents
                .groupByKey(Grouped.with(Serdes.String(), serdeFactory.userEventSerde()))
                .windowedBy(tumblingWindow)
                .aggregate(
                        EventAggregate::new,
                        (productId, event, aggregate) -> accumulate(event, aggregate),
                        Materialized.with(Serdes.String(), serdeFactory.eventAggregateSerde())
                )
                .toStream()
                .foreach((windowedKey, aggregate) -> {
                    if (aggregate == null) return;

                    long productId = Long.parseLong(windowedKey.key());
                    Instant windowStart = Instant.ofEpochMilli(windowedKey.window().start());
                    Instant windowEnd   = Instant.ofEpochMilli(windowedKey.window().end());

                    log.debug("Tumbling window closed: productId={} window=[{} - {}] total={}",
                            productId, windowStart, windowEnd, aggregate.totalEvents());

                    ProductMetricEvent metricEvent = buildMetricEvent(
                            productId, windowStart, windowEnd, aggregate, WindowType.TUMBLING_5M);

                    metricsWriterService.writeProductMetric(metricEvent);
                });

        return productEvents;
    }

    private EventAggregate accumulate(UserEvent event, EventAggregate agg) {
        return switch (event.eventType()) {
            case CLICK       -> agg.addClick();
            case ADD_TO_CART -> agg.addCart();
            case PURCHASE    -> agg.addPurchase();
            case EXIT        -> agg.addExit();
            default          -> agg;
        };
    }

    private ProductMetricEvent buildMetricEvent(
            long productId, Instant windowStart, Instant windowEnd,
            EventAggregate agg, WindowType windowType) {

        BigDecimal conversionRate = agg.getClickCount() > 0
                ? BigDecimal.valueOf(agg.getPurchaseCount())
                        .divide(BigDecimal.valueOf(agg.getClickCount()), 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return ProductMetricEvent.builder()
                .productId(productId)
                .windowType(windowType)
                .windowStart(windowStart)
                .windowEnd(windowEnd)
                .clickCount(agg.getClickCount())
                .cartCount(agg.getCartCount())
                .purchaseCount(agg.getPurchaseCount())
                .exitCount(agg.getExitCount())
                .conversionRate(conversionRate)
                .computedAt(Instant.now())
                .build();
    }
}
