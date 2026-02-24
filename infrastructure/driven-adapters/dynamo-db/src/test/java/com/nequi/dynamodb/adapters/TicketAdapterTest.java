package com.nequi.dynamodb.adapters;

import com.nequi.dynamodb.dto.TicketDto;
import com.nequi.dynamodb.repositories.TicketDynamoRepository;
import com.nequi.model.enums.GeneralMessage;
import com.nequi.model.exception.BusinessException;
import com.nequi.model.exception.TechnicalException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;

import java.util.List;

import static com.nequi.dynamodb.util.Constants.*;
import static com.nequi.model.enums.TicketStates.AVAILABLE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class TicketAdapterTest {

    @Mock
    private TicketDynamoRepository ticketDynamoRepository;

    @InjectMocks
    private TicketAdapter ticketAdapter;

    private TicketDto ticketDto;
    private static final String EVENT_ID = "evt-123";
    private static final String ORDER_ID = "ord-456";

    @BeforeEach
    void setUp() {
        ticketDto = TicketDto.builder()
                .pk(BASE_EVENT_PK + EVENT_ID)
                .sk(BASE_TICKET_PK + "1")
                .status(AVAILABLE.getName())
                .build();
    }

    @Test
    void createTicketsSuccess() {
        given(ticketDynamoRepository.save(any(TicketDto.class))).willReturn(Mono.just(ticketDto));

        StepVerifier.create(ticketAdapter.createTickets(EVENT_ID, "2"))
                .verifyComplete();
    }

    @Test
    void createTicketsTechnicalError() {
        given(ticketDynamoRepository.save(any(TicketDto.class))).willReturn(Mono.error(new RuntimeException("Dynamo Error")));

        StepVerifier.create(ticketAdapter.createTickets(EVENT_ID, "2"))
                .expectError(TechnicalException.class)
                .verify();
    }

    @Test
    void reserveTicketsBusinessErrorNotEnoughTickets() {
        given(ticketDynamoRepository.query(any(QueryEnhancedRequest.class)))
                .willReturn(Mono.just(List.of(ticketDto)));
        StepVerifier.create(ticketAdapter.reserveTickets(EVENT_ID, ORDER_ID, 2, 300))
                .expectErrorMatches(throwable -> throwable instanceof BusinessException &&
                        ((BusinessException) throwable).getGeneralMessage() == GeneralMessage.UNAVAILABLE_TICKETS)
                .verify();
    }

}