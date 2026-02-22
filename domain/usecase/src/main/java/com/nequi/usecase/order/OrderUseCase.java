package com.nequi.usecase.order;

import com.nequi.model.enums.GeneralMessage;
import com.nequi.model.exception.BusinessException;
import com.nequi.model.exception.TechnicalException;
import com.nequi.model.order.Order;
import com.nequi.model.order.gateways.OrderGateway;
import com.nequi.model.order.gateways.ProcessorGateway;
import com.nequi.model.ticketing.gateways.TicketingGateway;
import com.nequi.usecase.event.EventUseCase;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;


@RequiredArgsConstructor
public class OrderUseCase {

    private final EventUseCase eventUseCase;
    private final OrderGateway orderGateway;
    private final ProcessorGateway processorGateway;
    private final TicketingGateway ticketingGateway;

    public Mono<String> create(Order order) {
        return eventUseCase.queryEvent(order.getEventId())
                .onErrorResume(BusinessException.class, exception -> Mono.error(exception.getGeneralMessage().equals(GeneralMessage.NOT_FOUND) ?
                        new BusinessException(GeneralMessage.UNPROCESSABLE_CONTENT) : new TechnicalException(GeneralMessage.INTERNAL_SERVER_ERROR)))
                .filter(event -> event.getStatus().equals("PUBLISHED"))
                .switchIfEmpty(Mono.defer(() -> Mono.error(new BusinessException(GeneralMessage.CONFLICT))))
                .flatMap(supplier -> orderGateway.createOrder(order)
                        .doOnSuccess(createdOrder -> processorGateway.processOrder(createdOrder)
                                .onErrorResume(error -> Mono.empty())
                                .subscribeOn(Schedulers.boundedElastic())
                                .subscribe()))
                .map(Order::getId);
    }

    public Mono<Void> process(Order order) {
        return ticketingGateway.reserveTickets(order.getEventId(), order.getId(), order.getQuantity());
    }

}
