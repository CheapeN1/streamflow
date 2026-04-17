package com.streamflow.processor.model.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.streamflow.processor.model.enums.WindowType;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Output event published to product-metrics topic after windowed aggregation.
 */
@Builder
public record ProductMetricEvent(
        Long productId,
        WindowType windowType,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Instant windowStart,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Instant windowEnd,
        long clickCount,
        long cartCount,
        long purchaseCount,
        long exitCount,
        BigDecimal conversionRate,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Instant computedAt
) {}
