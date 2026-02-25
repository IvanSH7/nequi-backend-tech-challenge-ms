package com.nequi.sqs.sender;

import com.nequi.model.exception.TechnicalException;
import com.nequi.model.order.Order;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import tools.jackson.databind.ObjectMapper;

import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class SQSSenderTest {

    @Mock
    private SqsAsyncClient sqsAsyncClient;
    @Mock
    private ObjectMapper objectMapper;

    private SQSSender sqsSender;

    private static final String PURCHASE_QUEUE_URL = "https://sqs.us-east-1.amazonaws.com/123456789/purchase-queue";
    private static final String RELEASE_QUEUE_URL = "https://sqs.us-east-1.amazonaws.com/123456789/release-queue";

    @BeforeEach
    void setUp() {
        sqsSender = new SQSSender(sqsAsyncClient, PURCHASE_QUEUE_URL, RELEASE_QUEUE_URL, objectMapper);
    }

    @Test
    void shouldSendProcessOrderMessageSuccessfully() {
        var order = Order.builder().id("order-1").eventId("event-1").quantity(2).build();
        given(objectMapper.writeValueAsString(order)).willReturn("{\"id\":\"order-1\"}");
        given(sqsAsyncClient.sendMessage(any(SendMessageRequest.class)))
                .willReturn(CompletableFuture.completedFuture(SendMessageResponse.builder().messageId("msg-1").build()));
        StepVerifier.create(sqsSender.processOrder(order))
                .verifyComplete();
    }

    @Test
    void shouldMapErrorWhenSendMessageFails() {
        var order = Order.builder().id("order-1").eventId("event-1").quantity(2).build();
        given(objectMapper.writeValueAsString(order)).willReturn("{\"id\":\"order-1\"}");
        given(sqsAsyncClient.sendMessage(any(SendMessageRequest.class)))
                .willReturn(CompletableFuture.failedFuture(new RuntimeException("SQS unavailable")));
        StepVerifier.create(sqsSender.processOrder(order))
                .verifyError(TechnicalException.class);
    }

    @Test
    void shouldSendScheduleOrderReleaseMessageSuccessfully() {
        given(sqsAsyncClient.sendMessage(any(SendMessageRequest.class)))
                .willReturn(CompletableFuture.completedFuture(SendMessageResponse.builder().messageId("msg-2").build()));
        StepVerifier.create(sqsSender.scheduleOrderRelease("order-1", 120))
                .verifyComplete();
    }

    @Test
    void shouldMapErrorWhenSendScheduleOrderReleaseMessageFails() {
        given(sqsAsyncClient.sendMessage(any(SendMessageRequest.class)))
                .willReturn(CompletableFuture.failedFuture(new RuntimeException("SQS unavailable")));
        StepVerifier.create(sqsSender.scheduleOrderRelease("order-1", 120))
                .verifyError(TechnicalException.class);
    }

}
