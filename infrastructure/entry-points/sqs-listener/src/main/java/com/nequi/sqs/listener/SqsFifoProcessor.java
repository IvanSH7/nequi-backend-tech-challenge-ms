package com.nequi.sqs.listener;

import com.nequi.sqs.listener.dto.ProcessOrderMessage;
import com.nequi.usecase.order.OrderUseCase;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
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
                .flatMap(processOrderMessage -> Mono.justOrEmpty(orderUseCase.process(MAPPER.toDomain(processOrderMessage))))
                .doOnSubscribe(subscription -> log.info("Message to process", kv("processMessage", message.body())))
                .doOnSuccess(success -> log.info("Successful Order Process", kv("successfulOrderProcess", message.body())))
                .doOnError(error -> log.error("Process Order error", kv("processOrderError", error)))
                .onErrorResume(error -> Mono.empty())
                .then();
    }
}
