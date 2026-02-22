package com.nequi.model.order.gateways;

import com.nequi.model.order.Order;
import reactor.core.publisher.Mono;

public interface ProcessorGateway {

    Mono<Void> processOrder(Order order);

}
