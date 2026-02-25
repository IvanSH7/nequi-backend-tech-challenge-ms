package com.nequi.sqs.listener.helper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FifoListenerTest {

    @Mock
    private SqsAsyncClient asyncClient;

    @Mock
    private Function<Message, Mono<Void>> processor;

    private Message message;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        message = Message.builder().body("message").receiptHandle("receipt-handle").build();
        var deleteMessageResponse = DeleteMessageResponse.builder().build();
        var messageResponse = ReceiveMessageResponse.builder().messages(message).build();

        when(asyncClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(messageResponse));
        when(asyncClient.deleteMessage(any(DeleteMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(deleteMessageResponse));
    }

    @Test
    void listenerTest() {
        var sqsListener = buildListener(processor);

        Flux<Void> flow = ReflectionTestUtils.invokeMethod(sqsListener, "listen");
        StepVerifier.create(flow).verifyComplete();
    }

    @Test
    void shouldProcessAndConfirmMessage() {
        when(processor.apply(any())).thenReturn(Mono.empty());

        var sqsListener = buildListener(processor);

        Flux<Void> flow = ReflectionTestUtils.invokeMethod(sqsListener, "listen");
        StepVerifier.create(flow).verifyComplete();

        verify(processor).apply(message);
        verify(asyncClient).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    void shouldContinueOnProcessorError() {
        when(processor.apply(any())).thenReturn(Mono.error(new RuntimeException("processing failed")));

        var sqsListener = buildListener(processor);

        Flux<Void> flow = ReflectionTestUtils.invokeMethod(sqsListener, "listen");
        StepVerifier.create(flow).verifyComplete();
    }

    @Test
    void shouldHandleEmptyMessageList() {
        var emptyResponse = ReceiveMessageResponse.builder().messages(List.of()).build();
        when(asyncClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(emptyResponse));

        var sqsListener = buildListener(processor);

        Flux<Void> flow = ReflectionTestUtils.invokeMethod(sqsListener, "listen");
        StepVerifier.create(flow).verifyComplete();

        verifyNoInteractions(processor);
    }

    @Test
    void shouldHandleReceiveMessageFailure() {
        when(asyncClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("SQS unavailable")));

        var sqsListener = buildListener(processor);

        Flux<Void> flow = ReflectionTestUtils.invokeMethod(sqsListener, "listen");
        StepVerifier.create(flow).verifyError(RuntimeException.class);
    }

    private FifoListener buildListener(Function<Message, Mono<Void>> proc) {
        return FifoListener.builder()
                .client(asyncClient)
                .processor(proc)
                .operation("operation")
                .maxNumberOfMessages(1)
                .build();
    }

}
