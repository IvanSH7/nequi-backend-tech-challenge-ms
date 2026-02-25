package com.nequi.api.mapper;

import com.nequi.api.dto.request.CreateEventRequest;
import com.nequi.api.dto.request.CreateOrderRequest;
import com.nequi.api.dto.response.*;
import com.nequi.model.enums.GeneralMessage;
import com.nequi.model.event.Event;
import com.nequi.model.order.Order;
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

    Event toDomain(CreateEventRequest createEventRequest);

    Order toDomain(CreateOrderRequest createOrderRequest);

    @Mapping(target = "id", source = "id")
    CreateDataResponse toCreateDataResponse(String id);

    @Mapping(target = "eventId", source = "event.id")
    QueryEventsDataResponse toQueryEventDataResponse(Event event);

    @Mapping(target = "remainingCapacity", source = "availability")
    QueryAvailabilityDataResponse toQueryAvailabilityDataResponse(String availability);

    @Mapping(target = "status", source = "order.status" )
    QueryOrderDataResponse toQueryOrderDataResponse(Order order);

}
