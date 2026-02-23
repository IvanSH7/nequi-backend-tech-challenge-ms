package com.nequi.sqs.sender;

import com.nequi.model.enums.GeneralMessage;
import com.nequi.model.exception.TechnicalException;
import com.nequi.model.order.Order;
import com.nequi.model.order.gateways.ProcessorGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import tools.jackson.databind.ObjectMapper;


import static net.logstash.logback.argument.StructuredArguments.kv;

@Slf4j
@RequiredArgsConstructor
public class SQSSender implements ProcessorGateway {

    private final SqsAsyncClient client;
    private final String purchaseQueueUrl;
    private final String releaseQueueUrl;
    private final String releaseDelay;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> processOrder(Order order) {
        return Mono.fromCallable(() -> buildFifoRequest(order))
                .flatMap(sendMessageRequest -> {
                    log.info("SQS Send Process Order Message", kv("processOrderMessage", sendMessageRequest.toString()));
                    return Mono.fromFuture(client.sendMessage(sendMessageRequest));
                })
                .doOnNext(response -> log.info("SQS Send Process Order Response", kv("processOrderResponse", response.messageId())))
                .doOnError(error -> log.info("SQS Send Process Order Error", kv("processOrderError", error.getMessage())))
                .onErrorMap(error -> new TechnicalException(error, GeneralMessage.INTERNAL_SERVER_ERROR))
                .then();
    }

    @Override
    public Mono<Void> scheduleOrderRelease(String orderId, Integer delay) {
        return Mono.fromCallable(() -> buildStandardRequest(orderId, delay))
                .flatMap(sendMessageRequest -> Mono.fromFuture(client.sendMessage(sendMessageRequest))
                        .doOnSubscribe(sub -> log.info("SQS Send Schedule Order Release Message", kv("scheduleOrderReleaseMessage", sendMessageRequest.toString()))))
                .doOnNext(response -> log.info("SQS Send Schedule Order Release Response", kv("scheduleOrderReleaseResponse", response.messageId())))
                .doOnError(error -> log.info("SQS Send Schedule Order Release Error", kv("scheduleOrderReleaseError", error.getMessage())))
                .onErrorMap(error -> new TechnicalException(error, GeneralMessage.INTERNAL_SERVER_ERROR))
                .then();
    }

    private SendMessageRequest buildFifoRequest(Order order) {
        return SendMessageRequest.builder()
                .queueUrl(purchaseQueueUrl)
                .messageBody(objectMapper.writeValueAsString(order))
                .messageGroupId(order.getEventId())
                .messageDeduplicationId(order.getId())
                .build();
    }


    private SendMessageRequest buildStandardRequest(String orderId, Integer delay) {
        return SendMessageRequest.builder()
                .delaySeconds(delay)
                .queueUrl(releaseQueueUrl)
                .messageBody(orderId)
                .build();
    }

}
