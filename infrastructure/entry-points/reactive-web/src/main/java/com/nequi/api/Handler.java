package com.nequi.api;

import com.nequi.api.dto.request.CreateEventRequest;
import com.nequi.api.dto.request.CreateOrderRequest;
import com.nequi.api.validator.HandlerValidator;
import com.nequi.model.enums.GeneralMessage;
import com.nequi.model.enums.Operation;
import com.nequi.model.exception.ServiceException;
import com.nequi.model.exception.TechnicalException;
import com.nequi.usecase.event.EventUseCase;
import com.nequi.usecase.order.OrderUseCase;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.function.Predicate;

import static com.nequi.api.mapper.HandlerMapper.MAPPER;
import static com.nequi.api.util.Constants.*;
import static com.nequi.api.util.Constants.FALLBACK_METHOD_NAME;
import static com.nequi.api.util.Utils.*;
import static net.logstash.logback.argument.StructuredArguments.kv;

@Slf4j
@Component
@RequiredArgsConstructor
public class Handler {

    private final EventUseCase eventUseCase;
    private final OrderUseCase orderUseCase;

    @CircuitBreaker(name = CREATE_EVENT_METHOD_NAME, fallbackMethod = FALLBACK_METHOD_NAME)
    public Mono<ServerResponse> createEvent(ServerRequest serverRequest) {
        var requestId = getRequestId(serverRequest);
        return serverRequest.bodyToMono(CreateEventRequest.class)
                .flatMap(createEventRequest -> {
                    logRequest(Operation.CREATE_EVENT, serverRequest.path(), createEventRequest);
                    return HandlerValidator.validateCreateEvent(createEventRequest, requestId)
                            .filter(Errors::hasErrors)
                            .flatMap(errors -> buildResponse(Operation.CREATE_EVENT, GeneralMessage.BAD_REQUEST, requestId, errors))
                            .switchIfEmpty(Mono.defer(() ->
                                    eventUseCase.create(MAPPER.toDomain(createEventRequest))
                                            .flatMap(eventId ->
                                                    buildResponse(Operation.CREATE_EVENT, GeneralMessage.CREATED, requestId, MAPPER.toCreateDataResponse(eventId)))
                                            .doOnError(Predicate.not(ServiceException.class::isInstance), error ->
                                                    log.info("Error trying to create an event", kv(ERROR, error)))
                                            .onErrorResume(ServiceException.class, error ->
                                                    buildResponse(Operation.CREATE_EVENT, error.getGeneralMessage(), requestId))));
                });
    }

    @CircuitBreaker(name = QUERY_EVENTS_METHOD_NAME, fallbackMethod = FALLBACK_METHOD_NAME)
    public Mono<ServerResponse> queryEvents(ServerRequest serverRequest) {
        var requestId = getRequestId(serverRequest);
        return Mono.defer(() -> {
            logRequest(Operation.QUERY_EVENTS, serverRequest.path(), null);
            return HandlerValidator.validateQueryEvents(requestId)
                    .filter(Errors::hasErrors)
                    .flatMap(errors -> buildResponse(Operation.QUERY_EVENTS, GeneralMessage.BAD_REQUEST, requestId, errors))
                    .switchIfEmpty(Mono.defer(() ->
                            eventUseCase.queryEvents()
                                    .flatMap(events ->
                                            buildResponse(Operation.QUERY_EVENTS, GeneralMessage.SUCCESS, requestId,
                                                    events.stream().map(MAPPER::toQueryEventDataResponse).toList()))
                                    .doOnError(Predicate.not(ServiceException.class::isInstance), error ->
                                            log.info("Error trying to query events", kv(ERROR, error)))
                                    .onErrorResume(ServiceException.class, error ->
                                            buildResponse(Operation.QUERY_EVENTS, error.getGeneralMessage(), requestId))));
        });
    }

    @CircuitBreaker(name = QUERY_EVENT_METHOD_NAME, fallbackMethod = FALLBACK_METHOD_NAME)
    public Mono<ServerResponse> queryEvent(ServerRequest serverRequest) {
        var requestId = getRequestId(serverRequest);
        var eventId = serverRequest.pathVariable(PATH_VARIABLE_ID);
        return Mono.defer(() -> {
            logRequest(Operation.QUERY_EVENT, serverRequest.path(), Map.of(PATH_VARIABLE_ID, eventId));
            return HandlerValidator.validateQuery(eventId, requestId)
                    .filter(Errors::hasErrors)
                    .flatMap(errors -> buildResponse(Operation.QUERY_EVENT, GeneralMessage.BAD_REQUEST, requestId, errors))
                    .switchIfEmpty(Mono.defer(() ->
                            eventUseCase.queryEvent(eventId)
                                    .flatMap(event ->
                                            buildResponse(Operation.QUERY_EVENT, GeneralMessage.SUCCESS, requestId, MAPPER.toQueryEventDataResponse(event)))
                                    .doOnError(Predicate.not(ServiceException.class::isInstance), error ->
                                            log.info("Error trying to query an event", kv(ERROR, error)))
                                    .onErrorResume(ServiceException.class, error ->
                                            buildResponse(Operation.QUERY_EVENT, error.getGeneralMessage(), requestId))));
        });
    }

    @CircuitBreaker(name = QUERY_AVAILABILITY_METHOD_NAME, fallbackMethod = FALLBACK_METHOD_NAME)
    public Mono<ServerResponse> queryAvailability(ServerRequest serverRequest) {
        var requestId = getRequestId(serverRequest);
        var eventId = serverRequest.pathVariable(PATH_VARIABLE_ID);
        return Mono.defer(() -> {
            logRequest(Operation.QUERY_AVAILABILITY, serverRequest.path(), Map.of(PATH_VARIABLE_ID, eventId));
            return HandlerValidator.validateQuery(eventId, requestId)
                    .filter(Errors::hasErrors)
                    .flatMap(errors -> buildResponse(Operation.QUERY_AVAILABILITY, GeneralMessage.BAD_REQUEST, requestId, errors))
                    .switchIfEmpty(Mono.defer(() ->
                            eventUseCase.queryEvent(eventId)
                                    .flatMap(event ->
                                            buildResponse(Operation.QUERY_AVAILABILITY, GeneralMessage.SUCCESS, requestId, MAPPER.toQueryAvailabilityDataResponse(event.getAvailability())))
                                    .doOnError(Predicate.not(ServiceException.class::isInstance), error ->
                                            log.info("Error trying to query event's availability", kv(ERROR, error)))
                                    .onErrorResume(ServiceException.class, error ->
                                            buildResponse(Operation.QUERY_AVAILABILITY, error.getGeneralMessage(), requestId))));
        });
    }

    @CircuitBreaker(name = CREATE_ORDER_METHOD_NAME, fallbackMethod = FALLBACK_METHOD_NAME)
    public Mono<ServerResponse> createOrder(ServerRequest serverRequest) {
        var requestId = getRequestId(serverRequest);
        return serverRequest.bodyToMono(CreateOrderRequest.class)
                .flatMap(createOrderRequest -> {
                    logRequest(Operation.CREATE_ORDER, serverRequest.path(), createOrderRequest);
                    return HandlerValidator.validateCreateOrder(createOrderRequest, requestId)
                            .filter(Errors::hasErrors)
                            .flatMap(errors -> buildResponse(Operation.CREATE_ORDER, GeneralMessage.BAD_REQUEST, requestId, errors))
                            .switchIfEmpty(Mono.defer(() ->
                                    orderUseCase.create(MAPPER.toDomain(createOrderRequest))
                                            .flatMap(orderId ->
                                                    buildResponse(Operation.CREATE_ORDER, GeneralMessage.CREATED, requestId, MAPPER.toCreateDataResponse(orderId)))
                                            .doOnError(Predicate.not(ServiceException.class::isInstance), error ->
                                                    log.info("Error trying to create an order", kv(ERROR, error)))
                                            .onErrorResume(ServiceException.class, error ->
                                                    buildResponse(Operation.CREATE_ORDER, error.getGeneralMessage(), requestId))));
                });
    }

    @CircuitBreaker(name = QUERY_ORDER_METHOD_NAME, fallbackMethod = FALLBACK_METHOD_NAME)
    public Mono<ServerResponse> queryOrder(ServerRequest serverRequest) {
        var requestId = getRequestId(serverRequest);
        var orderId = serverRequest.pathVariable(PATH_VARIABLE_ID);
        return Mono.defer(() -> {
            logRequest(Operation.QUERY_ORDER, serverRequest.path(), Map.of(PATH_VARIABLE_ID, orderId));
            return HandlerValidator.validateQuery(orderId, requestId)
                    .filter(Errors::hasErrors)
                    .flatMap(errors -> buildResponse(Operation.QUERY_ORDER, GeneralMessage.BAD_REQUEST, requestId, errors))
                    .switchIfEmpty(Mono.defer(() ->
                            orderUseCase.queryOrder(orderId)
                                    .flatMap(event ->
                                            buildResponse(Operation.QUERY_ORDER, GeneralMessage.SUCCESS, requestId, MAPPER.toQueryOrderDataResponse(event)))
                                    .doOnError(Predicate.not(ServiceException.class::isInstance), error ->
                                            log.info("Error trying to query an Order", kv(ERROR, error)))
                                    .onErrorResume(ServiceException.class, error ->
                                            buildResponse(Operation.QUERY_ORDER, error.getGeneralMessage(), requestId))));
        });
    }

    @CircuitBreaker(name = PAY_ORDER_METHOD_NAME, fallbackMethod = FALLBACK_METHOD_NAME)
    public Mono<ServerResponse> payOrder(ServerRequest serverRequest) {
        var requestId = getRequestId(serverRequest);
        var orderId = serverRequest.pathVariable(PATH_VARIABLE_ID);
        return Mono.defer(() -> {
            logRequest(Operation.PAY_ORDER, serverRequest.path(), Map.of(PATH_VARIABLE_ID, orderId));
            return HandlerValidator.validateQuery(orderId, requestId)
                    .filter(Errors::hasErrors)
                    .flatMap(errors -> buildResponse(Operation.PAY_ORDER, GeneralMessage.BAD_REQUEST, requestId, errors))
                    .switchIfEmpty(Mono.defer(() ->
                            orderUseCase.payOrder(orderId)
                                    .then(Mono.defer(() -> buildResponse(Operation.PAY_ORDER, GeneralMessage.ACCEPTED, requestId)))
                                    .doOnError(Predicate.not(ServiceException.class::isInstance), error ->
                                            log.info("Error trying to pay an order", kv(ERROR, error)))
                                    .onErrorResume(ServiceException.class, error ->
                                            buildResponse(Operation.PAY_ORDER, error.getGeneralMessage(), requestId))));
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
