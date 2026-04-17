package com.streamflow.analytics.controller;

import com.streamflow.analytics.dto.response.DashboardSummaryResponse;
import com.streamflow.analytics.dto.response.LeaderboardEntry;
import com.streamflow.analytics.dto.response.ProductMetricsResponse;
import com.streamflow.analytics.model.enums.WindowType;
import com.streamflow.analytics.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/dashboard")
    public DashboardSummaryResponse getDashboard() {
        return analyticsService.getDashboardSummary();
    }

    @GetMapping("/leaderboard")
    public List<LeaderboardEntry> getLeaderboard(
            @RequestParam(defaultValue = "10") int topN) {
        return analyticsService.getClickLeaderboard(topN);
    }

    @GetMapping("/products/{productId}/history")
    public List<ProductMetricsResponse> getProductHistory(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "TUMBLING_5M") WindowType windowType,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return analyticsService.getProductHistory(productId, windowType, from, to);
    }

    @GetMapping("/active-users")
    public long getActiveUsers() {
        return analyticsService.getActiveUserCount();
    }
}
