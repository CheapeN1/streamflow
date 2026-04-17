package com.streamflow.analytics.model.entity;

import com.streamflow.analytics.model.enums.EventType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "user_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "session_id", nullable = false, length = 64)
    private String sessionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 30)
    private EventType action;

    @Column(name = "product_id")
    private Long productId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Column(name = "event_time", nullable = false)
    private Instant eventTime;

    @Column(name = "ingested_at", updatable = false)
    private Instant ingestedAt;

    @PrePersist
    void prePersist() {
        if (ingestedAt == null) {
            ingestedAt = Instant.now();
        }
    }
}
