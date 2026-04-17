package com.streamflow.processor.streams.processor;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EventAggregateTest {

    @Test
    void givenNewAggregate_whenDefaultState_thenAllCountsZero() {
        EventAggregate agg = new EventAggregate();
        assertThat(agg.getClickCount()).isZero();
        assertThat(agg.getCartCount()).isZero();
        assertThat(agg.getPurchaseCount()).isZero();
        assertThat(agg.getExitCount()).isZero();
        assertThat(agg.totalEvents()).isZero();
    }

    @Test
    void givenAggregate_whenAddClick_thenClickCountIncremented() {
        EventAggregate agg = new EventAggregate();
        agg.addClick();
        assertThat(agg.getClickCount()).isEqualTo(1);
        assertThat(agg.totalEvents()).isEqualTo(1);
    }

    @Test
    void givenAggregate_whenAddCart_thenCartCountIncremented() {
        EventAggregate agg = new EventAggregate();
        agg.addCart();
        assertThat(agg.getCartCount()).isEqualTo(1);
    }

    @Test
    void givenAggregate_whenAddPurchase_thenPurchaseCountIncremented() {
        EventAggregate agg = new EventAggregate();
        agg.addPurchase();
        assertThat(agg.getPurchaseCount()).isEqualTo(1);
    }

    @Test
    void givenAggregate_whenAddExit_thenExitCountIncremented() {
        EventAggregate agg = new EventAggregate();
        agg.addExit();
        assertThat(agg.getExitCount()).isEqualTo(1);
    }

    @Test
    void givenMixedEvents_whenTotalEvents_thenSumOfAllCounts() {
        EventAggregate agg = new EventAggregate();
        agg.addClick(); agg.addClick(); agg.addClick(); // 3
        agg.addCart();  agg.addCart();                  // 2
        agg.addPurchase();                              // 1
        agg.addExit();                                  // 1

        assertThat(agg.totalEvents()).isEqualTo(7);
    }

    @Test
    void givenChainedCalls_whenFluent_thenCorrectCounts() {
        EventAggregate agg = new EventAggregate()
                .addClick()
                .addClick()
                .addCart()
                .addPurchase();

        assertThat(agg.getClickCount()).isEqualTo(2);
        assertThat(agg.getCartCount()).isEqualTo(1);
        assertThat(agg.getPurchaseCount()).isEqualTo(1);
        assertThat(agg.totalEvents()).isEqualTo(4);
    }
}
