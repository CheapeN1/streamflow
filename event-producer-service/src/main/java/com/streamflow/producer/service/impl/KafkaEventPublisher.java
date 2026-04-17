package com.streamflow.producer.service.impl;

import com.streamflow.producer.model.event.UserEvent;
import com.streamflow.producer.service.EventPublisher;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Kafka implementation of EventPublisher.
 * Tracks per-event-type counters and exposes them to Prometheus via Micrometer.
 */
@Slf4j
@Service
public class KafkaEventPublisher implements EventPublisher {

    private final KafkaTemplate<String, UserEvent> kafkaTemplate;
    private final String userEventsTopic;
    private final String deadLetterTopic;
    private final MeterRegistry meterRegistry;
    private final Counter publishedCounter;
    private final Counter failedCounter;

    public KafkaEventPublisher(
            KafkaTemplate<String, UserEvent> kafkaTemplate,
            @Value("${streamflow.kafka.topics.user-events}") String userEventsTopic,
            @Value("${streamflow.kafka.topics.dead-letter-queue}") String deadLetterTopic,
            MeterRegistry meterRegistry) {
        this.kafkaTemplate    = kafkaTemplate;
        this.userEventsTopic  = userEventsTopic;
        this.deadLetterTopic  = deadLetterTopic;
        this.meterRegistry    = meterRegistry;

        this.publishedCounter = Counter.builder("streamflow.events.published.total")
                .description("Total events successfully published to Kafka")
                .register(meterRegistry);

        this.failedCounter = Counter.builder("streamflow.events.failed.total")
                .description("Total events failed to publish")
                .register(meterRegistry);
    }

    @Override
    public void publish(UserEvent event) {
        String partitionKey = event.userId() != null
                ? String.valueOf(event.userId())
                : event.sessionId();

        CompletableFuture<SendResult<String, UserEvent>> future =
                kafkaTemplate.send(userEventsTopic, partitionKey, event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish event={} to topic={}",
                        event.eventId(), userEventsTopic, ex);
                failedCounter.increment();
                sendToDeadLetter(event, ex.getMessage());
            } else {
                publishedCounter.increment();
                if (result.getRecordMetadata() != null) {
                    log.debug("Event published: id={} partition={} offset={}",
                            event.eventId(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                }

                // Per event-type counter — Grafana "Event Type Distribution" paneli için
                if (event.eventType() != null) {
                    Counter.builder("streamflow.events.by_type.total")
                            .description("Events published per event type")
                            .tag("event_type", event.eventType().getValue())
                            .register(meterRegistry)
                            .increment();
                }
            }
        });
    }

    @Override
    public void publishBatch(List<UserEvent> events) {
        events.forEach(this::publish);
    }

    private void sendToDeadLetter(UserEvent event, String reason) {
        try {
            kafkaTemplate.send(deadLetterTopic, event.eventId(), event);
            log.warn("Event sent to DLQ: id={} reason={}", event.eventId(), reason);
        } catch (Exception e) {
            log.error("Dead-letter write also failed for event={}", event.eventId(), e);
        }
    }
}
