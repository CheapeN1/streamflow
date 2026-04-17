package com.streamflow.analytics.service.impl;

import com.streamflow.analytics.dto.response.DashboardSummaryResponse;
import com.streamflow.analytics.dto.response.LeaderboardEntry;
import com.streamflow.analytics.mapper.ProductMetricsMapper;
import com.streamflow.analytics.model.entity.ProductWindowMetricsEntity;
import com.streamflow.analytics.model.enums.WindowType;
import com.streamflow.analytics.repository.ProductWindowMetricsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceImplTest {

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ProductWindowMetricsRepository metricsRepo;
    @Mock private ProductMetricsMapper mapper;
    @Mock private ZSetOperations<String, String> zSetOps;
    @Mock private ValueOperations<String, String> valueOps;

    private AnalyticsServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AnalyticsServiceImpl(redisTemplate, metricsRepo, mapper);
    }

    @Test
    void givenRedisLeaderboard_whenGetLeaderboard_thenRankedListReturned() {
        ZSetOperations.TypedTuple<String> entry1 = mockTuple("42", 10.0);
        ZSetOperations.TypedTuple<String> entry2 = mockTuple("7",  5.0);

        when(redisTemplate.opsForZSet()).thenReturn(zSetOps);
        when(zSetOps.reverseRangeWithScores("leaderboard:clicks:5min", 0, 4))
                .thenReturn(Set.of(entry1, entry2));

        List<LeaderboardEntry> result = service.getClickLeaderboard(5);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).rank()).isEqualTo(1);
    }

    @Test
    void givenEmptyRedis_whenGetLeaderboard_thenFallsBackToDatabase() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOps);
        when(zSetOps.reverseRangeWithScores(anyString(), anyLong(), anyLong()))
                .thenReturn(Set.of());
        when(metricsRepo.findTopClickedSince(any(), any(), any()))
                .thenReturn(List.of());

        List<LeaderboardEntry> result = service.getClickLeaderboard(5);

        assertThat(result).isEmpty();
        verify(metricsRepo).findTopClickedSince(eq(WindowType.TUMBLING_5M), any(), any());
    }

    @Test
    void givenActiveUsersInRedis_whenGetActiveUserCount_thenCorrectValue() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("active_users:current")).thenReturn("25");

        long count = service.getActiveUserCount();

        assertThat(count).isEqualTo(25L);
    }

    @Test
    void givenNoActiveUsersKey_whenGetCount_thenZero() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("active_users:current")).thenReturn(null);

        assertThat(service.getActiveUserCount()).isZero();
    }

    @Test
    void givenLeaderboard_whenGetDashboard_thenSummaryContainsLeaderboard() {
        ZSetOperations.TypedTuple<String> entry = mockTuple("42", 8.0);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOps);
        when(zSetOps.reverseRangeWithScores(anyString(), anyLong(), anyLong()))
                .thenReturn(Set.of(entry));
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("active_users:current")).thenReturn("10");

        DashboardSummaryResponse dashboard = service.getDashboardSummary();

        assertThat(dashboard.activeUsers()).isEqualTo(10L);
        assertThat(dashboard.topClickedProducts()).hasSize(1);
        assertThat(dashboard.generatedAt()).isNotNull();
    }

    @Test
    void givenProductHistory_whenQueried_thenMappedCorrectly() {
        ProductWindowMetricsEntity entity = ProductWindowMetricsEntity.builder()
                .productId(42L)
                .windowType(WindowType.TUMBLING_5M)
                .windowStart(Instant.now().minusSeconds(300))
                .windowEnd(Instant.now())
                .clickCount(5L)
                .purchaseCount(1L)
                .build();

        when(metricsRepo.findByProductIdAndTimeRange(anyLong(), any(), any()))
                .thenReturn(List.of(entity));

        service.getProductHistory(42L, WindowType.TUMBLING_5M,
                Instant.now().minusSeconds(3600), Instant.now());

        verify(mapper).toResponse(entity);
    }

    @SuppressWarnings("unchecked")
    private ZSetOperations.TypedTuple<String> mockTuple(String value, double score) {
        ZSetOperations.TypedTuple<String> tuple = mock(ZSetOperations.TypedTuple.class);
        when(tuple.getValue()).thenReturn(value);
        when(tuple.getScore()).thenReturn(score);
        return tuple;
    }
}
