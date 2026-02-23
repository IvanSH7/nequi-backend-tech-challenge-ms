package com.nequi.api.controllers;

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

import static com.nequi.api.util.Utils.buildResponse;
import static net.logstash.logback.argument.StructuredArguments.kv;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayOrderController {

    private final OrderUseCase orderUseCase;

    public Mono<ServerResponse> execute(String orderId, String requestId) {
        return HandlerValidator.validateQuery(orderId, requestId)
                .filter(Errors::hasErrors)
                .flatMap(errors -> buildResponse(Operation.PAY_ORDER, GeneralMessage.BAD_REQUEST, requestId, errors))
                .switchIfEmpty(Mono.defer(() ->
                        orderUseCase.payOrder(orderId)
                                .then(Mono.defer(() -> buildResponse(Operation.PAY_ORDER, GeneralMessage.ACCEPTED, requestId)))
                                .doOnError(Predicate.not(ServiceException.class::isInstance), error ->
                                        log.info("Error trying to pay an order", kv("error", error.getMessage())))
                                .onErrorResume(ServiceException.class, error ->
                                        buildResponse(Operation.PAY_ORDER, error.getGeneralMessage(), requestId))));
    }

}
