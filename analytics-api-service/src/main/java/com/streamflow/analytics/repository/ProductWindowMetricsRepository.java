package com.streamflow.analytics.repository;

import com.streamflow.analytics.model.entity.ProductWindowMetricsEntity;
import com.streamflow.analytics.model.enums.WindowType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ProductWindowMetricsRepository
        extends JpaRepository<ProductWindowMetricsEntity, Long> {

    List<ProductWindowMetricsEntity> findByProductIdAndWindowTypeOrderByWindowStartDesc(
            Long productId, WindowType windowType, Pageable pageable);

    @Query("SELECT p FROM ProductWindowMetricsEntity p " +
           "WHERE p.windowType = :windowType " +
           "AND p.windowStart >= :since " +
           "ORDER BY p.clickCount DESC")
    List<ProductWindowMetricsEntity> findTopClickedSince(
            @Param("windowType") WindowType windowType,
            @Param("since") Instant since,
            Pageable pageable);

    @Query("SELECT p FROM ProductWindowMetricsEntity p " +
           "WHERE p.productId = :productId " +
           "AND p.windowStart BETWEEN :from AND :to " +
           "ORDER BY p.windowStart ASC")
    List<ProductWindowMetricsEntity> findByProductIdAndTimeRange(
            @Param("productId") Long productId,
            @Param("from") Instant from,
            @Param("to") Instant to);
}
