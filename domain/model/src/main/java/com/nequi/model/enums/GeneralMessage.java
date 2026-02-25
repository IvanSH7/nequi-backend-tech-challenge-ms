package com.nequi.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Getter
@RequiredArgsConstructor
public enum GeneralMessage {

    SUCCESS("200", "NEQ200",  "OK"),
    CREATED("201", "NEQ201",  "Created"),
    ACCEPTED("202", "NEQ202",  "Accepted"),

    // External Errors
    BAD_REQUEST("400", "NEQ400", "Bad Request"),
    NOT_FOUND("404", "NEQ404", "Not Found"),
    CONFLICT("429", "NEQ409", "Conflict"),
    PRECONDITION_FAILED("412", "NEQ412", "Precondition Failed"),
    UNPROCESSABLE_CONTENT("422", "NEQ422", "Unprocessable Content"),

    INTERNAL_SERVER_ERROR("500", "N500000", "Internal Server Error"),
    SERVICE_UNAVAILABLE_ERROR("503", "N503000", "Service Unavailable"),


    // ###### Internal Errors

    // ### REQUESTS VALIDATION
    INVALID_REQUEST_ID(BAD_REQUEST.code, "VAL001", "Invalid x-request-id header"),
    INVALID_ID(BAD_REQUEST.code, "VAL002", "Invalid id"),
    INVALID_EVENT_NAME(BAD_REQUEST.code, "VAL003", "Invalid event name"),
    INVALID_EVENT_DATE(BAD_REQUEST.code, "VAL004", "Invalid event date"),
    INVALID_EVENT_PLACE(BAD_REQUEST.code, "VAL005", "Invalid event place"),
    INVALID_EVENT_CAPACITY(BAD_REQUEST.code, "VAL006", "Invalid event capacity"),
    INVALID_ORDER_QUANTITY(BAD_REQUEST.code, "VAL007", "Invalid order quantity"),

    // ### ASYNC ERRORS
    EVENT_NOT_FOUND( "", "ENF000", "Event Not Found"),
    ORDER_NOT_FOUND( "", "ONF000", "Order Not Found"),
    UNAVAILABLE_TICKETS( "", "OPE000", "Unavailable Tickets");


    private final String code;
    private final String externalCode;
    private final String externalMessage;

    private static final Map<String, GeneralMessage> BY_GENERAL_MESSAGE = new HashMap<>();

    static {
        for (GeneralMessage generalMessage: values()) {
            BY_GENERAL_MESSAGE.put(generalMessage.externalCode, generalMessage);
        }
    }

    public static GeneralMessage findByExternalCode(String externalCode) {
        if (Objects.isNull(externalCode) || Objects.isNull(BY_GENERAL_MESSAGE.get(externalCode))) {
            return GeneralMessage.INTERNAL_SERVER_ERROR;
        }
        return BY_GENERAL_MESSAGE.get(externalCode);
    }

}
