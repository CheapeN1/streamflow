package com.streamflow.processor.service;

import com.streamflow.processor.model.event.ProductMetricEvent;
import com.streamflow.processor.model.event.SessionMetricEvent;

public interface RedisMetricsWriter {
    void writeProductMetricToRedis(ProductMetricEvent event);
    void writeSessionMetricToRedis(SessionMetricEvent event);
    void incrementActiveUsers(Long userId);
    void decrementActiveUsers(Long userId);
}
