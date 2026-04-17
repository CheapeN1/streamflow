package com.streamflow.analytics.service;

import com.streamflow.analytics.dto.response.DashboardSummaryResponse;
import com.streamflow.analytics.dto.response.LeaderboardEntry;
import com.streamflow.analytics.dto.response.ProductMetricsResponse;
import com.streamflow.analytics.model.enums.WindowType;

import java.time.Instant;
import java.util.List;

public interface AnalyticsService {
    DashboardSummaryResponse getDashboardSummary();
    List<LeaderboardEntry> getClickLeaderboard(int topN);
    List<ProductMetricsResponse> getProductHistory(Long productId, WindowType windowType, Instant from, Instant to);
    long getActiveUserCount();
}
