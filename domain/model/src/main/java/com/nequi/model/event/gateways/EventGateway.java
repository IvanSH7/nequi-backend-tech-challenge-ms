package com.nequi.model.event.gateways;

import com.nequi.model.event.Event;
import reactor.core.publisher.Mono;

import java.util.List;

public interface EventGateway {

    Mono<Void> createEvent(Event event, String eventId);
    Mono<Void> updateEvent(String eventId, String status);
    Mono<List<Event>> queryEvents();
    Mono<Event> getEvent(String eventId);

}
