package com.nequi.dynamodb.mapper;

import com.nequi.dynamodb.dto.EventDto;
import com.nequi.dynamodb.dto.TicketDto;
import com.nequi.model.event.Event;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

@Mapper(builder = @Builder(disableBuilder = true))
public interface DynamoMapper {

    String BASE_EVENT_PK = "EVENT#";
    String SK_METADATA = "METADATA";
    String TYPE_EVENT = "Event";

    DynamoMapper MAPPER = Mappers.getMapper(DynamoMapper.class);

    @Mapping(target = "pk", source = "eventId", qualifiedByName = "buildEventPk")
    @Mapping(target = "sk", constant = SK_METADATA)
    @Mapping(target = "type", constant = "Event")
    @Mapping(target = "status", constant = "CREATING")
    @Mapping(target = "totalCapacity", source = "event.capacity")
    @Mapping(target = "availableCount", source = "event.capacity")
    @Mapping(target = "name", source = "event.name")
    @Mapping(target = "place", source = "event.place")
    @Mapping(target = "date", source = "event.date")
    EventDto toEventDto(Event event, String eventId);

    @Mapping(target = "pk", source = "eventId", qualifiedByName = "buildEventPk")
    @Mapping(target = "sk", constant = SK_METADATA)
    @Mapping(target = "status", constant = "PUBLISHED")
    EventDto toUpdateEventDto(String eventId);

    @Mapping(target = "pk", source = "eventId", qualifiedByName = "buildEventPk")
    @Mapping(target = "sk", source = "ticketId", qualifiedByName = "buildTicketSk")
    @Mapping(target = "type", constant = "Ticket")
    @Mapping(target = "status", constant = "AVAILABLE")
    @Mapping(target = "version", constant = "1")
    TicketDto toTicketDto(String eventId, Integer ticketId);

    @Named("buildEventPk")
    default String buildEventPk(String eventId) {
        return BASE_EVENT_PK.concat(eventId);
    }

    @Named("buildTicketSk")
    default String buildTicketSk(Integer ticketId) {
        return String.format("TICKET#%06d", ticketId);
    }

    @Mapping(target = "id", source = "eventDto.pk", qualifiedByName = "decodeId")
    @Mapping(target = "name", source = "eventDto.name")
    @Mapping(target = "place", source = "eventDto.place")
    @Mapping(target = "date", source = "eventDto.date")
    @Mapping(target = "status", source = "eventDto.status")
    @Mapping(target = "capacity", source = "eventDto.totalCapacity")
    @Mapping(target = "availability", source = "eventDto.availableCount")
    Event toDomainQueryEvent(EventDto eventDto);

    @Named("decodeId")
    default String decodeId(String id) {
        return id.split("#")[1];
    }


}
