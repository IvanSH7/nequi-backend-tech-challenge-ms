package com.nequi.sqs.listener;

import com.nequi.sqs.listener.dto.ProcessOrderMessage;
import com.nequi.usecase.order.OrderUseCase;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.model.Message;
import tools.jackson.databind.ObjectMapper;

import java.util.function.Function;

import static com.nequi.sqs.listener.mapper.SqsMapper.MAPPER;
import static net.logstash.logback.argument.StructuredArguments.kv;


@Slf4j
@Service
@AllArgsConstructor
public class SqsFifoProcessor implements Function<Message, Mono<Void>> {

    private final ObjectMapper objectMapper;
    private final OrderUseCase orderUseCase;

    @Override
    public Mono<Void> apply(Message message) {
        return Mono.fromCallable(() -> objectMapper.readValue(message.body(), ProcessOrderMessage.class))
                .flatMap(processOrderMessage -> orderUseCase.process(MAPPER.toDomain(processOrderMessage)))
                .doOnSubscribe(subscription -> log.info("SQS Incoming Order to Process", kv("order", message.body())))
                .doOnSuccess(orderId -> log.info("SQS Successful Processed Order", kv("orderId", orderId)))
                .doOnError(error -> log.error("SQS Error Trying to Process Order", kv("processOrderError", error.getMessage())))
                .then();
    }
}
