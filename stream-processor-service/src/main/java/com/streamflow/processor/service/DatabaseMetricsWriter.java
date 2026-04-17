package com.streamflow.processor.service;

import com.streamflow.processor.model.event.ProductMetricEvent;
import com.streamflow.processor.model.event.SessionMetricEvent;

public interface DatabaseMetricsWriter {
    void writeProductMetricToDatabase(ProductMetricEvent event);
    void writeSessionMetricToDatabase(SessionMetricEvent event);
}
