package com.streamflow.processor.service;

import com.streamflow.processor.model.event.ProductMetricEvent;
import com.streamflow.processor.model.event.SessionMetricEvent;

/**
 * Facade that orchestrates Redis and Database writes from a single call site.
 * Topology classes depend only on this interface, not on storage implementations.
 */
public interface MetricsWriterService {
    void writeProductMetric(ProductMetricEvent event);
    void writeSessionMetric(SessionMetricEvent event);
}
