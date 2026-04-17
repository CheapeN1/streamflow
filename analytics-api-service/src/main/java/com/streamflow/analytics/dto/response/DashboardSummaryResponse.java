package com.streamflow.analytics.dto.response;

import lombok.Builder;

import java.time.Instant;
import java.util.List;

@Builder
public record DashboardSummaryResponse(
        long activeUsers,
        long totalEvents5min,
        List<LeaderboardEntry> topClickedProducts,
        Instant generatedAt
) {}
