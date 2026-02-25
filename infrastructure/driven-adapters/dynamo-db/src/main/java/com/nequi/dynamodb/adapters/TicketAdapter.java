package com.nequi.dynamodb.adapters;

import com.nequi.dynamodb.dto.EventDto;
import com.nequi.dynamodb.dto.OrderDto;
import com.nequi.dynamodb.dto.TicketDto;
import com.nequi.dynamodb.repositories.EventDynamoRepository;
import com.nequi.dynamodb.repositories.OrderDynamoRepository;
import com.nequi.dynamodb.repositories.TicketDynamoRepository;
import com.nequi.dynamodb.util.Utils;
import com.nequi.model.enums.GeneralMessage;
import com.nequi.model.enums.TicketStates;
import com.nequi.model.exception.BusinessException;
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
import static com.nequi.dynamodb.util.Constants.*;
import static com.nequi.model.enums.EventStates.PUBLISHED;
import static com.nequi.model.enums.OrderStates.*;
import static com.nequi.model.enums.TicketStates.AVAILABLE;
import static com.nequi.model.enums.TicketStates.SOLD;
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
                .map(index -> MAPPER.toTicketDto(eventId, index, AVAILABLE.getName()))
                .flatMap(ticketDynamoRepository::save, 25)
                .doOnSubscribe(sub -> log.info("Dynamo Concurrent Save Request", kv("eventId", eventId), kv("totalTickets", requiredTickets)))
                .then().doOnSuccess(supplier-> log.info("Dynamo Concurrent Save Response", kv("eventId", eventId)))
                .doOnError(error -> log.info("Dynamo Concurrent Save Error", kv("concurrentSaveError", error.getMessage())))
                .onErrorMap(Utils::handleTechnicalError);
    }

    @Override
    public Mono<Void> reserveTickets(String eventId, String orderId, Integer orderTickets, Integer ttl) {
        Expression filterExpression = Expression.builder()
                .expression("#status = :status")
                .putExpressionName("#status", "status")
                .putExpressionValue(":status", AttributeValue.builder().s(AVAILABLE.getName()).build())
                .build();
        QueryEnhancedRequest queryRequest = QueryEnhancedRequest.builder()
                .queryConditional(buildQueryConditional(eventId))
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
        Expression filterExpression = Expression.builder()
                .expression("orderId = :orderId")
                .putExpressionValue(":orderId", AttributeValue.builder().s(orderId).build())
                .build();
        QueryEnhancedRequest queryRequest = QueryEnhancedRequest.builder()
                .queryConditional(buildQueryConditional(eventId))
                .filterExpression(filterExpression)
                .build();
        return ticketDynamoRepository.query(queryRequest)
                .flatMap(tickets -> getEventMetadata(eventId)
                        .flatMap(event -> executeReleaseTransaction(event, orderId, tickets)));
    }

    @Override
    public Mono<Void> confirmTickets(String eventId, String orderId) {
        Expression filterExpression = Expression.builder()
                .expression("orderId = :orderId")
                .putExpressionValue(":orderId", AttributeValue.builder().s(orderId).build())
                .build();
        QueryEnhancedRequest queryRequest = QueryEnhancedRequest.builder()
                .queryConditional(buildQueryConditional(eventId))
                .filterExpression(filterExpression)
                .build();

        return ticketDynamoRepository.query(queryRequest)
                .flatMap(tickets -> executeConfirmTransaction(orderId, tickets));
    }

    private QueryConditional buildQueryConditional(String eventId) {
        return QueryConditional.sortBeginsWith(
                Key.builder().partitionValue(BASE_EVENT_PK.concat(eventId)).sortValue(BASE_TICKET_PK).build());
    }

    private Mono<EventDto> getEventMetadata(String eventId) {
        return eventDynamoRepository.getById(BASE_EVENT_PK.concat(eventId), SORT_KEY_METADATA);
    }

    private Mono<Void> executeEnhancedTransaction(EventDto eventDto, String orderId, Integer quantity, List<TicketDto> tickets, Integer ttl) {
        eventDto.setAvailableCount(eventDto.getAvailableCount() - quantity);
        OrderDto updatedOrderDto = MAPPER.toUpdateOrderDto(orderId, RESERVED.getName(), Instant.now().plusSeconds(ttl).getEpochSecond());
        TransactWriteItemsEnhancedRequest.Builder txBuilder = TransactWriteItemsEnhancedRequest.builder();
        Expression eventCondition = Expression.builder()
                .expression("availableCount >= :qty AND #status = :published")
                .putExpressionName("#status", "status")
                .putExpressionValue(":qty", AttributeValue.builder().n(String.valueOf(quantity)).build())
                .putExpressionValue(":published", AttributeValue.builder().s(PUBLISHED.getName()).build())
                .build();
        txBuilder.addUpdateItem(eventDynamoRepository.getTable(), eventDynamoRepository.buildUpdateTransaction(eventDto, eventCondition));
        txBuilder.addUpdateItem(orderDynamoRepository.getTable(), orderDynamoRepository.buildUpdateTransaction(updatedOrderDto, null));
        Expression ticketCondition = Expression.builder()
                .expression("#status = :avail")
                .putExpressionName("#status", "status")
                .putExpressionValue(":avail", AttributeValue.builder().s(AVAILABLE.getName()).build())
                .build();
        for (TicketDto ticketDto : tickets) {
            ticketDto.setStatus(TicketStates.RESERVED.getName());
            ticketDto.setOrderId(orderId);
            txBuilder.addUpdateItem(ticketDynamoRepository.getTable(), ticketDynamoRepository.buildUpdateTransaction(ticketDto, ticketCondition));
        }
        return ticketDynamoRepository.executeTransaction(txBuilder.build())
                .doOnSubscribe(sub -> log.info("Dynamo Reserve Write", kv(ORDER_ID, orderId), kv("quantity", quantity)))
                .doOnSuccess(supplier-> log.info("Dynamo Reserve Write Response", kv(ORDER_ID, orderId)))
                .doOnError(error -> log.info("Dynamo Reserve Write Error", kv(ORDER_ID, orderId), kv("reserveWriteError", error.getMessage())))
                .onErrorMap(Utils::handleTechnicalError);
    }

    private Mono<Void> executeReleaseTransaction(EventDto eventDto, String orderId, List<TicketDto> tickets) {
        eventDto.setAvailableCount(eventDto.getAvailableCount() + tickets.size());
        OrderDto orderUpdateDto = new OrderDto();
        orderUpdateDto.setPk(BASE_ORDER_PK.concat(orderId));
        orderUpdateDto.setSk(SORT_KEY_METADATA);
        orderUpdateDto.setStatus(EXPIRED.getName());
        TransactWriteItemsEnhancedRequest.Builder txBuilder = TransactWriteItemsEnhancedRequest.builder();
        Expression eventCondition = Expression.builder()
                .expression("#status = :published")
                .putExpressionName("#status", "status")
                .putExpressionValue(":published", AttributeValue.builder().s(PUBLISHED.getName()).build())
                .build();
        txBuilder.addUpdateItem(eventDynamoRepository.getTable(), eventDynamoRepository.buildUpdateTransaction(eventDto, eventCondition));
        txBuilder.addUpdateItem(orderDynamoRepository.getTable(), orderDynamoRepository.buildUpdateTransaction(orderUpdateDto, null));
        for (TicketDto ticketDto : tickets) {
            ticketDto.setStatus(AVAILABLE.getName());
            ticketDto.setOrderId("");
            txBuilder.addUpdateItem(ticketDynamoRepository.getTable(), ticketDynamoRepository.buildUpdateTransaction(ticketDto, null));
        }
        return ticketDynamoRepository.executeTransaction(txBuilder.build())
                .doOnSubscribe(sub -> log.info("Dynamo Release Write", kv(ORDER_ID, orderId)))
                .doOnSuccess(supplier-> log.info("Dynamo Release Write Response", kv(ORDER_ID, orderId)))
                .doOnError(error -> log.info("Dynamo Release Write Error", kv(ORDER_ID, orderId), kv("releaseWriteError", error.getMessage())))
                .onErrorMap(Utils::handleTechnicalError);
    }

    private Mono<Void> executeConfirmTransaction(String orderId, List<TicketDto> tickets) {
        OrderDto orderUpdateDto = new OrderDto();
        orderUpdateDto.setPk(BASE_ORDER_PK + orderId);
        orderUpdateDto.setSk(SORT_KEY_METADATA);
        orderUpdateDto.setStatus(CONFIRMED.getName());
        TransactWriteItemsEnhancedRequest.Builder txBuilder = TransactWriteItemsEnhancedRequest.builder();
        Expression orderCondition = Expression.builder()
                .expression("#status = :reserved")
                .putExpressionName("#status", "status")
                .putExpressionValue(":reserved", AttributeValue.builder().s(RESERVED.getName()).build())
                .build();
        txBuilder.addUpdateItem(orderDynamoRepository.getTable(), orderDynamoRepository.buildUpdateTransaction(orderUpdateDto, orderCondition));
        for (TicketDto ticketDto : tickets) {
            ticketDto.setStatus(SOLD.getName());
            Expression ticketCondition = Expression.builder()
                    .expression("#status = :reserved")
                    .putExpressionName("#status", "status")
                    .putExpressionValue(":reserved", AttributeValue.builder().s(TicketStates.RESERVED.getName()).build())
                    .build();
            txBuilder.addUpdateItem(ticketDynamoRepository.getTable(), ticketDynamoRepository.buildUpdateTransaction(ticketDto, ticketCondition));
        }
        return ticketDynamoRepository.executeTransaction(txBuilder.build())
                .doOnSubscribe(sub -> log.info("Dynamo Confirm Write", kv(ORDER_ID, orderId)))
                .doOnSuccess(success -> log.info("Dynamo Confirm Write Response", kv(ORDER_ID, orderId)))
                .doOnError(error -> log.info("Dynamo Confirm Write Error", kv(ORDER_ID, orderId), kv("confirmWriteError", error.getMessage())))
                .onErrorMap(Utils::handleTechnicalError);
    }

}
