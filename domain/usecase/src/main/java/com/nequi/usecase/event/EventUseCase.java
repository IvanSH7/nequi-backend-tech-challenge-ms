package com.nequi.usecase.event;


import com.nequi.model.enums.GeneralMessage;
import com.nequi.model.event.Event;
import com.nequi.model.event.gateways.EventGateway;
import com.nequi.model.exception.BusinessException;
import com.nequi.model.ticketing.gateways.TicketingGateway;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;


@RequiredArgsConstructor
public class EventUseCase {

    private final EventGateway eventGateway;
    private final TicketingGateway ticketingGateway;

    public Mono<String> create(Event event) {
        return eventGateway.createEvent(event)
                .doOnSuccess(eventId -> ticketingGateway.createTickets(eventId, event.getCapacity())
                        .then(Mono.defer(() -> eventGateway.updateEvent(eventId)))
                        .onErrorResume(error -> Mono.empty())
                        .subscribeOn(Schedulers.boundedElastic())
                        .subscribe()
                );
    }

    public Mono<List<Event>> queryEvents() {
        return eventGateway.queryEvents();
    }

    public Mono<Event> queryEvent(String eventId) {
        return eventGateway.queryEvent(eventId)
                .switchIfEmpty(Mono.defer(() -> Mono.error(new BusinessException(GeneralMessage.NOT_FOUND))));
    }


}
