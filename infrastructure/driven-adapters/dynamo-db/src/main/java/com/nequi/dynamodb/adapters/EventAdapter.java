package com.nequi.dynamodb.adapters;

import com.nequi.dynamodb.repositories.EventDynamoRepository;
import com.nequi.model.enums.GeneralMessage;
import com.nequi.model.event.Event;
import com.nequi.model.event.gateways.EventGateway;
import com.nequi.model.exception.TechnicalException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static com.nequi.dynamodb.mapper.DynamoMapper.MAPPER;
import static net.logstash.logback.argument.StructuredArguments.kv;

@Component
@AllArgsConstructor
@Slf4j
public class EventAdapter implements EventGateway {

    private final EventDynamoRepository eventDynamoRepository;

    @Override
    public Mono<String> createEvent(Event event, String requestId) {
        var eventId = UUID.randomUUID().toString();
        var eventDto = MAPPER.toEventDto(event, eventId);
        return eventDynamoRepository.save(eventDto)
                .doOnSubscribe(sub -> log.info("Dynamo save Request", kv("event", eventDto)))
                .doOnSuccess(savedEvent-> log.info("Dynamo save Response", kv("savedEvent", savedEvent)))
                .doOnError(error -> log.info("Dynamo save Error", kv("error", error.getMessage())))
                .onErrorMap(error -> new TechnicalException(error, GeneralMessage.INTERNAL_SERVER_ERROR))
                .thenReturn(eventId);
    }

    @Override
    public Mono<Void> updateEvent(String eventId) {
        var updatedEventDto = MAPPER.toUpdateEventDto(eventId);
        return eventDynamoRepository.update(updatedEventDto)
                .doOnSubscribe(sub -> log.info("Dynamo Update Request", kv("updateEvent", updatedEventDto)))
                .doOnSuccess(updatedEvent-> log.info("Dynamo Update Response", kv("updatedEvent", updatedEvent)))
                .doOnError(error -> log.info("Dynamo Update Error", kv("error", error.getMessage())))
                .onErrorMap(error -> new TechnicalException(error, GeneralMessage.INTERNAL_SERVER_ERROR))
                .then();
    }

}
