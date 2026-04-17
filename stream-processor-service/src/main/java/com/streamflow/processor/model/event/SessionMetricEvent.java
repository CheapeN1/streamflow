package com.streamflow.processor.model.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;

import java.time.Instant;

/**
 * Output event published to session-events topic after session window closes.
 */
@Builder
public record SessionMetricEvent(
        String sessionId,
        Long userId,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Instant windowStart,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Instant windowEnd,
        long totalEvents,
        boolean hasPurchase
) {}
