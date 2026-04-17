package com.streamflow.analytics.service.impl;

import com.streamflow.analytics.dto.response.DashboardSummaryResponse;
import com.streamflow.analytics.dto.response.LeaderboardEntry;
import com.streamflow.analytics.dto.response.ProductMetricsResponse;
import com.streamflow.analytics.mapper.ProductMetricsMapper;
import com.streamflow.analytics.model.enums.WindowType;
import com.streamflow.analytics.repository.ProductWindowMetricsRepository;
import com.streamflow.analytics.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private static final String LEADERBOARD_KEY  = "leaderboard:clicks:5min";
    private static final String ACTIVE_USERS_KEY = "active_users:current";

    private final StringRedisTemplate redisTemplate;
    private final ProductWindowMetricsRepository productMetricsRepo;
    private final ProductMetricsMapper productMetricsMapper;

    @Override
    public DashboardSummaryResponse getDashboardSummary() {
        long activeUsers = getActiveUserCount();
        List<LeaderboardEntry> leaderboard = getClickLeaderboard(10);

        return DashboardSummaryResponse.builder()
                .activeUsers(activeUsers)
                .totalEvents5min(leaderboard.stream().mapToLong(e -> (long) e.clickScore()).sum())
                .topClickedProducts(leaderboard)
                .generatedAt(Instant.now())
                .build();
    }

    @Override
    public List<LeaderboardEntry> getClickLeaderboard(int topN) {
        Set<ZSetOperations.TypedTuple<String>> rawEntries =
                redisTemplate.opsForZSet().reverseRangeWithScores(LEADERBOARD_KEY, 0, topN - 1);

        if (rawEntries == null || rawEntries.isEmpty()) {
            log.debug("Leaderboard is empty, falling back to DB");
            return getLeaderboardFromDatabase(topN);
        }

        AtomicInteger rank = new AtomicInteger(1);
        return rawEntries.stream()
                .map(entry -> LeaderboardEntry.builder()
                        .productId(Long.parseLong(entry.getValue()))
                        .clickScore(entry.getScore() != null ? entry.getScore() : 0.0)
                        .rank(rank.getAndIncrement())
                        .build())
                .toList();
    }

    @Override
    public List<ProductMetricsResponse> getProductHistory(
            Long productId, WindowType windowType, Instant from, Instant to) {
        return productMetricsRepo
                .findByProductIdAndTimeRange(productId, from, to)
                .stream()
                .filter(e -> e.getWindowType() == windowType)
                .map(productMetricsMapper::toResponse)
                .toList();
    }

    @Override
    public long getActiveUserCount() {
        String raw = redisTemplate.opsForValue().get(ACTIVE_USERS_KEY);
        if (raw == null) return 0L;
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private List<LeaderboardEntry> getLeaderboardFromDatabase(int topN) {
        List<LeaderboardEntry> result = new ArrayList<>();
        AtomicInteger rank = new AtomicInteger(1);

        productMetricsRepo
                .findTopClickedSince(WindowType.TUMBLING_5M, Instant.now().minusSeconds(300),
                        PageRequest.of(0, topN))
                .forEach(entity -> result.add(
                        LeaderboardEntry.builder()
                                .productId(entity.getProductId())
                                .clickScore(entity.getClickCount())
                                .rank(rank.getAndIncrement())
                                .build()));
        return result;
    }
}
