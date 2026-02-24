package com.nequi.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;


@Getter
@RequiredArgsConstructor
public enum OrderStates {

    PENDING_CONFIRMATION("PENDING_CONFIRMATION"),
    RESERVED("RESERVED"),
    EXPIRED("EXPIRED"),
    CONFIRMED("CONFIRMED"),
    FAILED("FAILED");

    private final String name;

}
