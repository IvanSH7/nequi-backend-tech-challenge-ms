package com.nequi.usecase.order;

import com.nequi.model.enums.GeneralMessage;
import com.nequi.model.enums.OrderStates;
import com.nequi.model.event.Event;
import com.nequi.model.exception.BusinessException;
import com.nequi.model.exception.TechnicalException;
import com.nequi.model.order.Order;
import com.nequi.model.order.gateways.OrderGateway;
import com.nequi.model.order.gateways.ProcessorGateway;
import com.nequi.model.ticketing.gateways.TicketingGateway;
import com.nequi.usecase.event.EventUseCase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static com.nequi.model.enums.EventStates.PUBLISHED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderUseCaseTest {

    @Mock
    private EventUseCase eventUseCase;
    @Mock
    private OrderGateway orderGateway;
    @Mock
    private ProcessorGateway processorGateway;
    @Mock
    private TicketingGateway ticketingGateway;

    private OrderUseCase orderUseCase;
    private Order order;
    private Event publishedEvent;

    @BeforeEach
    void setUp() {
        orderUseCase = new OrderUseCase(eventUseCase, orderGateway, processorGateway, ticketingGateway, 120);
        order = Order.builder().id("order-1").eventId("evt-1").quantity(2).status(OrderStates.PENDING_CONFIRMATION.getName()).build();
        publishedEvent = Event.builder().id("evt-1").status(PUBLISHED.getName()).build();
    }

    @Test
    void shouldCreateOrder() {
        given(eventUseCase.queryEvent(anyString())).willReturn(Mono.just(publishedEvent));
        given(orderGateway.createOrder(any())).willReturn(Mono.just(order));
        given(processorGateway.processOrder(any())).willReturn(Mono.empty());
        StepVerifier.create(orderUseCase.create(order))
                .expectNextMatches(id -> {
                    Assertions.assertNotNull(id);
                    return true;
                }).verifyComplete();
    }

    @Test
    void shouldNotCreateOrderUnpublishedEvent() {
        Event unpublishedEvent = Event.builder().id("evt-1").status("CREATING").build();
        given(eventUseCase.queryEvent(anyString())).willReturn(Mono.just(unpublishedEvent));
        StepVerifier.create(orderUseCase.create(order))
                .expectErrorMatches(ex -> ex instanceof BusinessException &&
                        ((BusinessException) ex).getGeneralMessage() == GeneralMessage.CONFLICT)
                .verify();
    }

    @Test
    void shouldNotCreateOrderUnexistentEvent() {
        given(eventUseCase.queryEvent(anyString())).willReturn(Mono.error(new BusinessException(GeneralMessage.NOT_FOUND)));
        StepVerifier.create(orderUseCase.create(order))
                .expectErrorMatches(ex -> ex instanceof BusinessException &&
                        ((BusinessException) ex).getGeneralMessage() == GeneralMessage.UNPROCESSABLE_CONTENT)
                .verify();
    }

    @Test
    void shouldNotCreateOrderError() {
        given(eventUseCase.queryEvent(anyString())).willReturn(Mono.error(new BusinessException(GeneralMessage.INTERNAL_SERVER_ERROR)));
        StepVerifier.create(orderUseCase.create(order))
                .expectErrorMatches(ex -> ex instanceof TechnicalException &&
                        ((TechnicalException) ex).getGeneralMessage() == GeneralMessage.INTERNAL_SERVER_ERROR)
                .verify();
        verifyNoInteractions(orderGateway);
        verifyNoInteractions(processorGateway);
    }

    @Test
    void shouldProcessOrder() {
        given(ticketingGateway.reserveTickets(anyString(), anyString(), anyInt(), anyInt())).willReturn(Mono.empty());
        given(processorGateway.scheduleOrderRelease(anyString(), anyInt())).willReturn(Mono.empty());
        StepVerifier.create(orderUseCase.process(order))
                .expectNextMatches(id -> {
                    Assertions.assertNotNull(id);
                    return true;
                }).verifyComplete();
    }

    @Test
    void shouldUpdateFailedOrder() {
        given(ticketingGateway.reserveTickets(anyString(), anyString(), anyInt(), anyInt()))
                .willReturn(Mono.error(new BusinessException(GeneralMessage.UNAVAILABLE_TICKETS)));
        given(orderGateway.updateOrder(anyString(), anyString())).willReturn(Mono.empty());
        StepVerifier.create(orderUseCase.process(order))
                .expectNextMatches(id -> {
                    Assertions.assertNotNull(id);
                    return true;
                }).verifyComplete();
        verifyNoInteractions(processorGateway);
    }

    @Test
    void shouldReleaseTickets() {
        Order reservedOrder = order.toBuilder().status(OrderStates.RESERVED.getName()).build();
        given(orderGateway.getOrder(anyString())).willReturn(Mono.just(reservedOrder));
        given(ticketingGateway.releaseTickets(anyString(), anyString())).willReturn(Mono.empty());
        StepVerifier.create(orderUseCase.release("order-1"))
                .verifyComplete();
    }

    @Test
    void shouldNotReleaseTickets() {
        given(orderGateway.getOrder(anyString())).willReturn(Mono.just(order));
        StepVerifier.create(orderUseCase.release("order-1"))
                .verifyComplete();
        verifyNoInteractions(ticketingGateway);
    }

    @Test
    void shouldReturnOrder() {
        given(orderGateway.getOrder(anyString())).willReturn(Mono.just(order));
        StepVerifier.create(orderUseCase.queryOrder("order-1"))
                .expectNext(order)
                .verifyComplete();
    }

    @Test
    void shouldReturnOrderNotFound() {
        given(orderGateway.getOrder(anyString())).willReturn(Mono.empty());
        StepVerifier.create(orderUseCase.queryOrder("order-1"))
                .expectErrorMatches(ex -> ex instanceof BusinessException &&
                        ((BusinessException) ex).getGeneralMessage() == GeneralMessage.NOT_FOUND)
                .verify();
    }

    @Test
    void shouldPayOrder() {
        Order reservedOrder = order.toBuilder().status(OrderStates.RESERVED.getName()).build();
        given(orderGateway.getOrder(anyString())).willReturn(Mono.just(reservedOrder));
        given(ticketingGateway.confirmTickets(anyString(), anyString())).willReturn(Mono.empty());
        StepVerifier.create(orderUseCase.payOrder("order-1"))
                .verifyComplete();
    }

    @Test
    void shouldNotPayUnexistentOrder() {
        given(orderGateway.getOrder(anyString())).willReturn(Mono.empty());
        StepVerifier.create(orderUseCase.payOrder("order-1"))
                .expectErrorMatches(ex -> ex instanceof BusinessException &&
                        ((BusinessException) ex).getGeneralMessage() == GeneralMessage.NOT_FOUND)
                .verify();
    }

    @Test
    void shouldNotPayUnreservedOrder() {
        given(orderGateway.getOrder(anyString())).willReturn(Mono.just(order));
        StepVerifier.create(orderUseCase.payOrder("order-1"))
                .expectErrorMatches(ex -> ex instanceof BusinessException &&
                        ((BusinessException) ex).getGeneralMessage() == GeneralMessage.PRECONDITION_FAILED)
                .verify();
    }

}
