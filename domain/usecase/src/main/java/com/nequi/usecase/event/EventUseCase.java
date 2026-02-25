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
import java.util.UUID;

import static com.nequi.model.enums.EventStates.FAILED;
import static com.nequi.model.enums.EventStates.PUBLISHED;

@RequiredArgsConstructor
public class EventUseCase {

    private final EventGateway eventGateway;
    private final TicketingGateway ticketingGateway;

    public Mono<String> create(Event event) {
        return Mono.just(UUID.randomUUID().toString())
                .flatMap(eventId -> eventGateway.createEvent(event, eventId)
                        .doOnSuccess(unUsed -> ticketingGateway.createTickets(eventId, event.getCapacity())
                                .then(Mono.defer(() -> eventGateway.updateEvent(eventId, PUBLISHED.getName())))
                                .onErrorResume(e -> eventGateway.updateEvent(eventId, FAILED.getName()))
                                .subscribeOn(Schedulers.boundedElastic())
                                .subscribe()
                        ).thenReturn(eventId));
    }

    public Mono<List<Event>> queryEvents() {
        return eventGateway.queryEvents();
    }

    public Mono<Event> queryEvent(String eventId) {
        return eventGateway.getEvent(eventId)
                .switchIfEmpty(Mono.defer(() -> Mono.error(new BusinessException(GeneralMessage.NOT_FOUND))));
    }

}
