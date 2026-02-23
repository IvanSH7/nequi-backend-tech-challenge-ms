package com.nequi.dynamodb.adapters;

import com.nequi.dynamodb.repositories.OrderDynamoRepository;
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
import static net.logstash.logback.argument.StructuredArguments.kv;

@Component
@AllArgsConstructor
@Slf4j
public class OrderAdapter implements OrderGateway {

    private final OrderDynamoRepository orderDynamoRepository;

    @Override
    public Mono<Order> createOrder(Order order) {
        var orderId = UUID.randomUUID().toString();
        var orderDto = MAPPER.toOrderDto(order, orderId);
        return orderDynamoRepository.save(orderDto)
                .doOnSubscribe(sub -> log.info("Dynamo Save Order Request", kv("saveOrderRequest", orderDto)))
                .doOnSuccess(savedOrder-> log.info("Dynamo Save Order Response", kv("saveOrderResponse", savedOrder)))
                .doOnError(error -> log.info("Dynamo Save Order Error", kv("saveOrderError", error.getMessage())))
                .onErrorMap(error -> new TechnicalException(error, GeneralMessage.INTERNAL_SERVER_ERROR))
                .map(MAPPER::toDomainOrder);
    }

    @Override
    public Mono<Void> updateOrder(String id, String status) {
        return orderDynamoRepository.getById("ORDER#".concat(id), "METADATA")
                .switchIfEmpty(Mono.defer(() -> Mono.error(new TechnicalException(GeneralMessage.ORDER_NOT_FOUND))))
                .flatMap(currentOrder -> {
                    currentOrder.setStatus(status);
                    return orderDynamoRepository.update(currentOrder)
                            .doOnSubscribe(sub -> log.info("Dynamo Update Order Request", kv("updateOrderRequest", currentOrder)))
                            .doOnSuccess(updatedOrder-> log.info("Dynamo Update Order Response", kv("updateOrderResponse", updatedOrder)))
                            .doOnError(error -> log.info("Dynamo Update Order Error", kv("updateOrderError", error.getMessage())));
                })
                .onErrorMap(Predicate.not(ServiceException.class::isInstance),
                        error -> new TechnicalException(error, GeneralMessage.INTERNAL_SERVER_ERROR))
                .then();
    }

    @Override
    public Mono<Order> getOrder(String orderID) {
        return orderDynamoRepository.getById("ORDER#".concat(orderID), "METADATA")
                .doOnSubscribe(sub -> log.info("Dynamo Get Order Request", kv("getOrderRequest", orderID)))
                .doOnSuccess(order -> log.info("Dynamo Get Order Response", kv("getOrderResponse", order)))
                .doOnError(error -> log.info("Dynamo Get Order Error", kv("getOrderError", error.getMessage())))
                .onErrorMap(error -> new TechnicalException(error, GeneralMessage.INTERNAL_SERVER_ERROR))
                .map(MAPPER::toDomainOrder);
    }

}
