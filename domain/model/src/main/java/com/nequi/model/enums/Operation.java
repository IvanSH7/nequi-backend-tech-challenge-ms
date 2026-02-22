package com.nequi.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Getter
@RequiredArgsConstructor
public enum Operation {

    CREATE_EVENT("create-event", "/api/v1/events", "Create Event Request", "createEventRQ", "Create Event Response", "createEventRS"),
    QUERY_EVENTS("query-events", "/api/v1/events", "Query Events Request", "queryEventsRQ", "Query Events Response", "queryEventsRS"),
    QUERY_EVENT("query-event", "/api/v1/events/{id}", "Query Event Request", "queryEventRQ", "Query Event Response", "queryEventRS"),
    QUERY_AVAILABILITY("query-availability", "/api/v1/events/{id}/availability", "Query Availability Request", "queryAvailabilitytRQ", "Query Availability Response", "queryAvailabilityRS"),
    CREATE_ORDER("create-order", "/api/v1/orders", "Create Order Request", "createOrderRQ", "Create Order Response", "createOrderRS"),
    QUERY_ORDER("query-order", "/api/v1/orders/{id}", "Query Order Request", "queryOrderRQ", "Query Order Response", "queryOrderRS"),
    PAY_ORDER("pay-order", "/api/v1/orders/{id}/pay", "Pay Order Request", "payOrderRQ", "Pay Order Response", "payOrderRS");


    private final String name;
    private final String path;
    private final String nameRequest;
    private final String kvRequest;
    private final String nameResponse;
    private final String kvResponse;

    private static final Map<String, Operation> BY_OPERATION = new HashMap<>();

    static {
        for (Operation operation : values()) {
            BY_OPERATION.put(operation.path, operation);
        }
    }

    public static Operation findByPath(String path) {
        for (Operation operation : values()) {
            if (pathMatches(path, operation.path)) {
                return operation;
            }
        }
        throw new IllegalArgumentException(String.format("Operation %s is not registered", path));
    }

    private static boolean pathMatches(String actualPath, String templatePath) {
        String[] actualSegments = actualPath.split("/");
        String[] templateSegments = templatePath.split("/");

        if (actualSegments.length != templateSegments.length) {
            return false;
        }

        for (int i = 0; i < actualSegments.length; i++) {
            if (!templateSegments[i].startsWith("{") && !actualSegments[i].equals(templateSegments[i])) {
                return false;
            }
        }
        return true;
    }
}
