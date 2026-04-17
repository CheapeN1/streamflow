package com.streamflow.analytics.repository;

import com.streamflow.analytics.model.entity.SessionMetricsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface SessionMetricsRepository extends JpaRepository<SessionMetricsEntity, String> {

    List<SessionMetricsEntity> findByUserId(Long userId);

    @Query("SELECT s FROM SessionMetricsEntity s WHERE s.startTime >= :since ORDER BY s.startTime DESC")
    List<SessionMetricsEntity> findRecentSessions(
            @Param("since") Instant since,
            org.springframework.data.domain.Pageable pageable);

    @Query("SELECT COUNT(s) FROM SessionMetricsEntity s WHERE s.purchased = true AND s.startTime >= :since")
    long countPurchaseSessionsSince(@Param("since") Instant since);
}
