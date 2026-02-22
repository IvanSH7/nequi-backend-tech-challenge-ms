package com.nequi.sqs.listener;

import com.nequi.sqs.listener.dto.ProcessOrderMessage;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.model.Message;
import tools.jackson.databind.ObjectMapper;

import java.util.function.Function;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Slf4j
@Service
@AllArgsConstructor
public class SqsStandardProcessor implements Function<Message, Mono<Void>> {

    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> apply(Message message) {
        return Mono.fromCallable(() -> objectMapper.readValue(message.body(), ProcessOrderMessage.class))
                .doOnSubscribe(subscription -> log.info("Message to reverse", kv("reverseMessage", message.body())))
                .doOnSuccess(success -> log.info("Successful reverse", kv("successfulReverse", message.body())))
                .doOnError(error -> log.error("Reverse error", kv("reverseError", error)))
                .onErrorResume(error -> Mono.empty())
                .then();
    }
}
