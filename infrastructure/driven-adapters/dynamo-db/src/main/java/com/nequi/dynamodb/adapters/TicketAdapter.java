package com.nequi.dynamodb.adapters;

import com.nequi.dynamodb.dto.EventDto;
import com.nequi.dynamodb.dto.OrderDto;
import com.nequi.dynamodb.dto.TicketDto;
import com.nequi.dynamodb.repositories.EventDynamoRepository;
import com.nequi.dynamodb.repositories.OrderDynamoRepository;
import com.nequi.dynamodb.repositories.TicketDynamoRepository;
import com.nequi.model.enums.GeneralMessage;
import com.nequi.model.exception.BusinessException;
import com.nequi.model.exception.TechnicalException;
import com.nequi.model.ticketing.gateways.TicketingGateway;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.*;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.Instant;
import java.util.List;

import static com.nequi.dynamodb.mapper.DynamoMapper.MAPPER;
import static net.logstash.logback.argument.StructuredArguments.kv;

@Component
@AllArgsConstructor
@Slf4j
public class TicketAdapter implements TicketingGateway {

    private final TicketDynamoRepository ticketDynamoRepository;
    private final EventDynamoRepository eventDynamoRepository;
    private final OrderDynamoRepository orderDynamoRepository;

    @Override
    public Mono<Void> createTickets(String eventId, String requiredTickets) {
        return Flux.range(1, Integer.parseInt(requiredTickets))
                .map(index -> MAPPER.toTicketDto(eventId, index))
                .flatMap(ticketDynamoRepository::save, 25)
                .doOnSubscribe(sub -> log.info("Dynamo Concurrent save Request", kv("eventId", eventId), kv("totalTickets", requiredTickets)))
                .then().doOnSuccess(supplier-> log.info("Dynamo Concurrent save Response", kv("eventId", eventId)))
                .doOnError(error -> log.info("Dynamo Concurrent Save Error", kv("error", error.getMessage())))
                .onErrorMap(error -> new TechnicalException(error, GeneralMessage.INTERNAL_SERVER_ERROR));
    }

    @Override
    public Mono<Void> reserveTickets(String eventId, String orderId, Integer orderTickets, Integer ttl) {
        QueryConditional queryConditional = QueryConditional.sortBeginsWith(
                Key.builder().partitionValue("EVENT#" + eventId).sortValue("TICKET#").build());
        Expression filterExpression = Expression.builder()
                .expression("#status = :status")
                .putExpressionName("#status", "status")
                .putExpressionValue(":status", AttributeValue.builder().s("AVAILABLE").build())
                .build();
        QueryEnhancedRequest queryRequest = QueryEnhancedRequest.builder()
                .queryConditional(queryConditional)
                .filterExpression(filterExpression)
                .build();
        return ticketDynamoRepository.query(queryRequest)
                .flatMap(tickets -> {
                    if (tickets.size() < orderTickets) {
                        return Mono.error(new BusinessException(
                                GeneralMessage.UNAVAILABLE_TICKETS.getExternalMessage(), GeneralMessage.UNAVAILABLE_TICKETS));
                    }
                    var ticketsToReserve = tickets.subList(0, orderTickets);
                    return getEventMetadata(eventId)
                            .flatMap(eventDto -> executeEnhancedTransaction(eventDto, orderId, orderTickets, ticketsToReserve, ttl));
                });
    }

    @Override
    public Mono<Void> releaseTickets(String eventId, String orderId) {
        QueryConditional queryConditional = QueryConditional.sortBeginsWith(
                Key.builder().partitionValue("EVENT#" + eventId).sortValue("TICKET#").build());
        Expression filterExpression = Expression.builder()
                .expression("orderId = :orderId")
                .putExpressionValue(":orderId", AttributeValue.builder().s(orderId).build())
                .build();
        QueryEnhancedRequest queryRequest = QueryEnhancedRequest.builder()
                .queryConditional(queryConditional)
                .filterExpression(filterExpression)
                .build();
        return ticketDynamoRepository.query(queryRequest)
                .flatMap(tickets -> getEventMetadata(eventId)
                        .flatMap(event -> executeReleaseTransaction(event, orderId, tickets)));
    }

    private Mono<EventDto> getEventMetadata(String eventId) {
        return eventDynamoRepository.getById("EVENT#".concat(eventId), "METADATA");
    }

    private Mono<Void> executeEnhancedTransaction(EventDto eventDto, String orderId, Integer quantity, List<TicketDto> tickets, Integer ttl) {
        eventDto.setAvailableCount(eventDto.getAvailableCount() - quantity);
        OrderDto updatedOrderDto = MAPPER.toUpdateOrderDto(orderId, "RESERVED", Instant.now().plusSeconds(ttl).getEpochSecond());

        TransactWriteItemsEnhancedRequest.Builder txBuilder = TransactWriteItemsEnhancedRequest.builder();
        Expression eventCondition = Expression.builder()
                .expression("availableCount >= :qty AND #status = :published")
                .putExpressionName("#status", "status")
                .putExpressionValue(":qty", AttributeValue.builder().n(String.valueOf(quantity)).build())
                .putExpressionValue(":published", AttributeValue.builder().s("PUBLISHED").build())
                .build();
        txBuilder.addUpdateItem(eventDynamoRepository.getTable(), eventDynamoRepository.buildUpdateTransaction(eventDto, eventCondition));
        txBuilder.addUpdateItem(orderDynamoRepository.getTable(), orderDynamoRepository.buildUpdateTransaction(updatedOrderDto, null));
        Expression ticketCondition = Expression.builder()
                .expression("#status = :avail")
                .putExpressionName("#status", "status")
                .putExpressionValue(":avail", AttributeValue.builder().s("AVAILABLE").build())
                .build();
        for (TicketDto ticketDto : tickets) {
            ticketDto.setStatus("RESERVED");
            ticketDto.setOrderId(orderId);
            txBuilder.addUpdateItem(ticketDynamoRepository.getTable(), ticketDynamoRepository.buildUpdateTransaction(ticketDto, ticketCondition));
        }
        return ticketDynamoRepository.executeTransaction(txBuilder.build())
                .doOnSubscribe(sub -> log.info("Dynamo Reserve Write",
                        kv("orderId", orderId), kv("quantity", quantity), kv("reserveWrite", txBuilder.toString())))
                .doOnSuccess(supplier-> log.info("Dynamo Reserve Write Response", kv("orderId", orderId)))
                .doOnError(error -> log.info("Dynamo Reserve Write Error", kv("orderId", orderId), kv("error", error.getMessage())))
                .onErrorMap(e -> new TechnicalException(e, GeneralMessage.INTERNAL_SERVER_ERROR));
    }

    private Mono<Void> executeReleaseTransaction(EventDto eventDto, String orderId, List<TicketDto> tickets) {
        eventDto.setAvailableCount(eventDto.getAvailableCount() + tickets.size());
        OrderDto orderUpdateDto = new OrderDto();
        orderUpdateDto.setPk("ORDER#" + orderId);
        orderUpdateDto.setSk("METADATA");
        orderUpdateDto.setStatus("EXPIRED");
        TransactWriteItemsEnhancedRequest.Builder txBuilder = TransactWriteItemsEnhancedRequest.builder();
        Expression eventCondition = Expression.builder()
                .expression("#status = :published")
                .putExpressionName("#status", "status")
                .putExpressionValue(":published", AttributeValue.builder().s("PUBLISHED").build())
                .build();
        txBuilder.addUpdateItem(eventDynamoRepository.getTable(), eventDynamoRepository.buildUpdateTransaction(eventDto, eventCondition));
        txBuilder.addUpdateItem(orderDynamoRepository.getTable(), orderDynamoRepository.buildUpdateTransaction(orderUpdateDto, null));
        for (TicketDto ticketDto : tickets) {
            ticketDto.setStatus("AVAILABLE");
            ticketDto.setOrderId("");
            txBuilder.addUpdateItem(ticketDynamoRepository.getTable(), ticketDynamoRepository.buildUpdateTransaction(ticketDto, null));
        }
        return ticketDynamoRepository.executeTransaction(txBuilder.build())
                .doOnSubscribe(sub -> log.info("Dynamo Release Write",
                        kv("orderId", orderId), kv("releaseWrite", txBuilder.toString())))
                .doOnSuccess(supplier-> log.info("Dynamo Release Write Response", kv("orderId", orderId)))
                .doOnError(error -> log.info("Dynamo Release Write Error", kv("orderId", orderId), kv("error", error.getMessage())))
                .onErrorMap(e -> new TechnicalException(e, GeneralMessage.INTERNAL_SERVER_ERROR));
    }

}
