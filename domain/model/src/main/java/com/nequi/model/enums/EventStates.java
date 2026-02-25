package com.nequi.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;


@Getter
@RequiredArgsConstructor
public enum EventStates {

    CREATING("CREATING"),
    FAILED("FAILED"),
    PUBLISHED("PUBLISHED");

    private final String name;

}
