package com.nequi.api.controllers;

import com.nequi.api.dto.request.CreateOrderRequest;
import com.nequi.api.validator.HandlerValidator;
import com.nequi.model.enums.GeneralMessage;
import com.nequi.model.enums.Operation;
import com.nequi.model.exception.ServiceException;
import com.nequi.usecase.order.OrderUseCase;
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
public class CreateOrderController {

    private final OrderUseCase orderUseCase;

    public Mono<ServerResponse> execute(CreateOrderRequest createOrderRequest, String requestId) {
        return HandlerValidator.validateCreateOrder(createOrderRequest, requestId)
                .filter(Errors::hasErrors)
                .flatMap(errors -> buildResponse(Operation.CREATE_ORDER, GeneralMessage.BAD_REQUEST, requestId, errors))
                .switchIfEmpty(Mono.defer(() ->
                        orderUseCase.create(MAPPER.toDomain(createOrderRequest))
                                .flatMap(orderId ->
                                        buildResponse(Operation.CREATE_ORDER, GeneralMessage.CREATED, requestId, MAPPER.toCreateDataResponse(orderId)))
                                .doOnError(Predicate.not(ServiceException.class::isInstance), error ->
                                        log.info("Error trying to create an order", kv("Error", error.getMessage())))
                                .onErrorResume(ServiceException.class, error ->
                                        buildResponse(Operation.CREATE_ORDER, error.getGeneralMessage(), requestId))));
    }

}
