package com.streamflow.producer.mapper;

import com.streamflow.producer.dto.request.UserEventRequest;
import com.streamflow.producer.model.enums.EventType;
import com.streamflow.producer.model.event.UserEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class UserEventMapperTest {

    private UserEventMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new UserEventMapper();
    }

    @Test
    void givenFullRequest_whenMapped_thenAllFieldsPresent() {
        Instant eventTime = Instant.now();
        UserEventRequest request = UserEventRequest.builder()
                .userId(1L)
                .sessionId("my-session")
                .eventType(EventType.CLICK)
                .productId(42L)
                .price(99.99)
                .eventTime(eventTime)
                .build();

        UserEvent event = mapper.toEvent(request);

        assertThat(event.eventId()).isNotNull();
        assertThat(event.userId()).isEqualTo(1L);
        assertThat(event.sessionId()).isEqualTo("my-session");
        assertThat(event.eventType()).isEqualTo(EventType.CLICK);
        assertThat(event.productId()).isEqualTo(42L);
        assertThat(event.price()).isEqualTo(99.99);
        assertThat(event.eventTime()).isEqualTo(eventTime);
    }

    @Test
    void givenNullSessionId_whenMapped_thenSessionIdGenerated() {
        UserEventRequest request = UserEventRequest.builder()
                .userId(1L)
                .eventType(EventType.CLICK)
                .sessionId(null)
                .build();

        UserEvent event = mapper.toEvent(request);

        assertThat(event.sessionId()).isNotNull().isNotBlank();
    }

    @Test
    void givenNullEventTime_whenMapped_thenEventTimeSetToNow() {
        UserEventRequest request = UserEventRequest.builder()
                .userId(1L)
                .eventType(EventType.PURCHASE)
                .eventTime(null)
                .build();

        Instant before = Instant.now().minusSeconds(1);
        UserEvent event = mapper.toEvent(request);
        Instant after = Instant.now().plusSeconds(1);

        assertThat(event.eventTime()).isBetween(before, after);
    }

    @Test
    void givenTwoRequests_whenMapped_thenEventIdsAreDifferent() {
        UserEventRequest request = UserEventRequest.builder()
                .userId(1L)
                .eventType(EventType.CLICK)
                .build();

        UserEvent event1 = mapper.toEvent(request);
        UserEvent event2 = mapper.toEvent(request);

        assertThat(event1.eventId()).isNotEqualTo(event2.eventId());
    }
}
