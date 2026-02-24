package com.nequi.usecase.event;

import com.nequi.model.enums.GeneralMessage;
import com.nequi.model.event.Event;
import com.nequi.model.event.gateways.EventGateway;
import com.nequi.model.exception.BusinessException;
import com.nequi.model.ticketing.gateways.TicketingGateway;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static com.nequi.model.enums.EventStates.FAILED;
import static com.nequi.model.enums.EventStates.PUBLISHED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventUseCaseTest {

    @Mock
    private EventGateway eventGateway;
    @Mock
    private TicketingGateway ticketingGateway;

    private EventUseCase eventUseCase;

    private Event event;

    @BeforeEach
    void setUp() {
        eventUseCase = new EventUseCase(eventGateway, ticketingGateway);
        event = Event.builder()
                .name("Concert")
                .place("Stadium")
                .date("01/12/2027")
                .capacity("100")
                .build();
    }

    @Test
    void shouldCreateEvent() {
        given(eventGateway.createEvent(any(Event.class), anyString())).willReturn(Mono.empty());
        given(ticketingGateway.createTickets(anyString(), anyString())).willReturn(Mono.empty());
        given(eventGateway.updateEvent(anyString(), anyString())).willReturn(Mono.empty());
        StepVerifier.create(eventUseCase.create(event))
                .expectNextMatches(id -> {
                    Assertions.assertNotNull(id);
                    return true;
                })
                .verifyComplete();
        verify(eventGateway).createEvent(any(Event.class), anyString());
        verify(ticketingGateway).createTickets(anyString(), anyString());
        verify(eventGateway).updateEvent(anyString(), eq(PUBLISHED.getName()));
        verify(eventGateway, times(1)).updateEvent(anyString(), anyString());
    }

    @Test
    void shouldCreateEventFailedState() {
        given(eventGateway.createEvent(any(Event.class), anyString())).willReturn(Mono.empty());
        given(ticketingGateway.createTickets(anyString(), anyString())).willReturn(Mono.error(new RuntimeException("ticketing error")));
        StepVerifier.create(eventUseCase.create(event))
                .expectNextMatches(id -> {
                    Assertions.assertNotNull(id);
                    return true;
                })
                .verifyComplete();
        verify(eventGateway).createEvent(any(Event.class), anyString());
        verify(ticketingGateway).createTickets(anyString(), anyString());
        verify(eventGateway, timeout(2000)).updateEvent(anyString(), eq(FAILED.getName()));
        verify(eventGateway, times(1)).updateEvent(anyString(), anyString());
    }

    @Test
    void shouldReturnEvents() {
        List<Event> events = List.of(event);
        given(eventGateway.queryEvents()).willReturn(Mono.just(events));
        StepVerifier.create(eventUseCase.queryEvents())
                .expectNext(events)
                .verifyComplete();
    }

    @Test
    void shouldReturnEvent() {
        given(eventGateway.getEvent("evt-1")).willReturn(Mono.just(event));
        StepVerifier.create(eventUseCase.queryEvent("evt-1"))
                .expectNext(event)
                .verifyComplete();
    }

    @Test
    void shouldReturnEventNotFound() {
        given(eventGateway.getEvent("evt-1")).willReturn(Mono.empty());
        StepVerifier.create(eventUseCase.queryEvent("evt-1"))
                .expectErrorMatches(throwable -> throwable instanceof BusinessException &&
                        ((BusinessException) throwable).getGeneralMessage() == GeneralMessage.NOT_FOUND)
                .verify();
    }
}
