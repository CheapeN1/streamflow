package com.streamflow.producer.model.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.streamflow.producer.model.enums.EventType;
import lombok.Builder;

import java.time.Instant;
import java.util.Map;

/**
 * Immutable Kafka event payload representing a single user action.
 * Uses Java 17 record for structural equality and compact syntax.
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserEvent(
        String eventId,
        Long userId,
        String sessionId,
        EventType eventType,
        Long productId,
        String categoryId,
        Double price,
        Map<String, Object> metadata,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Instant eventTime,
        String sourceIp
) {

    /** Returns true if this event is product-related and can be aggregated. */
    public boolean isProductEvent() {
        return productId != null && (
                eventType == EventType.CLICK ||
                eventType == EventType.ADD_TO_CART ||
                eventType == EventType.PURCHASE
        );
    }
}
