package com.nequi.api.mapper;

import com.nequi.api.dto.response.*;
import com.nequi.model.enums.GeneralMessage;
import com.nequi.model.enums.States;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;


@Mapper(imports = { LocalDateTime.class })
public interface HandlerMapper {

    HandlerMapper MAPPER = Mappers.getMapper(HandlerMapper.class);

    @Mapping(target = "status.code", source = "generalMessage.externalCode")
    @Mapping(target = "status.description", source = "generalMessage.externalMessage")
    @Mapping(target = "data", source = "data", defaultExpression = "java(new java.util.HashMap<>())")
    Response toResponse(GeneralMessage generalMessage, Object data);

    @Mapping(target = "id", constant = "d7cfd25b-8f43-4c3e-b5eb-87059dcf5de2")
    CreateDataResponse toCreateDataResponse(String nothing);

    @Mapping(target = "name", constant = "Test")
    @Mapping(target = "date", constant = "24/12/2026")
    @Mapping(target = "place", constant = "Test")
    @Mapping(target = "capacity", constant = "100")
    @Mapping(target = "eventId", constant = "d7cfd25b-8f43-4c3e-b5eb-87059dcf5de2")
    QueryEventsDataResponse toQueryEventsDataResponse(String nothing);

    @Mapping(target = "eventId", constant = "d7cfd25b-8f43-4c3e-b5eb-87059dcf5de2")
    @Mapping(target = "name", constant = "Test")
    @Mapping(target = "remainingCapacity", constant = "50")
    QueryAvailabilityDataResponse toQueryAvailabilityDataResponse(String nothing);

    @Mapping(target = "orderId", constant = "Test")
    @Mapping(target = "state", constant = "RESERVED" )
    QueryOrderDataResponse toQueryOrderDataResponse(String nothing);

}
