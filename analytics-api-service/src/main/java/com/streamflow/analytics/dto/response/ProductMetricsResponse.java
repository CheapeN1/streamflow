package com.streamflow.analytics.dto.response;

import com.streamflow.analytics.model.enums.WindowType;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;

@Builder
public record ProductMetricsResponse(
        Long productId,
        WindowType windowType,
        Instant windowStart,
        Instant windowEnd,
        long clickCount,
        long cartCount,
        long purchaseCount,
        long exitCount,
        BigDecimal conversionRate
) {}
