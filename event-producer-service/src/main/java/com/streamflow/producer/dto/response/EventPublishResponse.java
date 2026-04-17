package com.streamflow.producer.dto.response;

import lombok.Builder;

import java.time.Instant;

@Builder
public record EventPublishResponse(
        String eventId,
        String status,
        Instant publishedAt
) {
    public static EventPublishResponse accepted(String eventId) {
        return EventPublishResponse.builder()
                .eventId(eventId)
                .status("ACCEPTED")
                .publishedAt(Instant.now())
                .build();
    }
}
