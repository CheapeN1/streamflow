package com.streamflow.analytics.dto.response;

import lombok.Builder;

@Builder
public record LeaderboardEntry(
        Long productId,
        double clickScore,
        int rank
) {}
