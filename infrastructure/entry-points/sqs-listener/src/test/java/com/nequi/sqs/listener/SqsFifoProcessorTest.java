package com.nequi.sqs.listener;

import com.nequi.model.enums.GeneralMessage;
import com.nequi.model.exception.TechnicalException;
import com.nequi.model.order.Order;
import com.nequi.sqs.listener.dto.ProcessOrderMessage;
import com.nequi.usecase.order.OrderUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.sqs.model.Message;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

class SqsFifoProcessorTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private OrderUseCase orderUseCase;

    private SqsFifoProcessor processor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        processor = new SqsFifoProcessor(objectMapper, orderUseCase);
    }

    @Test
    void shouldProcessMessageSuccessfully() {
        var body = "{\"id\":\"order-1\",\"eventId\":\"event-1\",\"quantity\":\"2\",\"status\":\"RESERVED\"}";
        var message = Message.builder().body(body).build();
        given(objectMapper.readValue(eq(body), eq(ProcessOrderMessage.class)))
                .willReturn(new ProcessOrderMessage("order-1", "event-1", "2", "RESERVED"));
        given(orderUseCase.process(any(Order.class))).willReturn(Mono.just("order-1"));
        StepVerifier.create(processor.apply(message))
                .verifyComplete();
    }

    @Test
    void shouldPropagateErrorWhenDeserializationFails() {
        var message = Message.builder().body("invalid-json").build();
        given(objectMapper.readValue(eq("invalid-json"), eq(ProcessOrderMessage.class)))
                .willThrow(new RuntimeException("deserialization error"));
        StepVerifier.create(processor.apply(message))
                .verifyError(RuntimeException.class);
    }

    @Test
    void shouldPropagateErrorWhenUseCaseFails()  {
        var body = "{\"id\":\"order-1\",\"eventId\":\"event-1\",\"quantity\":\"2\",\"status\":\"RESERVED\"}";
        var message = Message.builder().body(body).build();
        given(objectMapper.readValue(eq(body), eq(ProcessOrderMessage.class)))
                .willReturn(new ProcessOrderMessage("order-1", "event-1", "2", "RESERVED"));
        given(orderUseCase.process(any(Order.class))).willReturn(Mono.error(new TechnicalException(GeneralMessage.INTERNAL_SERVER_ERROR)));
        StepVerifier.create(processor.apply(message))
                .verifyError(TechnicalException.class);
    }
}
