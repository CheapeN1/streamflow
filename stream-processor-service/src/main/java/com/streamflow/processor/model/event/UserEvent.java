package com.streamflow.processor.model.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.streamflow.processor.model.enums.EventType;
import lombok.Builder;

import java.time.Instant;
import java.util.Map;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
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
    public boolean isProductEvent() {
        return productId != null && (
                eventType == EventType.CLICK ||
                eventType == EventType.ADD_TO_CART ||
                eventType == EventType.PURCHASE
        );
    }
}
