package com.streamflow.processor.exception;

public class StreamTopologyException extends RuntimeException {

    public StreamTopologyException(String message) {
        super(message);
    }

    public StreamTopologyException(String message, Throwable cause) {
        super(message, cause);
    }
}
