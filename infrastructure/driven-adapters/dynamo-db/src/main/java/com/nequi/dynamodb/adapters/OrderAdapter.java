package com.nequi.dynamodb.adapters;

import com.nequi.dynamodb.repositories.OrderDynamoRepository;
import com.nequi.dynamodb.util.Utils;
import com.nequi.model.enums.GeneralMessage;
import com.nequi.model.exception.ServiceException;
import com.nequi.model.exception.TechnicalException;
import com.nequi.model.order.Order;
import com.nequi.model.order.gateways.OrderGateway;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.function.Predicate;

import static com.nequi.dynamodb.mapper.DynamoMapper.MAPPER;
import static com.nequi.dynamodb.util.Constants.BASE_ORDER_PK;
import static com.nequi.dynamodb.util.Constants.SORT_KEY_METADATA;
import static com.nequi.model.enums.OrderStates.PENDING_CONFIRMATION;
import static net.logstash.logback.argument.StructuredArguments.kv;

@Component
@AllArgsConstructor
@Slf4j
public class OrderAdapter implements OrderGateway {

    private final OrderDynamoRepository orderDynamoRepository;

    @Override
    public Mono<Order> createOrder(Order order) {
        var orderId = UUID.randomUUID().toString();
        var orderDto = MAPPER.toOrderDto(order, orderId, PENDING_CONFIRMATION.getName());
        return orderDynamoRepository.save(orderDto)
                .doOnSubscribe(sub -> log.info("Dynamo Save Order Request", kv("saveOrderRequest", orderDto)))
                .doOnSuccess(savedOrder-> log.info("Dynamo Save Order Response", kv("saveOrderResponse", savedOrder)))
                .doOnError(error -> log.info("Dynamo Save Order Error", kv("saveOrderError", error.getMessage())))
                .onErrorMap(Utils::handleTechnicalError)
                .map(MAPPER::toDomainOrder);
    }

    @Override
    public Mono<Void> updateOrder(String id, String status) {
        return orderDynamoRepository.getById(BASE_ORDER_PK.concat(id), SORT_KEY_METADATA)
                .switchIfEmpty(Mono.defer(() -> Mono.error(new TechnicalException(GeneralMessage.ORDER_NOT_FOUND))))
                .flatMap(currentOrder -> {
                    currentOrder.setStatus(status);
                    return orderDynamoRepository.update(currentOrder)
                            .doOnSubscribe(sub -> log.info("Dynamo Update Order Request", kv("updateOrderRequest", currentOrder)))
                            .doOnSuccess(updatedOrder-> log.info("Dynamo Update Order Response", kv("updateOrderResponse", updatedOrder)))
                            .doOnError(error -> log.info("Dynamo Update Order Error", kv("updateOrderError", error.getMessage())));
                })
                .onErrorMap(Predicate.not(ServiceException.class::isInstance), Utils::handleTechnicalError)
                .then();
    }

    @Override
    public Mono<Order> getOrder(String orderId) {
        return orderDynamoRepository.getById(BASE_ORDER_PK.concat(orderId), SORT_KEY_METADATA)
                .doOnSubscribe(sub -> log.info("Dynamo Get Order Request", kv("getOrderRequest", orderId)))
                .doOnSuccess(order -> log.info("Dynamo Get Order Response", kv("getOrderResponse", order)))
                .doOnError(error -> log.info("Dynamo Get Order Error", kv("getOrderError", error.getMessage())))
                .onErrorMap(Utils::handleTechnicalError)
                .map(MAPPER::toDomainOrder);
    }

}
