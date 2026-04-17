package com.streamflow.processor.repository;

import com.streamflow.processor.model.entity.ProductWindowMetricsEntity;
import com.streamflow.processor.model.enums.WindowType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface ProductWindowMetricsRepository extends JpaRepository<ProductWindowMetricsEntity, Long> {

    Optional<ProductWindowMetricsEntity> findByProductIdAndWindowStartAndWindowType(
            Long productId, Instant windowStart, WindowType windowType);

    @Query("SELECT p FROM ProductWindowMetricsEntity p " +
           "WHERE p.productId = :productId AND p.windowType = :windowType " +
           "ORDER BY p.windowStart DESC")
    java.util.List<ProductWindowMetricsEntity> findLatestByProductIdAndWindowType(
            @Param("productId") Long productId,
            @Param("windowType") WindowType windowType,
            org.springframework.data.domain.Pageable pageable);
}
