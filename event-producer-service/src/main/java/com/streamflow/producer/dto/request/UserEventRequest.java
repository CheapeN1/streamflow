package com.streamflow.producer.dto.request;

import com.streamflow.producer.model.enums.EventType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;

import java.time.Instant;
import java.util.Map;

@Builder
public record UserEventRequest(
        @NotNull(message = "userId is required")
        @Positive(message = "userId must be positive")
        Long userId,

        String sessionId,

        @NotNull(message = "eventType is required")
        EventType eventType,

        Long productId,
        String categoryId,
        Double price,
        Map<String, Object> metadata,
        Instant eventTime
) {}
