package com.streamflow.analytics.mapper;

import com.streamflow.analytics.dto.response.ProductMetricsResponse;
import com.streamflow.analytics.model.entity.ProductWindowMetricsEntity;
import org.springframework.stereotype.Component;

@Component
public class ProductMetricsMapper {

    public ProductMetricsResponse toResponse(ProductWindowMetricsEntity entity) {
        return ProductMetricsResponse.builder()
                .productId(entity.getProductId())
                .windowType(entity.getWindowType())
                .windowStart(entity.getWindowStart())
                .windowEnd(entity.getWindowEnd())
                .clickCount(entity.getClickCount() != null ? entity.getClickCount() : 0L)
                .cartCount(entity.getCartCount() != null ? entity.getCartCount() : 0L)
                .purchaseCount(entity.getPurchaseCount() != null ? entity.getPurchaseCount() : 0L)
                .exitCount(entity.getExitCount() != null ? entity.getExitCount() : 0L)
                .conversionRate(entity.getConversionRate())
                .build();
    }
}
