package com.streamflow.processor.streams.topology;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.streamflow.processor.model.enums.EventType;
import com.streamflow.processor.model.event.ProductMetricEvent;
import com.streamflow.processor.model.event.UserEvent;
import com.streamflow.processor.service.MetricsWriterService;
import com.streamflow.processor.streams.processor.EventAggregate;
import com.streamflow.processor.streams.serde.JsonSerde;
import com.streamflow.processor.streams.serde.SerdeFactory;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TumblingWindowTopologyTest {

    private TopologyTestDriver testDriver;
    private TestInputTopic<String, UserEvent> inputTopic;
    private SerdeFactory serdeFactory;

    @Mock
    private MetricsWriterService metricsWriterService;

    private static final String USER_EVENTS_TOPIC = "user-events";

    @BeforeEach
    void setUp() {
        serdeFactory = new SerdeFactory();
        StreamsBuilder builder = new StreamsBuilder();

        // Build a simplified topology directly for testing
        builder.stream(USER_EVENTS_TOPIC,
                org.apache.kafka.streams.kstream.Consumed.with(
                        Serdes.String(), serdeFactory.userEventSerde()))
                .filter((k, v) -> v != null && v.isProductEvent())
                .selectKey((k, v) -> String.valueOf(v.productId()))
                .groupByKey(org.apache.kafka.streams.kstream.Grouped.with(
                        Serdes.String(), serdeFactory.userEventSerde()))
                .windowedBy(TimeWindows.ofSizeAndGrace(Duration.ofMinutes(5), Duration.ofMinutes(1)))
                .aggregate(
                        EventAggregate::new,
                        (productId, event, agg) -> switch (event.eventType()) {
                            case CLICK -> agg.addClick();
                            case ADD_TO_CART -> agg.addCart();
                            case PURCHASE -> agg.addPurchase();
                            case EXIT -> agg.addExit();
                            default -> agg;
                        },
                        org.apache.kafka.streams.kstream.Materialized.with(
                                Serdes.String(), serdeFactory.eventAggregateSerde())
                )
                .toStream()
                .foreach((windowedKey, aggregate) -> {
                    if (aggregate == null) return;
                    ProductMetricEvent event = ProductMetricEvent.builder()
                            .productId(Long.parseLong(windowedKey.key()))
                            .clickCount(aggregate.getClickCount())
                            .cartCount(aggregate.getCartCount())
                            .purchaseCount(aggregate.getPurchaseCount())
                            .exitCount(aggregate.getExitCount())
                            .computedAt(Instant.now())
                            .build();
                    metricsWriterService.writeProductMetric(event);
                });

        Properties config = new Properties();
        config.put(StreamsConfig.APPLICATION_ID_CONFIG, "test-app");
        config.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:9092");
        config.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        config.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());

        testDriver = new TopologyTestDriver(builder.build(), config);
        inputTopic = testDriver.createInputTopic(
                USER_EVENTS_TOPIC,
                Serdes.String().serializer(),
                serdeFactory.userEventSerde().serializer());
    }

    @AfterEach
    void tearDown() {
        testDriver.close();
    }

    @Test
    void givenClickEvents_whenWindowCloses_thenMetricsWritten() {
        Instant now = Instant.now();

        UserEvent click1 = buildEvent(1L, 42L, EventType.CLICK, now);
        UserEvent click2 = buildEvent(2L, 42L, EventType.CLICK, now.plusSeconds(30));
        UserEvent purchase = buildEvent(3L, 42L, EventType.PURCHASE, now.plusSeconds(60));

        inputTopic.pipeInput("user-1", click1, now);
        inputTopic.pipeInput("user-2", click2, now.plusSeconds(30));
        inputTopic.pipeInput("user-3", purchase, now.plusSeconds(60));

        // Advance time past window to trigger emission
        inputTopic.pipeInput("user-x",
                buildEvent(99L, 99L, EventType.CLICK, now.plusSeconds(400)),
                now.plusSeconds(400));

        ArgumentCaptor<ProductMetricEvent> captor = ArgumentCaptor.forClass(ProductMetricEvent.class);
        verify(metricsWriterService, atLeastOnce()).writeProductMetric(captor.capture());

        // Kafka Streams emits intermediate KTable updates, so take the LAST (final) value for product 42
        ProductMetricEvent captured = captor.getAllValues().stream()
                .filter(e -> e.productId() == 42L)
                .reduce((first, second) -> second)   // last emitted update is the final aggregate
                .orElseThrow(() -> new AssertionError("No metric for product 42"));

        assertThat(captured.clickCount()).isEqualTo(2);
        assertThat(captured.purchaseCount()).isEqualTo(1);
    }

    @Test
    void givenNullEvent_whenFiltered_thenNotProcessed() {
        inputTopic.pipeInput("key", null);
        verifyNoInteractions(metricsWriterService);
    }

    @Test
    void givenEventWithoutProductId_whenFiltered_thenNotProcessed() {
        UserEvent exitEvent = UserEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .userId(1L)
                .sessionId("session-1")
                .eventType(EventType.EXIT)
                .productId(null)  // No product — should be filtered
                .eventTime(Instant.now())
                .build();

        inputTopic.pipeInput("key", exitEvent);
        verifyNoInteractions(metricsWriterService);
    }

    private UserEvent buildEvent(long eventSuffix, long productId, EventType type, Instant time) {
        return UserEvent.builder()
                .eventId("event-" + eventSuffix)
                .userId(100L + eventSuffix)
                .sessionId("session-" + eventSuffix)
                .eventType(type)
                .productId(productId)
                .eventTime(time)
                .build();
    }
}
