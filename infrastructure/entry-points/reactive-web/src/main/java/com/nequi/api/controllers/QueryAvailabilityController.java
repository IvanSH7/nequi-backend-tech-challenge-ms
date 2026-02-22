package com.nequi.api.controllers;

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
public class QueryAvailabilityController {

    private final EventUseCase eventUseCase;

    public Mono<ServerResponse> execute(String eventId, String requestId) {
        return HandlerValidator.validateQuery(eventId, requestId)
                .filter(Errors::hasErrors)
                .flatMap(errors -> buildResponse(Operation.QUERY_AVAILABILITY, GeneralMessage.BAD_REQUEST, requestId, errors))
                .switchIfEmpty(Mono.defer(() ->
                        eventUseCase.queryEvent(eventId)
                                .flatMap(event ->
                                        buildResponse(Operation.QUERY_AVAILABILITY, GeneralMessage.SUCCESS, requestId, MAPPER.toQueryAvailabilityDataResponse(event.getAvailability())))
                                .doOnError(Predicate.not(ServiceException.class::isInstance), error ->
                                        log.info("Error trying to query event's availability", kv("Error", error.getMessage())))
                                .onErrorResume(ServiceException.class, error ->
                                        buildResponse(Operation.QUERY_AVAILABILITY, error.getGeneralMessage(), requestId))));
    }

}
