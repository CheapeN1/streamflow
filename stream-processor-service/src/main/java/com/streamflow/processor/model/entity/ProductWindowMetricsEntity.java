package com.streamflow.processor.model.entity;

import com.streamflow.processor.model.enums.WindowType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
    name = "product_window_metrics",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_pwm_product_window",
        columnNames = {"product_id", "window_start", "window_type"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductWindowMetricsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "window_start", nullable = false)
    private Instant windowStart;

    @Column(name = "window_end", nullable = false)
    private Instant windowEnd;

    @Enumerated(EnumType.STRING)
    @Column(name = "window_type", nullable = false, length = 20)
    private WindowType windowType;

    @Column(name = "click_count")
    @Builder.Default
    private Long clickCount = 0L;

    @Column(name = "cart_count")
    @Builder.Default
    private Long cartCount = 0L;

    @Column(name = "purchase_count")
    @Builder.Default
    private Long purchaseCount = 0L;

    @Column(name = "exit_count")
    @Builder.Default
    private Long exitCount = 0L;

    @Column(name = "conversion_rate", precision = 5, scale = 4)
    private BigDecimal conversionRate;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
