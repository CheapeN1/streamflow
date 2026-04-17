package com.streamflow.processor.streams.topology;

import com.streamflow.processor.model.event.SessionMetricEvent;
import com.streamflow.processor.model.event.UserEvent;
import com.streamflow.processor.service.MetricsWriterService;
import com.streamflow.processor.streams.processor.SessionAggregate;
import com.streamflow.processor.streams.serde.SerdeFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Session window topology: activity-based windows with 10-minute inactivity gap.
 * Use case: "User session duration and conversion analysis"
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SessionWindowTopology {

    private final SerdeFactory serdeFactory;
    private final MetricsWriterService metricsWriterService;

    @Value("${streamflow.kafka.topics.user-events}")
    private String userEventsTopic;

    @Value("${streamflow.streams.session-inactivity-gap-minutes}")
    private int inactivityGapMinutes;

    @Value("${streamflow.streams.grace-period-minutes}")
    private int gracePeriodMinutes;

    @Bean
    public KStream<String, UserEvent> sessionWindowStream(StreamsBuilder streamsBuilder) {
        KStream<String, UserEvent> sessionEvents = streamsBuilder
                .stream(userEventsTopic, Consumed.with(Serdes.String(), serdeFactory.userEventSerde()))
                .filter((key, event) -> event != null && event.sessionId() != null)
                // Re-key by sessionId for session-level aggregation
                .selectKey((key, event) -> event.sessionId());

        SessionWindows sessionWindow = SessionWindows.ofInactivityGapAndGrace(
                Duration.ofMinutes(inactivityGapMinutes),
                Duration.ofMinutes(gracePeriodMinutes)
        );

        sessionEvents
                .groupByKey(Grouped.with(Serdes.String(), serdeFactory.userEventSerde()))
                .windowedBy(sessionWindow)
                .aggregate(
                        SessionAggregate::new,
                        // Aggregator
                        (sessionId, event, aggregate) -> {
                            if (aggregate.getUserId() == null) {
                                aggregate.setUserId(event.userId());
                            }
                            boolean isPurchase = event.eventType() != null &&
                                    event.eventType().name().equals("PURCHASE");
                            return aggregate.increment(isPurchase);
                        },
                        // Session merger (when two sessions merge)
                        (sessionId, agg1, agg2) -> {
                            agg1.setTotalEvents(agg1.getTotalEvents() + agg2.getTotalEvents());
                            agg1.setHasPurchase(agg1.isHasPurchase() || agg2.isHasPurchase());
                            return agg1;
                        },
                        Materialized.with(Serdes.String(), serdeFactory.sessionAggregateSerde())
                )
                .toStream()
                .foreach((windowedKey, aggregate) -> {
                    if (aggregate == null) return;

                    String sessionId = windowedKey.key();
                    Instant windowStart = Instant.ofEpochMilli(windowedKey.window().start());
                    Instant windowEnd   = Instant.ofEpochMilli(windowedKey.window().end());

                    log.debug("Session window update: sessionId={} events={} purchased={}",
                            sessionId, aggregate.getTotalEvents(), aggregate.isHasPurchase());

                    SessionMetricEvent metricEvent = SessionMetricEvent.builder()
                            .sessionId(sessionId)
                            .userId(aggregate.getUserId())
                            .windowStart(windowStart)
                            .windowEnd(windowEnd)
                            .totalEvents(aggregate.getTotalEvents())
                            .hasPurchase(aggregate.isHasPurchase())
                            .build();

                    metricsWriterService.writeSessionMetric(metricEvent);
                });

        return sessionEvents;
    }
}
