package com.nequi.dynamodb.adapters;

import com.nequi.dynamodb.repositories.OrderDynamoRepository;
import com.nequi.model.enums.GeneralMessage;
import com.nequi.model.exception.TechnicalException;
import com.nequi.model.order.Order;
import com.nequi.model.order.gateways.OrderGateway;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

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
                .doOnSubscribe(sub -> log.info("Dynamo Save Order Request", kv("order", orderDto)))
                .doOnSuccess(savedOrder-> log.info("Dynamo Save Order Response", kv("savedOrder", savedOrder)))
                .doOnError(error -> log.info("Dynamo Save Order Error", kv("error", error.getMessage())))
                .onErrorMap(error -> new TechnicalException(error, GeneralMessage.INTERNAL_SERVER_ERROR))
                .map(MAPPER::toDomainOrder);
    }

}
