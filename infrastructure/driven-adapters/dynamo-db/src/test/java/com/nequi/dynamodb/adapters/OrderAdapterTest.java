package com.nequi.dynamodb.adapters;

import com.nequi.dynamodb.dto.OrderDto;
import com.nequi.dynamodb.repositories.OrderDynamoRepository;
import com.nequi.model.enums.GeneralMessage;
import com.nequi.model.exception.TechnicalException;
import com.nequi.model.order.Order;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static com.nequi.dynamodb.util.Constants.BASE_ORDER_PK;
import static com.nequi.dynamodb.util.Constants.SORT_KEY_METADATA;
import static com.nequi.model.enums.OrderStates.PENDING_CONFIRMATION;
import static com.nequi.model.enums.OrderStates.RESERVED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class OrderAdapterTest {

    @Mock
    private OrderDynamoRepository orderDynamoRepository;

    @InjectMocks
    private OrderAdapter orderAdapter;

    private Order order;
    private OrderDto orderDto;
    private static final String ORDER_ID = "order-123";

    @BeforeEach
    void setUp() {
        order = Order.builder()
                .eventId("event-abc")
                .quantity(2)
                .status(PENDING_CONFIRMATION.getName())
                .build();

        orderDto = OrderDto.builder()
                .pk(BASE_ORDER_PK + ORDER_ID)
                .sk(SORT_KEY_METADATA)
                .type("Order")
                .status(PENDING_CONFIRMATION.getName())
                .eventId("EVENT#event-abc")
                .quantity(2)
                .build();
    }

    @Test
    void createOrderSuccess() {
        given(orderDynamoRepository.save(any(OrderDto.class))).willReturn(Mono.just(orderDto));
        StepVerifier.create(orderAdapter.createOrder(order))
                .expectNextMatches(o -> PENDING_CONFIRMATION.getName().equals(o.getStatus()) && o.getQuantity() == 2)
                .verifyComplete();
    }

    @Test
    void createOrderError() {
        given(orderDynamoRepository.save(any(OrderDto.class))).willReturn(Mono.error(new RuntimeException("DynamoDB error")));
        StepVerifier.create(orderAdapter.createOrder(order))
                .expectError(TechnicalException.class)
                .verify();
    }

    @Test
    void updateOrderSuccess() {
        given(orderDynamoRepository.getById(eq(BASE_ORDER_PK + ORDER_ID), eq(SORT_KEY_METADATA)))
                .willReturn(Mono.just(orderDto));
        given(orderDynamoRepository.update(any(OrderDto.class))).willReturn(Mono.just(orderDto));
        StepVerifier.create(orderAdapter.updateOrder(ORDER_ID, RESERVED.getName()))
                .verifyComplete();
    }

    @Test
    void updateOrderNotFound() {
        given(orderDynamoRepository.getById(eq(BASE_ORDER_PK + ORDER_ID), eq(SORT_KEY_METADATA)))
                .willReturn(Mono.empty());
        StepVerifier.create(orderAdapter.updateOrder(ORDER_ID, RESERVED.getName()))
                .expectErrorMatches(t -> t instanceof TechnicalException &&
                        ((TechnicalException) t).getGeneralMessage() == GeneralMessage.ORDER_NOT_FOUND)
                .verify();
    }

    @Test
    void updateOrderError() {
        given(orderDynamoRepository.getById(eq(BASE_ORDER_PK + ORDER_ID), eq(SORT_KEY_METADATA)))
                .willReturn(Mono.just(orderDto));
        given(orderDynamoRepository.update(any(OrderDto.class))).willReturn(Mono.error(new RuntimeException("DynamoDB error")));
        StepVerifier.create(orderAdapter.updateOrder(ORDER_ID, RESERVED.getName()))
                .expectError(TechnicalException.class)
                .verify();
    }

    @Test
    void getOrderSuccess() {
        given(orderDynamoRepository.getById(eq(BASE_ORDER_PK + ORDER_ID), eq(SORT_KEY_METADATA)))
                .willReturn(Mono.just(orderDto));
        StepVerifier.create(orderAdapter.getOrder(ORDER_ID))
                .expectNextMatches(o -> o.getQuantity() == 2)
                .verifyComplete();
    }

    @Test
    void getOrderError() {
        given(orderDynamoRepository.getById(eq(BASE_ORDER_PK + ORDER_ID), eq(SORT_KEY_METADATA)))
                .willReturn(Mono.error(new RuntimeException("DynamoDB error")));
        StepVerifier.create(orderAdapter.getOrder(ORDER_ID))
                .expectError(TechnicalException.class)
                .verify();
    }

}
