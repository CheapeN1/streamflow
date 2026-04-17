package com.streamflow.producer.mapper;

import com.streamflow.producer.dto.request.UserEventRequest;
import com.streamflow.producer.model.event.UserEvent;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class UserEventMapper {

    public UserEvent toEvent(UserEventRequest request) {
        return UserEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .userId(request.userId())
                .sessionId(request.sessionId() != null
                        ? request.sessionId()
                        : UUID.randomUUID().toString())
                .eventType(request.eventType())
                .productId(request.productId())
                .categoryId(request.categoryId())
                .price(request.price())
                .metadata(request.metadata())
                .eventTime(request.eventTime() != null ? request.eventTime() : Instant.now())
                .build();
    }
}
