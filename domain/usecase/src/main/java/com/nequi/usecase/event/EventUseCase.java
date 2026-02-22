package com.nequi.usecase.event;


import com.nequi.model.event.Event;
import com.nequi.model.event.gateways.EventGateway;
import com.nequi.model.ticketing.gateways.TicketingGateway;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;


@RequiredArgsConstructor
public class EventUseCase {

    private final EventGateway eventGateway;
    private final TicketingGateway ticketingGateway;

    public Mono<String> create(Event event, String requestId) {
        return eventGateway.createEvent(event, requestId)
                .doOnSuccess(eventId -> ticketingGateway.createTickets(eventId, event.getCapacity())
                        .then(Mono.defer(() -> eventGateway.updateEvent(eventId)))
                        .onErrorResume(error -> Mono.empty())
                        .subscribeOn(Schedulers.boundedElastic())
                        .subscribe()
                );
    }

}
