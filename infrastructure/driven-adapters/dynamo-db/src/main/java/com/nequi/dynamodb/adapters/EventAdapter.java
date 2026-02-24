package com.nequi.dynamodb.adapters;

import com.nequi.dynamodb.repositories.EventDynamoRepository;
import com.nequi.dynamodb.util.Utils;
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
import static com.nequi.dynamodb.util.Constants.*;
import static com.nequi.model.enums.EventStates.CREATING;
import static net.logstash.logback.argument.StructuredArguments.kv;

@Component
@AllArgsConstructor
@Slf4j
public class EventAdapter implements EventGateway {

    private final EventDynamoRepository eventDynamoRepository;

    @Override
    public Mono<Void> createEvent(Event event, String eventId) {
        var eventDto = MAPPER.toEventDto(event, eventId, CREATING.getName());
        return eventDynamoRepository.save(eventDto)
                .doOnSubscribe(sub -> log.info("Dynamo Save Event Request", kv("saveEventRequest", eventDto)))
                .doOnSuccess(savedEvent-> log.info("Dynamo Save Event Response", kv("saveEventResponse", savedEvent)))
                .doOnError(error -> log.info("Dynamo Save Event Error", kv("saveEventError", error.getMessage())))
                .onErrorMap(Utils::handleTechnicalError)
                .then();
    }

    @Override
    public Mono<Void> updateEvent(String eventId, String status) {
        return eventDynamoRepository.getById(BASE_EVENT_PK.concat(eventId), SORT_KEY_METADATA)
                .switchIfEmpty(Mono.defer(() -> Mono.error(new TechnicalException(GeneralMessage.EVENT_NOT_FOUND))))
                .flatMap(currentEvent -> {
                    currentEvent.setStatus(status);
                    return eventDynamoRepository.update(currentEvent)
                            .doOnSubscribe(sub -> log.info("Dynamo Update Event Request", kv("updateEventRequest", currentEvent)))
                            .doOnSuccess(updateEventResponse-> log.info("Dynamo Update Event Response", kv("updateEventResponse", updateEventResponse)))
                            .doOnError(error -> log.info("Dynamo Update Event Error", kv("updateEventError", error.getMessage())));
                }).onErrorMap(Predicate.not(ServiceException.class::isInstance), Utils::handleTechnicalError)
                .then();
    }

    @Override
    public Mono<List<Event>> queryEvents() {
        var generateQueryExpression = generateQueryExpression();
        return eventDynamoRepository.queryByIndex(generateQueryExpression, TYPE_INDEX)
                .doOnSubscribe(sub -> log.info("Dynamo Query Events Request", kv("queryEventsRequest", generateQueryExpression)))
                .doOnSuccess(events-> log.info("Dynamo Query Events Response", kv("queryEventsResponse", events)))
                .doOnError(error -> log.info("Dynamo Query Events Error", kv("queryEventsError", error.getMessage())))
                .onErrorMap(Utils::handleTechnicalError)
                .map(events -> events.stream().map(MAPPER::toDomainEvent).toList());
    }

    @Override
    public Mono<Event> getEvent(String eventId) {
        return eventDynamoRepository.getById(BASE_EVENT_PK.concat(eventId), SORT_KEY_METADATA)
                .doOnSubscribe(sub -> log.info("Dynamo Get Event Request", kv("getEventRequest", eventId)))
                .doOnSuccess(getEventResponse-> log.info("Dynamo Get Event Response", kv("getEventResponse", getEventResponse)))
                .doOnError(error -> log.info("Dynamo Get Event Error", kv("getEventError", error.getMessage())))
                .onErrorMap(Utils::handleTechnicalError)
                .map(MAPPER::toDomainEvent);
    }

    private QueryEnhancedRequest generateQueryExpression() {
        return QueryEnhancedRequest.builder()
                .queryConditional(QueryConditional.keyEqualTo(Key.builder().partitionValue(TYPE_EVENT).build()))
                .build();
    }

}
