package com.streamflow.processor.streams.serde;

import com.streamflow.processor.model.event.ProductMetricEvent;
import com.streamflow.processor.model.event.SessionMetricEvent;
import com.streamflow.processor.model.event.UserEvent;
import com.streamflow.processor.streams.processor.EventAggregate;
import com.streamflow.processor.streams.processor.SessionAggregate;
import org.springframework.stereotype.Component;

/**
 * Central factory providing typed Serde instances — avoids scattered new JsonSerde() calls.
 */
@Component
public class SerdeFactory {

    public JsonSerde<UserEvent> userEventSerde() {
        return new JsonSerde<>(UserEvent.class);
    }

    public JsonSerde<EventAggregate> eventAggregateSerde() {
        return new JsonSerde<>(EventAggregate.class);
    }

    public JsonSerde<SessionAggregate> sessionAggregateSerde() {
        return new JsonSerde<>(SessionAggregate.class);
    }

    public JsonSerde<ProductMetricEvent> productMetricEventSerde() {
        return new JsonSerde<>(ProductMetricEvent.class);
    }

    public JsonSerde<SessionMetricEvent> sessionMetricEventSerde() {
        return new JsonSerde<>(SessionMetricEvent.class);
    }
}
