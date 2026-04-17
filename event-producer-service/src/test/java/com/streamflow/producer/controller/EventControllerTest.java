package com.streamflow.producer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.streamflow.producer.dto.request.UserEventRequest;
import com.streamflow.producer.mapper.UserEventMapper;
import com.streamflow.producer.model.enums.EventType;
import com.streamflow.producer.model.event.UserEvent;
import com.streamflow.producer.service.EventPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EventController.class)
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EventPublisher eventPublisher;

    @MockBean
    private UserEventMapper userEventMapper;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    void givenValidRequest_whenPublish_thenAccepted() throws Exception {
        UserEventRequest request = UserEventRequest.builder()
                .userId(1L)
                .eventType(EventType.CLICK)
                .productId(42L)
                .build();

        UserEvent fakeEvent = UserEvent.builder()
                .eventId("test-id")
                .userId(1L)
                .eventType(EventType.CLICK)
                .eventTime(Instant.now())
                .build();

        when(userEventMapper.toEvent(any())).thenReturn(fakeEvent);
        doNothing().when(eventPublisher).publish(any());

        mockMvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.eventId").value("test-id"))
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    @Test
    void givenMissingUserId_whenPublish_thenBadRequest() throws Exception {
        UserEventRequest request = UserEventRequest.builder()
                .eventType(EventType.CLICK)
                .build();

        mockMvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
