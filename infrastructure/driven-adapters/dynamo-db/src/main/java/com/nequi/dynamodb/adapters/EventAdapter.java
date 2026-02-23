package com.nequi.dynamodb.adapters;

import com.nequi.dynamodb.repositories.EventDynamoRepository;
import com.nequi.model.enums.GeneralMessage;
import com.nequi.model.event.Event;
import com.nequi.model.event.gateways.EventGateway;
import com.nequi.model.exception.ServiceException;
import com.nequi.model.exception.TechnicalException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;

import java.util.List;
import java.util.function.Predicate;

import static com.nequi.dynamodb.mapper.DynamoMapper.MAPPER;
import static net.logstash.logback.argument.StructuredArguments.kv;

@Component
@AllArgsConstructor
@Slf4j
public class EventAdapter implements EventGateway {

    private final EventDynamoRepository eventDynamoRepository;

    @Override
    public Mono<Void> createEvent(Event event, String eventId) {
        var eventDto = MAPPER.toEventDto(event, eventId);
        return eventDynamoRepository.save(eventDto)
                .doOnSubscribe(sub -> log.info("Dynamo Save Event Request", kv("saveEventRequest", eventDto)))
                .doOnSuccess(savedEvent-> log.info("Dynamo Save Event Response", kv("saveEventResponse", savedEvent)))
                .doOnError(error -> log.info("Dynamo Save Event Error", kv("saveEventError", error.getMessage())))
                .onErrorMap(error -> new TechnicalException(error, GeneralMessage.INTERNAL_SERVER_ERROR))
                .then();
    }

    @Override
    public Mono<Void> updateEvent(String eventId, String status) {
        return eventDynamoRepository.getById("EVENT#".concat(eventId), "METADATA")
                .switchIfEmpty(Mono.defer(() -> Mono.error(new TechnicalException(GeneralMessage.EVENT_NOT_FOUND))))
                .flatMap(currentEvent -> {
                    currentEvent.setStatus(status);
                    return eventDynamoRepository.update(currentEvent)
                            .doOnSubscribe(sub -> log.info("Dynamo Update Event Request", kv("updateEventRequest", currentEvent)))
                            .doOnSuccess(updateEventResponse-> log.info("Dynamo Update Event Response", kv("updateEventResponse", updateEventResponse)))
                            .doOnError(error -> log.info("Dynamo Update Event Error", kv("updateEventError", error.getMessage())));
                }).onErrorMap(Predicate.not(ServiceException.class::isInstance),
                        error -> new TechnicalException(error, GeneralMessage.INTERNAL_SERVER_ERROR))
                .then();
    }

    @Override
    public Mono<List<Event>> queryEvents() {
        return eventDynamoRepository.queryByIndex(generateQueryExpression(), "EntityTypeIndex")
                .map(events -> events.stream().map(MAPPER::toDomainEvent).toList());
    }

    @Override
    public Mono<Event> getEvent(String eventId) {
        return eventDynamoRepository.getById("EVENT#".concat(eventId), "METADATA")
                .doOnSubscribe(sub -> log.info("Dynamo Get Event Request", kv("getEventRequest", eventId)))
                .doOnSuccess(getEventResponse-> log.info("Dynamo Get Event Response", kv("getEventResponse", getEventResponse)))
                .doOnError(error -> log.info("Dynamo Get Event Error", kv("getEventError", error.getMessage())))
                .onErrorMap(error -> new TechnicalException(error, GeneralMessage.INTERNAL_SERVER_ERROR))
                .map(MAPPER::toDomainEvent);
    }

    private QueryEnhancedRequest generateQueryExpression() {
        return QueryEnhancedRequest.builder()
                .queryConditional(QueryConditional.keyEqualTo(Key.builder().partitionValue("Event").build()))
                .build();
    }

}
