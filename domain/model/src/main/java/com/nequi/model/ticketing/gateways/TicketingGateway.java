package com.nequi.model.ticketing.gateways;

import reactor.core.publisher.Mono;

public interface TicketingGateway {

    Mono<Void> createTickets(String eventId, String requiredTickets);

}
