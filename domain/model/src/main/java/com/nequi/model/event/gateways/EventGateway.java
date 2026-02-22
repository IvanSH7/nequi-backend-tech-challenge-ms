package com.nequi.model.event.gateways;

import com.nequi.model.event.Event;
import reactor.core.publisher.Mono;

public interface EventGateway {

    Mono<String> createEvent(Event event, String requestId);

}
