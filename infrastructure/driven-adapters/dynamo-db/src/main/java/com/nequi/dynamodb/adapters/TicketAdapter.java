package com.nequi.dynamodb.adapters;

import com.nequi.dynamodb.repositories.TicketDynamoRepository;
import com.nequi.model.enums.GeneralMessage;
import com.nequi.model.exception.TechnicalException;
import com.nequi.model.ticketing.gateways.TicketingGateway;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


import static com.nequi.dynamodb.mapper.DynamoMapper.MAPPER;
import static net.logstash.logback.argument.StructuredArguments.kv;

@Component
@AllArgsConstructor
@Slf4j
public class TicketAdapter implements TicketingGateway {

    private final TicketDynamoRepository ticketDynamoRepository;

    @Override
    public Mono<Void> createTickets(String eventId, String requiredTickets) {
        return Flux.range(1, Integer.parseInt(requiredTickets))
                .map(index -> MAPPER.toTicketDto(eventId, index))
                .buffer(25)
                .flatMap(ticketDynamoRepository::processBatch)
                .doOnSubscribe(sub -> log.info("Dynamo Batch save Request", kv("eventId", eventId), kv("totalTickets", requiredTickets)))
                .then().doOnSuccess(savedEvent-> log.info("Dynamo Batch save Response", kv("eventId", eventId)))
                .doOnError(error -> log.info("Dynamo Batch save Error", kv("error", error.getMessage())))
                .onErrorMap(error -> new TechnicalException(error, GeneralMessage.INTERNAL_SERVER_ERROR));
    }

    @Override
    public Mono<Void> reserveTickets(String eventId, String orderId, Integer orderTickets) {
        return null;
    }
}
