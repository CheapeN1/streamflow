package com.streamflow.processor.exception;

public class MetricsWriteException extends RuntimeException {

    public MetricsWriteException(String message) {
        super(message);
    }

    public MetricsWriteException(String message, Throwable cause) {
        super(message, cause);
    }
}
