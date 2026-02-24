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

class StandardListenerTest {

    @Mock
    private SqsAsyncClient asyncClient;

    @Mock
    private Function<Message, Mono<Void>> processor;

    private Message message;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        message = Message.builder().body("message").receiptHandle("receipt-handle").build();
        var messageResponse = ReceiveMessageResponse.builder().messages(message).build();
        var deleteMessageResponse = DeleteMessageResponse.builder().build();

        when(asyncClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(messageResponse));
        when(asyncClient.deleteMessage(any(DeleteMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(deleteMessageResponse));
    }

    private StandardListener buildListener(Function<Message, Mono<Void>> proc) {
        return StandardListener.builder()
                .client(asyncClient)
                .processor(proc)
                .operation("operation")
                .maxNumberOfMessages(1)
                .build();
    }

    @Test
    void shouldProcessAndConfirmMessage() {
        when(processor.apply(any())).thenReturn(Mono.empty());

        Flux<Void> flow = ReflectionTestUtils.invokeMethod(buildListener(processor), "listen");
        StepVerifier.create(flow).verifyComplete();

        verify(processor).apply(message);
        verify(asyncClient).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    void shouldContinueOnProcessorError() {
        when(processor.apply(any())).thenReturn(Mono.error(new RuntimeException("processing failed")));

        Flux<Void> flow = ReflectionTestUtils.invokeMethod(buildListener(processor), "listen");
        StepVerifier.create(flow).verifyComplete();
    }

    @Test
    void shouldHandleEmptyMessageList() {
        var emptyResponse = ReceiveMessageResponse.builder().messages(List.of()).build();
        when(asyncClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(emptyResponse));

        Flux<Void> flow = ReflectionTestUtils.invokeMethod(buildListener(processor), "listen");
        StepVerifier.create(flow).verifyComplete();

        verifyNoInteractions(processor);
    }

    @Test
    void shouldHandleReceiveMessageFailure() {
        when(asyncClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("SQS unavailable")));

        Flux<Void> flow = ReflectionTestUtils.invokeMethod(buildListener(processor), "listen");
        StepVerifier.create(flow).verifyError(RuntimeException.class);
    }

}
