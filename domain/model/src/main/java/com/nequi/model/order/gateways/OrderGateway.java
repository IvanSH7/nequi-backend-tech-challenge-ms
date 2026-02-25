package com.nequi.model.order.gateways;

import com.nequi.model.order.Order;
import reactor.core.publisher.Mono;

public interface OrderGateway {

    Mono<Order> createOrder(Order order);
    Mono<Void> updateOrder(String id, String status);
    Mono<Order> getOrder(String orderId);

}
