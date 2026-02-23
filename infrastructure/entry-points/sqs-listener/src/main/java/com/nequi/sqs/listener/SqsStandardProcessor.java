package com.nequi.sqs.listener;

import com.nequi.usecase.order.OrderUseCase;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.model.Message;

import java.util.function.Function;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Slf4j
@Service
@AllArgsConstructor
public class SqsStandardProcessor implements Function<Message, Mono<Void>> {

    private final OrderUseCase orderUseCase;

    @Override
    public Mono<Void> apply(Message message) {
        return Mono.fromCallable(message::body)
                .doOnSubscribe(subscription -> log.info("SQS Incoming Release Order", kv("orderId", message.body())))
                .flatMap(orderUseCase::release)
                .doOnSuccess(success -> log.info("SQS Successful Released Order", kv("orderId", message.body())))
                .doOnError(error -> log.error("SQS Error Trying to Release Order", kv("releaseOrderError", error.getMessage())))
                .then();
    }
}
