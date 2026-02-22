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
    UNPROCESSABLE_CONTENT("422", "NEQ422", "Unprocessable Content"),

    UNAUTHORIZED(GeneralMessage.STATUS_CODE_401, "N401000", "Unauthorized"),
    FORBIDDEN(GeneralMessage.STATUS_CODE_403, "N403000", "Forbidden"),
    NOT_FOUND_1(GeneralMessage.STATUS_CODE_404, "N404001", NOT_FOUND.externalMessage),

    // Internal Errors
    INVALID_REQUEST_ID(BAD_REQUEST.code, "VAL001", "Invalid x-request-id header"),
    INVALID_ID(BAD_REQUEST.code, "VAL002", "Invalid id"),
    INVALID_EVENT_NAME(BAD_REQUEST.code, "VAL003", "Invalid event name"),
    INVALID_EVENT_DATE(BAD_REQUEST.code, "VAL004", "Invalid event date"),
    INVALID_EVENT_PLACE(BAD_REQUEST.code, "VAL005", "Invalid event place"),
    INVALID_EVENT_CAPACITY(BAD_REQUEST.code, "VAL006", "Invalid event capacity"),
    INVALID_ORDER_QUANTITY(BAD_REQUEST.code, "VAL007", "Invalid order quantity"),





    INTERNAL_SERVER_ERROR(GeneralMessage.STATUS_CODE_500, "N500000", "Internal Server Error"),
    SERVICE_UNAVAILABLE_ERROR(GeneralMessage.STATUS_CODE_503, "N503000", "Service Unavailable");


    private static final String STATUS_CODE_401 = "401";
    private static final String STATUS_CODE_403 = "403";
    private static final String STATUS_CODE_404 = "404";
    private static final String STATUS_CODE_500 = "500";
    private static final String STATUS_CODE_503 = "503";

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
