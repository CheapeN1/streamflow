package com.streamflow.analytics.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "session_metrics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionMetricsEntity {

    @Id
    @Column(name = "session_id", length = 64)
    private String sessionId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time")
    private Instant endTime;

    @Column(name = "total_events")
    @Builder.Default
    private Integer totalEvents = 0;

    @Column(name = "pages_visited")
    @Builder.Default
    private Integer pagesVisited = 0;

    @Column(name = "purchased")
    @Builder.Default
    private Boolean purchased = false;
}
