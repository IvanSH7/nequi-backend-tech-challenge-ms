package com.nequi.dynamodb.adapters;

import com.nequi.dynamodb.dto.EventDto;
import com.nequi.dynamodb.repositories.EventDynamoRepository;
import com.nequi.model.enums.GeneralMessage;
import com.nequi.model.event.Event;
import com.nequi.model.exception.TechnicalException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static com.nequi.dynamodb.util.Constants.*;
import static com.nequi.model.enums.EventStates.PUBLISHED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class EventAdapterTest {

    @Mock
    private EventDynamoRepository eventDynamoRepository;
    @InjectMocks
    private EventAdapter eventAdapter;
    private Event event;
    private EventDto eventDto;
    private static final String EVENT_ID = "abc123";

    @BeforeEach
    void setUp() {
        event = Event.builder()
                .id(EVENT_ID)
                .name("Concert")
                .place("Bogotá")
                .date("02/02/2026")
                .capacity("100")
                .availability("100")
                .status("CREATING")
                .build();

        eventDto = EventDto.builder()
                .pk(BASE_EVENT_PK + EVENT_ID)
                .sk(SORT_KEY_METADATA)
                .type("Event")
                .status("CREATING")
                .name("Concert")
                .place("Bogotá")
                .date("02/02/2026")
                .build();
    }

    @Test
    void createEventSuccess() {
        given(eventDynamoRepository.save(any(EventDto.class))).willReturn(Mono.just(eventDto));
        StepVerifier.create(eventAdapter.createEvent(event, EVENT_ID))
                .verifyComplete();
    }

    @Test
    void createEventError() {
        given(eventDynamoRepository.save(any(EventDto.class))).willReturn(Mono.error(new RuntimeException("DynamoDB error")));
        StepVerifier.create(eventAdapter.createEvent(event, EVENT_ID))
                .expectError(TechnicalException.class)
                .verify();
    }

    @Test
    void updateEventSuccess() {
        given(eventDynamoRepository.getById(eq(BASE_EVENT_PK + EVENT_ID), eq(SORT_KEY_METADATA)))
                .willReturn(Mono.just(eventDto));
        given(eventDynamoRepository.update(any(EventDto.class))).willReturn(Mono.just(eventDto));
        StepVerifier.create(eventAdapter.updateEvent(EVENT_ID, PUBLISHED.getName()))
                .verifyComplete();
    }

    @Test
    void updateEventNotFound() {
        given(eventDynamoRepository.getById(eq(BASE_EVENT_PK + EVENT_ID), eq(SORT_KEY_METADATA)))
                .willReturn(Mono.empty());
        StepVerifier.create(eventAdapter.updateEvent(EVENT_ID, PUBLISHED.getName()))
                .expectErrorMatches(throwable -> throwable instanceof TechnicalException &&
                        ((TechnicalException) throwable).getGeneralMessage() == GeneralMessage.EVENT_NOT_FOUND)
                .verify();
    }

    @Test
    void updateEventError() {
        given(eventDynamoRepository.getById(eq(BASE_EVENT_PK + EVENT_ID), eq(SORT_KEY_METADATA)))
                .willReturn(Mono.just(eventDto));
        given(eventDynamoRepository.update(any(EventDto.class))).willReturn(Mono.error(new RuntimeException("DynamoDB error")));
        StepVerifier.create(eventAdapter.updateEvent(EVENT_ID, PUBLISHED.getName()))
                .expectError(TechnicalException.class)
                .verify();
    }

    @Test
    void queryEventsSuccess() {
        given(eventDynamoRepository.queryByIndex(any(), eq(TYPE_INDEX)))
                .willReturn(Mono.just(List.of(eventDto)));
        StepVerifier.create(eventAdapter.queryEvents())
                .expectNextMatches(events -> events.size() == 1 && "Concert".equals(events.get(0).getName()))
                .verifyComplete();
    }

    @Test
    void queryEventsError() {
        given(eventDynamoRepository.queryByIndex(any(), eq(TYPE_INDEX)))
                .willReturn(Mono.error(new RuntimeException("DynamoDB error")));
        StepVerifier.create(eventAdapter.queryEvents())
                .expectError(TechnicalException.class)
                .verify();
    }

    @Test
    void getEventSuccess() {
        given(eventDynamoRepository.getById(eq(BASE_EVENT_PK + EVENT_ID), eq(SORT_KEY_METADATA)))
                .willReturn(Mono.just(eventDto));
        StepVerifier.create(eventAdapter.getEvent(EVENT_ID))
                .expectNextMatches(e -> "Concert".equals(e.getName()))
                .verifyComplete();
    }

    @Test
    void getEventTechnicalError() {
        given(eventDynamoRepository.getById(eq(BASE_EVENT_PK + EVENT_ID), eq(SORT_KEY_METADATA)))
                .willReturn(Mono.error(new RuntimeException("DynamoDB error")));
        StepVerifier.create(eventAdapter.getEvent(EVENT_ID))
                .expectError(TechnicalException.class)
                .verify();
    }

}
