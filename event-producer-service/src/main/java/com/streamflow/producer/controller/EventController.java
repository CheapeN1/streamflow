package com.streamflow.producer.controller;

import com.streamflow.producer.dto.request.UserEventRequest;
import com.streamflow.producer.dto.response.EventPublishResponse;
import com.streamflow.producer.mapper.UserEventMapper;
import com.streamflow.producer.model.event.UserEvent;
import com.streamflow.producer.service.EventPublisher;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class EventController {

    private final EventPublisher eventPublisher;
    private final UserEventMapper userEventMapper;

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public EventPublishResponse publishEvent(@Valid @RequestBody UserEventRequest request) {
        UserEvent event = userEventMapper.toEvent(request);
        eventPublisher.publish(event);
        log.info("Event accepted: id={} type={}", event.eventId(), event.eventType());
        return EventPublishResponse.accepted(event.eventId());
    }

    @PostMapping("/batch")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public List<EventPublishResponse> publishBatch(
            @Valid @RequestBody List<UserEventRequest> requests) {
        List<UserEvent> events = requests.stream()
                .map(userEventMapper::toEvent)
                .toList();
        eventPublisher.publishBatch(events);
        log.info("Batch accepted: count={}", events.size());
        return events.stream()
                .map(e -> EventPublishResponse.accepted(e.eventId()))
                .toList();
    }
}
