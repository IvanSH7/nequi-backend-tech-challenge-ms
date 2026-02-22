package com.nequi.api.controllers;

import com.nequi.api.validator.HandlerValidator;
import com.nequi.model.enums.GeneralMessage;
import com.nequi.model.enums.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import static com.nequi.api.mapper.HandlerMapper.MAPPER;
import static com.nequi.api.util.Utils.buildResponse;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueryEventController {

    public Mono<ServerResponse> execute(String eventId, String requestId) {
        return HandlerValidator.validateQuery(eventId, requestId)
                .filter(Errors::hasErrors)
                .flatMap(errors -> buildResponse(Operation.QUERY_EVENT, GeneralMessage.BAD_REQUEST, requestId, errors))
                .switchIfEmpty(Mono.defer(() -> buildResponse(Operation.QUERY_EVENT, GeneralMessage.SUCCESS, requestId, MAPPER.toQueryEventsDataResponse(""))));
    }

}
