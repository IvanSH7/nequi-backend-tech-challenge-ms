package com.nequi.sqs.listener.helper;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.util.function.Function;

@Slf4j
@Builder
public class StandardListener {
    private final SqsAsyncClient client;
    private String queueUrl;
    private Integer numberOfThreads;
    private Integer maxNumberOfMessages;
    private Integer waitTimeSeconds;
    private Integer visibilityTimeoutSeconds;
    private final Function<Message, Mono<Void>> processor;
    private String operation;

    public StandardListener start() {
        this.operation = "MessageFrom:" + queueUrl;
        Scheduler scheduler = Schedulers.newParallel("sqs-pool", numberOfThreads);
        for (var i = 0; i < numberOfThreads; i++) {
            listenRetryRepeat()
                    .subscribeOn(scheduler)
                    .subscribe();
        }
        return this;
    }

    private Flux<Void> listenRetryRepeat() {
        return listen()
                .doOnError(e -> log.error("Error listening standard queue", e))
                .repeat();
    }

    private Flux<Void> listen() {
        return getMessages()
                .flatMap(message -> processor.apply(message)
                        .name("async_operation")
                        .tag("operation", operation)
                        .then(confirm(message)),
                        maxNumberOfMessages)
                .onErrorContinue((e, o) -> log.error("Error listening standard message", e));
    }

    private Mono<Void> confirm(Message message) {
        return Mono.fromCallable(() -> getDeleteMessageRequest(message.receiptHandle()))
                .flatMap(request -> Mono.fromFuture(client.deleteMessage(request)))
                .then();
    }

    private Flux<Message> getMessages() {
        return Mono.fromCallable(this::getReceiveMessageRequest)
                .flatMap(request -> Mono.fromFuture(client.receiveMessage(request)))
                .doOnNext(response -> log.debug("{} received messages from standard sqs", response.messages().size()))
                .flatMapMany(response -> Flux.fromIterable(response.messages()));
    }

    private ReceiveMessageRequest getReceiveMessageRequest() {
        return ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .maxNumberOfMessages(maxNumberOfMessages)
                .waitTimeSeconds(waitTimeSeconds)
                .visibilityTimeout(visibilityTimeoutSeconds)
                .build();
    }

    private DeleteMessageRequest getDeleteMessageRequest(String receiptHandle) {
        return DeleteMessageRequest.builder()
                .queueUrl(queueUrl)
                .receiptHandle(receiptHandle)
                .build();
    }
}
