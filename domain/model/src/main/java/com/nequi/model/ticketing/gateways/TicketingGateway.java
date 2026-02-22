package com.nequi.model.ticketing.gateways;

import reactor.core.publisher.Mono;

public interface TicketingGateway {

    Mono<Void> createTickets(String eventId, String requiredTickets);
    Mono<Void> reserveTickets(String eventId, String orderId, Integer orderTickets);


}
