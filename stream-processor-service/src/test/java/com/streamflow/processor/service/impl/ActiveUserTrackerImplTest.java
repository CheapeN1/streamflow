package com.streamflow.processor.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActiveUserTrackerImplTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    private ActiveUserTrackerImpl tracker;

    @BeforeEach
    void setUp() {
        // lenient: not every test triggers opsForValue() (e.g. null-userId guard returns early)
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
        tracker = new ActiveUserTrackerImpl(redisTemplate);
    }

    @Test
    void givenNewUser_whenRecordActivity_thenCounterIncremented() {
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenReturn(Boolean.TRUE);

        tracker.recordActivity(1L);

        verify(valueOps).increment("active_users:current");
    }

    @Test
    void givenExistingUser_whenRecordActivity_thenTtlRefreshedNotIncremented() {
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenReturn(Boolean.FALSE);

        tracker.recordActivity(1L);

        verify(valueOps, never()).increment(anyString());
        verify(redisTemplate).expire(eq("active_user:1"), any(Duration.class));
    }

    @Test
    void givenNullUserId_whenRecordActivity_thenNoRedisCall() {
        tracker.recordActivity(null);

        verify(valueOps, never()).setIfAbsent(anyString(), anyString(), any());
    }

    @Test
    void givenActiveUsersInRedis_whenGetCount_thenCorrectValueReturned() {
        when(valueOps.get("active_users:current")).thenReturn("42");

        long count = tracker.getActiveUserCount();

        assertThat(count).isEqualTo(42L);
    }

    @Test
    void givenNoActiveUsersKey_whenGetCount_thenZeroReturned() {
        when(valueOps.get("active_users:current")).thenReturn(null);

        long count = tracker.getActiveUserCount();

        assertThat(count).isZero();
    }

    @Test
    void givenRedisException_whenRecordActivity_thenNoExceptionPropagated() {
        when(valueOps.setIfAbsent(anyString(), anyString(), any()))
                .thenThrow(new RuntimeException("Redis connection error"));

        // Should NOT throw
        tracker.recordActivity(99L);
    }
}
