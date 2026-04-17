package com.streamflow.processor.streams.topology;

import com.streamflow.processor.model.enums.WindowType;
import com.streamflow.processor.model.event.ProductMetricEvent;
import com.streamflow.processor.model.event.UserEvent;
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
 * Hopping (sliding) window topology: 5-minute windows advancing every 1 minute.
 * Use case: "Rolling 5-minute average updated every minute"
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HoppingWindowTopology {

    private final SerdeFactory serdeFactory;
    private final MetricsWriterService metricsWriterService;

    @Value("${streamflow.kafka.topics.user-events}")
    private String userEventsTopic;

    @Value("${streamflow.streams.hopping-window-minutes}")
    private int windowMinutes;

    @Value("${streamflow.streams.hopping-advance-minutes}")
    private int advanceMinutes;

    @Value("${streamflow.streams.grace-period-minutes}")
    private int gracePeriodMinutes;

    @Bean
    public KStream<String, UserEvent> hoppingWindowStream(StreamsBuilder streamsBuilder) {
        KStream<String, UserEvent> productEvents = streamsBuilder
                .stream(userEventsTopic, Consumed.with(Serdes.String(), serdeFactory.userEventSerde()))
                .filter((key, event) -> event != null && event.isProductEvent())
                .selectKey((key, event) -> String.valueOf(event.productId()));

        // Hopping window: size=5min, advance=1min
        SlidingWindows hoppingWindow = SlidingWindows.ofTimeDifferenceAndGrace(
                Duration.ofMinutes(windowMinutes),
                Duration.ofMinutes(gracePeriodMinutes)
        );

        productEvents
                .groupByKey(Grouped.with(Serdes.String(), serdeFactory.userEventSerde()))
                .windowedBy(hoppingWindow)
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

                    log.debug("Hopping window update: productId={} window=[{} - {}] total={}",
                            productId, windowStart, windowEnd, aggregate.totalEvents());

                    ProductMetricEvent metricEvent = buildMetricEvent(
                            productId, windowStart, windowEnd, aggregate);

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
            long productId, Instant windowStart, Instant windowEnd, EventAggregate agg) {

        BigDecimal conversionRate = agg.getClickCount() > 0
                ? BigDecimal.valueOf(agg.getPurchaseCount())
                        .divide(BigDecimal.valueOf(agg.getClickCount()), 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return ProductMetricEvent.builder()
                .productId(productId)
                .windowType(WindowType.HOPPING_1M)
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
