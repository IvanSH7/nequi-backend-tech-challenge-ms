package com.nequi.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;


@Getter
@RequiredArgsConstructor
public enum TicketStates {

    AVAILABLE("AVAILABLE"),
    RESERVED("RESERVED"),
    PENDING_CONFIRMATION("PENDING_CONFIRMATION"),
    SOLD("SOLD"),
    COMPLIMENTARY("COMPLIMENTARY");

    private final String name;

}
