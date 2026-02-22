package com.nequi.api.controllers;

import com.nequi.api.dto.request.CreateEventRequest;
import com.nequi.api.validator.HandlerValidator;
import com.nequi.model.enums.GeneralMessage;
import com.nequi.model.enums.Operation;
import com.nequi.model.exception.ServiceException;
import com.nequi.usecase.event.EventUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;


import java.util.function.Predicate;

import static com.nequi.api.mapper.HandlerMapper.MAPPER;
import static com.nequi.api.util.Utils.buildResponse;
import static net.logstash.logback.argument.StructuredArguments.kv;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreateEventController {

    private final EventUseCase eventUseCase;

    public Mono<ServerResponse> execute(CreateEventRequest createEventRequest, String requestId) {
        return HandlerValidator.validateCreateEvent(createEventRequest, requestId)
                .filter(Errors::hasErrors)
                .flatMap(errors -> buildResponse(Operation.CREATE_EVENT, GeneralMessage.BAD_REQUEST, requestId, errors))
                .switchIfEmpty(Mono.defer(() ->
                        eventUseCase.create(MAPPER.toDomain(createEventRequest))
                                .flatMap(eventId ->
                                        buildResponse(Operation.CREATE_EVENT, GeneralMessage.SUCCESS, requestId, MAPPER.toCreateDataResponse(eventId)))
                                .doOnError(Predicate.not(ServiceException.class::isInstance), error ->
                                        log.info("Error trying to create an event", kv("Error", error.getMessage())))
                                .onErrorResume(ServiceException.class, error ->
                                        buildResponse(Operation.CREATE_EVENT, error.getGeneralMessage(), requestId))));
    }

}
