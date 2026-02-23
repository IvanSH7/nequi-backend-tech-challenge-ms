package com.nequi.api;

import com.nequi.api.controllers.*;
import com.nequi.api.dto.request.CreateEventRequest;
import com.nequi.api.dto.request.CreateOrderRequest;
import com.nequi.model.enums.GeneralMessage;
import com.nequi.model.enums.Operation;
import com.nequi.model.exception.TechnicalException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.Map;

import static com.nequi.api.util.Constants.*;
import static com.nequi.api.util.Constants.FALLBACK_METHOD_NAME;
import static com.nequi.api.util.Utils.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class Handler {

    private final CreateEventController createEventController;
    private final QueryEventsController queryEventsController;
    private final QueryEventController queryEventController;
    private final QueryAvailabilityController queryAvailabilityController;
    private final CreateOrderController createOrderController;
    private final QueryOrderController queryOrderController;
    private final PayOrderController payOrderController;

    @CircuitBreaker(name = CREATE_EVENT_METHOD_NAME, fallbackMethod = FALLBACK_METHOD_NAME)
    public Mono<ServerResponse> createEvent(ServerRequest serverRequest) {
        var requestId = getRequestId(serverRequest);
        return serverRequest.bodyToMono(CreateEventRequest.class)
                .flatMap(createEventRequest -> {
                    logRequest(Operation.CREATE_EVENT, serverRequest.path(), createEventRequest);
                    return createEventController.execute(createEventRequest, requestId);
                });
    }

    @CircuitBreaker(name = QUERY_EVENTS_METHOD_NAME, fallbackMethod = FALLBACK_METHOD_NAME)
    public Mono<ServerResponse> queryEvents(ServerRequest serverRequest) {
        var requestId = getRequestId(serverRequest);
        return Mono.defer(() -> {
            logRequest(Operation.QUERY_EVENTS, serverRequest.path(), null);
            return queryEventsController.execute(requestId);
        });
    }

    @CircuitBreaker(name = QUERY_EVENT_METHOD_NAME, fallbackMethod = FALLBACK_METHOD_NAME)
    public Mono<ServerResponse> queryEvent(ServerRequest serverRequest) {
        var requestId = getRequestId(serverRequest);
        var eventId = serverRequest.pathVariable(PATH_VARIABLE_ID);
        return Mono.defer(() -> {
            logRequest(Operation.QUERY_EVENT, serverRequest.path(), Map.of(PATH_VARIABLE_ID, eventId));
            return queryEventController.execute(eventId, requestId);
        });
    }

    @CircuitBreaker(name = QUERY_AVAILABILITY_METHOD_NAME, fallbackMethod = FALLBACK_METHOD_NAME)
    public Mono<ServerResponse> queryAvailability(ServerRequest serverRequest) {
        var requestId = getRequestId(serverRequest);
        var eventId = serverRequest.pathVariable(PATH_VARIABLE_ID);
        return Mono.defer(() -> {
            logRequest(Operation.QUERY_AVAILABILITY, serverRequest.path(), Map.of(PATH_VARIABLE_ID, eventId));
            return queryAvailabilityController.execute(eventId, requestId);
        });
    }

    @CircuitBreaker(name = CREATE_ORDER_METHOD_NAME, fallbackMethod = FALLBACK_METHOD_NAME)
    public Mono<ServerResponse> createOrder(ServerRequest serverRequest) {
        var requestId = getRequestId(serverRequest);
        return serverRequest.bodyToMono(CreateOrderRequest.class)
                .flatMap(createOrderRequest -> {
                    logRequest(Operation.CREATE_ORDER, serverRequest.path(), createOrderRequest);
                    return createOrderController.execute(createOrderRequest, requestId);
                });
    }

    @CircuitBreaker(name = QUERY_ORDER_METHOD_NAME, fallbackMethod = FALLBACK_METHOD_NAME)
    public Mono<ServerResponse> queryOrder(ServerRequest serverRequest) {
        var requestId = getRequestId(serverRequest);
        var orderId = serverRequest.pathVariable(PATH_VARIABLE_ID);
        return Mono.defer(() -> {
            logRequest(Operation.QUERY_ORDER, serverRequest.path(), Map.of(PATH_VARIABLE_ID, orderId));
            return queryOrderController.execute(orderId, requestId);
        });
    }

    @CircuitBreaker(name = PAY_ORDER_METHOD_NAME, fallbackMethod = FALLBACK_METHOD_NAME)
    public Mono<ServerResponse> payOrder(ServerRequest serverRequest) {
        var requestId = getRequestId(serverRequest);
        var orderId = serverRequest.pathVariable(PATH_VARIABLE_ID);
        return Mono.defer(() -> {
            logRequest(Operation.PAY_ORDER, serverRequest.path(), Map.of(PATH_VARIABLE_ID, orderId));
            return payOrderController.execute(orderId, requestId);
        });
    }

    public Mono<ServerResponse> fallback(ServerRequest serverRequest, Exception exception) {
        return buildResponse(Operation.findByPath(serverRequest.path()),
                GeneralMessage.INTERNAL_SERVER_ERROR, getRequestId(serverRequest));
    }

    public Mono<ServerResponse> fallback(ServerRequest serverRequest, CallNotPermittedException exception) {
        return buildResponse(Operation.findByPath(serverRequest.path()),
                GeneralMessage.SERVICE_UNAVAILABLE_ERROR, getRequestId(serverRequest));
    }

    public Mono<ServerResponse> fallback(ServerRequest serverRequest, TechnicalException exception) {
        return buildResponse(Operation.findByPath(serverRequest.path()),
                exception.getGeneralMessage(), getRequestId(serverRequest));
    }

}
