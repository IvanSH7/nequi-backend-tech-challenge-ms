package com.nequi.model.event.gateways;

import com.nequi.model.event.Event;
import reactor.core.publisher.Mono;

import java.util.List;

public interface EventGateway {

    Mono<String> createEvent(Event event);
    Mono<Void> updateEvent(String eventId);
    Mono<List<Event>> queryEvents();
    Mono<Event> queryEvent(String eventId);

}
