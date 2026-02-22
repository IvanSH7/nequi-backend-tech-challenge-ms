package com.nequi.model.order.gateways;

import com.nequi.model.event.Event;
import com.nequi.model.order.Order;
import reactor.core.publisher.Mono;

import java.util.List;

public interface OrderGateway {

    Mono<Order> createOrder(Order order);

}
