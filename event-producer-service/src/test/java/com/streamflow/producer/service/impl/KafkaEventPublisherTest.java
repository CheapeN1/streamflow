package com.streamflow.producer.service.impl;

import com.streamflow.producer.model.enums.EventType;
import com.streamflow.producer.model.event.UserEvent;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaEventPublisherTest {

    @Mock
    private KafkaTemplate<String, UserEvent> kafkaTemplate;

    private KafkaEventPublisher publisher;
    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        publisher = new KafkaEventPublisher(
                kafkaTemplate,
                "user-events",
                "dead-letter-queue",
                meterRegistry);
    }

    @Test
    void givenValidEvent_whenPublish_thenKafkaTemplateCalled() {
        UserEvent event = buildEvent(EventType.CLICK);
        when(kafkaTemplate.send(anyString(), anyString(), any(UserEvent.class)))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        publisher.publish(event);

        verify(kafkaTemplate, times(1)).send(eq("user-events"), anyString(), eq(event));
    }

    @Test
    void givenValidEvent_whenPublishSucceeds_thenPublishedCounterIncremented() {
        UserEvent event = buildEvent(EventType.PURCHASE);
        when(kafkaTemplate.send(anyString(), anyString(), any(UserEvent.class)))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        publisher.publish(event);

        // Allow async whenComplete to run
        double count = meterRegistry.counter("streamflow.events.published.total").count();
        assertThat(count).isEqualTo(1.0);
    }

    @Test
    void givenKafkaFailure_whenPublish_thenDeadLetterCalled() {
        UserEvent event = buildEvent(EventType.CLICK);
        CompletableFuture<SendResult<String, UserEvent>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Kafka down"));

        when(kafkaTemplate.send(eq("user-events"), anyString(), any()))
                .thenReturn(failedFuture);
        when(kafkaTemplate.send(eq("dead-letter-queue"), anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        publisher.publish(event);

        verify(kafkaTemplate, timeout(1000)).send(eq("dead-letter-queue"), anyString(), any());
    }

    @Test
    void givenBatchOfEvents_whenPublishBatch_thenAllSent() {
        List<UserEvent> events = List.of(
                buildEvent(EventType.CLICK),
                buildEvent(EventType.ADD_TO_CART),
                buildEvent(EventType.PURCHASE));

        when(kafkaTemplate.send(anyString(), anyString(), any(UserEvent.class)))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        publisher.publishBatch(events);

        verify(kafkaTemplate, times(3)).send(anyString(), anyString(), any(UserEvent.class));
    }

    @Test
    void givenNullUserId_whenPublish_thenSessionIdUsedAsKey() {
        UserEvent event = UserEvent.builder()
                .eventId("test-id")
                .userId(null)
                .sessionId("my-session")
                .eventType(EventType.CLICK)
                .productId(1L)
                .eventTime(Instant.now())
                .build();

        when(kafkaTemplate.send(anyString(), anyString(), any(UserEvent.class)))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        publisher.publish(event);

        verify(kafkaTemplate).send(eq("user-events"), eq("my-session"), eq(event));
    }

    private UserEvent buildEvent(EventType type) {
        return UserEvent.builder()
                .eventId("event-" + System.nanoTime())
                .userId(1L)
                .sessionId("session-1")
                .eventType(type)
                .productId(42L)
                .eventTime(Instant.now())
                .build();
    }
}
