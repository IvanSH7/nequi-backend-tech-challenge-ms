package com.nequi.usecase.order;

import com.nequi.model.enums.GeneralMessage;
import com.nequi.model.enums.OrderStates;
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

import static com.nequi.model.enums.EventStates.PUBLISHED;


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
                .filter(event -> event.getStatus().equals(PUBLISHED.getName()))
                .switchIfEmpty(Mono.defer(() -> Mono.error(new BusinessException(GeneralMessage.CONFLICT))))
                .flatMap(supplier -> orderGateway.createOrder(order)
                        .doOnSuccess(createdOrder -> processorGateway.processOrder(createdOrder)
                                .onErrorResume(error -> Mono.empty())
                                .subscribeOn(Schedulers.boundedElastic())
                                .subscribe()))
                .map(Order::getId);
    }

    public Mono<String> process(Order order) {
        return ticketingGateway.reserveTickets(order.getEventId(), order.getId(), order.getQuantity(), 120)
                .then(Mono.defer(() -> processorGateway.scheduleOrderRelease(order.getId(), 120)))
                .onErrorResume(BusinessException.class, e -> orderGateway.updateOrder(order.getId(), OrderStates.FAILED.getName()))
                .thenReturn(order.getId());
    }

    public Mono<Void> release(String orderId) {
        return orderGateway.getOrder(orderId)
                .filter(order -> OrderStates.RESERVED.getName().equals(order.getStatus()))
                .flatMap(order -> ticketingGateway.releaseTickets(order.getEventId(), order.getId()));
    }

    public Mono<Order> queryOrder(String orderId) {
        return orderGateway.getOrder(orderId)
                .switchIfEmpty(Mono.defer(() -> Mono.error(new BusinessException(GeneralMessage.NOT_FOUND))));
    }

    public Mono<Void> payOrder(String orderId) {
        return orderGateway.getOrder(orderId)
                .switchIfEmpty(Mono.defer(() -> Mono.error(new BusinessException(GeneralMessage.NOT_FOUND))))
                .filter(order -> OrderStates.RESERVED.getName().equals(order.getStatus()))
                .switchIfEmpty(Mono.defer(() -> Mono.error(new BusinessException(GeneralMessage.PRECONDITION_FAILED))))
                .flatMap(order -> ticketingGateway.confirmTickets(order.getEventId(), order.getId()))
                .then();
    }

}
