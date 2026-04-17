package com.streamflow.producer.service;

import com.streamflow.producer.model.event.UserEvent;

public interface EventPublisher {
    void publish(UserEvent event);
    void publishBatch(java.util.List<UserEvent> events);
}
