package com.streamflow.processor.streams.processor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Mutable accumulator for session window analysis per userId.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionAggregate {

    private Long userId;

    @Builder.Default
    private long totalEvents = 0L;

    @Builder.Default
    private boolean hasPurchase = false;

    public SessionAggregate increment(boolean isPurchase) {
        totalEvents++;
        if (isPurchase) {
            hasPurchase = true;
        }
        return this;
    }
}
