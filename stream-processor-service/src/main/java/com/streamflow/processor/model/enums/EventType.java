package com.streamflow.processor.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum EventType {

    CLICK("CLICK"),
    ADD_TO_CART("ADD_TO_CART"),
    PURCHASE("PURCHASE"),
    EXIT("EXIT"),
    PAGE_VIEW("PAGE_VIEW"),
    SEARCH("SEARCH");

    private final String value;

    EventType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static EventType fromValue(String value) {
        for (EventType type : values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown EventType: " + value);
    }
}
