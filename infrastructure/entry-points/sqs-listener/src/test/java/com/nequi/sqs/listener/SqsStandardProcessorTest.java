package com.nequi.sqs.listener;

import com.nequi.model.enums.GeneralMessage;
import com.nequi.model.exception.TechnicalException;
import com.nequi.usecase.order.OrderUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.sqs.model.Message;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

class SqsStandardProcessorTest {

    @Mock
    private OrderUseCase orderUseCase;

    private SqsStandardProcessor processor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        processor = new SqsStandardProcessor(orderUseCase);
    }

    @Test
    void shouldReleaseOrderSuccessfully() {
        var message = Message.builder().body("order-1").build();
        given(orderUseCase.release(any(String.class))).willReturn(Mono.empty());
        StepVerifier.create(processor.apply(message))
                .verifyComplete();
    }

    @Test
    void shouldPropagateErrorWhenReleaseFails() {
        var message = Message.builder().body("order-1").build();
        given(orderUseCase.release(any(String.class)))
                .willReturn(Mono.error(new TechnicalException(GeneralMessage.INTERNAL_SERVER_ERROR)));
        StepVerifier.create(processor.apply(message))
                .verifyError(TechnicalException.class);
    }

}
