package com.streamflow.processor.streams.processor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Mutable accumulator for windowed product event counts.
 * Must be mutable (not a record) for Kafka Streams Aggregator compatibility.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventAggregate {

    @Builder.Default
    private long clickCount = 0L;

    @Builder.Default
    private long cartCount = 0L;

    @Builder.Default
    private long purchaseCount = 0L;

    @Builder.Default
    private long exitCount = 0L;

    public EventAggregate addClick() {
        clickCount++;
        return this;
    }

    public EventAggregate addCart() {
        cartCount++;
        return this;
    }

    public EventAggregate addPurchase() {
        purchaseCount++;
        return this;
    }

    public EventAggregate addExit() {
        exitCount++;
        return this;
    }

    public long totalEvents() {
        return clickCount + cartCount + purchaseCount + exitCount;
    }
}
