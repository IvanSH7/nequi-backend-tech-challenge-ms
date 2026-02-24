package com.nequi.dynamodb.mapper;

import com.nequi.dynamodb.dto.EventDto;
import com.nequi.dynamodb.dto.OrderDto;
import com.nequi.dynamodb.dto.TicketDto;
import com.nequi.model.event.Event;
import com.nequi.model.order.Order;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import static com.nequi.dynamodb.util.Constants.*;

@Mapper(builder = @Builder(disableBuilder = true))
public interface DynamoMapper {

    DynamoMapper MAPPER = Mappers.getMapper(DynamoMapper.class);

    @Mapping(target = "pk", source = "eventId", qualifiedByName = "buildEventPk")
    @Mapping(target = "sk", constant = SORT_KEY_METADATA)
    @Mapping(target = "type", constant = TYPE_EVENT)
    @Mapping(target = "status", source = "status")
    @Mapping(target = "totalCapacity", source = "event.capacity")
    @Mapping(target = "availableCount", source = "event.capacity")
    @Mapping(target = "name", source = "event.name")
    @Mapping(target = "place", source = "event.place")
    @Mapping(target = "date", source = "event.date")
    EventDto toEventDto(Event event, String eventId, String status);

    @Mapping(target = "pk", source = "eventId", qualifiedByName = "buildEventPk")
    @Mapping(target = "sk", source = "ticketId", qualifiedByName = "buildTicketSk")
    @Mapping(target = "type", constant = TYPE_TICKET)
    @Mapping(target = "status", source = "status")
    TicketDto toTicketDto(String eventId, Integer ticketId, String status);

    @Named("buildEventPk")
    default String buildEventPk(String eventId) {
        return BASE_EVENT_PK.concat(eventId);
    }

    @Named("buildTicketSk")
    default String buildTicketSk(Integer ticketId) {
        return String.format(BASE_TICKET_PK.concat("%06d"), ticketId);
    }

    @Mapping(target = "id", source = "eventDto.pk", qualifiedByName = "decodeId")
    @Mapping(target = "name", source = "eventDto.name")
    @Mapping(target = "place", source = "eventDto.place")
    @Mapping(target = "date", source = "eventDto.date")
    @Mapping(target = "status", source = "eventDto.status")
    @Mapping(target = "capacity", source = "eventDto.totalCapacity")
    @Mapping(target = "availability", source = "eventDto.availableCount")
    Event toDomainEvent(EventDto eventDto);

    @Mapping(target = "pk", source = "orderId", qualifiedByName = "buildOrderPk")
    @Mapping(target = "sk", constant = SORT_KEY_METADATA)
    @Mapping(target = "type", constant = TYPE_ORDER)
    @Mapping(target = "status", source = "status")
    @Mapping(target = "eventId", source = "order.eventId", qualifiedByName = "buildEventPk")
    @Mapping(target = "quantity", source = "order.quantity")
    OrderDto toOrderDto(Order order, String orderId, String status);

    @Mapping(target = "pk", source = "orderId", qualifiedByName = "buildOrderPk")
    @Mapping(target = "sk", constant = SORT_KEY_METADATA)
    @Mapping(target = "status", source = "status")
    @Mapping(target = "expiresAt", source = "ttl")
    OrderDto toUpdateOrderDto(String orderId, String status, Long ttl);

    @Mapping(target = "id", source = "orderDto.pk", qualifiedByName = "decodeId")
    @Mapping(target = "eventId", source = "orderDto.eventId", qualifiedByName = "decodeId")
    @Mapping(target = "quantity", source = "orderDto.quantity")
    @Mapping(target = "status", source = "orderDto.status")
    @Mapping(target = "expiresAt", source = "orderDto.expiresAt")
    Order toDomainOrder(OrderDto orderDto);

    @Named("buildOrderPk")
    default String buildOrderPk(String orderId) {
        return BASE_ORDER_PK.concat(orderId);
    }

    @Named("decodeId")
    default String decodeId(String id) {
        return id.split("#")[1];
    }

}
