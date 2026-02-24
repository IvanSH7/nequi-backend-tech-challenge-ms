package com.nequi.api;

import com.nequi.api.dto.request.CreateEventRequest;
import com.nequi.api.dto.request.CreateOrderRequest;
import com.nequi.api.dto.response.Response;
import com.nequi.model.enums.GeneralMessage;
import com.nequi.model.event.Event;
import com.nequi.model.exception.TechnicalException;
import com.nequi.model.order.Order;
import com.nequi.usecase.event.EventUseCase;
import com.nequi.usecase.order.OrderUseCase;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.fallback.FallbackMethod;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static com.nequi.api.util.Constants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@WebFluxTest
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {RouterRest.class, Handler.class})
class HandlerTest {

    @Autowired
    private ApplicationContext context;

    @MockitoBean
    private EventUseCase eventUseCase;
    @MockitoBean
    private OrderUseCase orderUseCase;

    private WebTestClient webTestClient;
    private String requestId;

    @BeforeEach
    void setUp() {
        requestId = UUID.randomUUID().toString();
        webTestClient = WebTestClient.bindToApplicationContext(context)
                .configureClient()
                .build();
    }

    @Test
    void shouldReturnSuccessResponseOnCreateEvent() {
        HashMap<String, String> data = new HashMap<>();
        data.put("id", "9ed544f3-a1e9-4b17-bc4e-790574ae2efb");
        given(eventUseCase.create(any())).willReturn(Mono.just(data.get("id")));
        webTestClient.post()
                .uri("/api/v1/events")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HEADER_X_REQUEST_ID, requestId)
                .body(Mono.just(CreateEventRequest.builder()
                        .name("Event1")
                        .place("Medellin")
                        .date("02/02/2027")
                        .capacity("10")
                        .build()), CreateEventRequest.class)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(Response.class)
                .value(response -> {
                    Assertions.assertNotNull(response);
                    Assertions.assertEquals(GeneralMessage.CREATED.getExternalCode(), response.getStatus().getCode());
                    Assertions.assertEquals(GeneralMessage.CREATED.getExternalMessage(), response.getStatus().getDescription());
                    Assertions.assertEquals(data, response.getData());
                });
        verify(eventUseCase).create(any());
        verifyNoInteractions(orderUseCase);

    }

    @Test
    void shouldReturnBadRequestOnCreateEvent() {
        webTestClient.post()
                .uri("/api/v1/events")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HEADER_X_REQUEST_ID, "")
                .body(Mono.just(CreateEventRequest.builder().build()), CreateEventRequest.class)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(Response.class)
                .value(response -> {
                    Assertions.assertNotNull(response);
                    Assertions.assertEquals(GeneralMessage.BAD_REQUEST.getExternalCode(), response.getStatus().getCode());
                    Assertions.assertEquals(GeneralMessage.BAD_REQUEST.getExternalMessage(), response.getStatus().getDescription());
                });
        verifyNoInteractions(eventUseCase);
        verifyNoInteractions(orderUseCase);
    }

    @Test
    void shouldReturnInternalServerErrorOnCreateEvent() {
        given(eventUseCase.create(any())).willReturn(Mono.error(new TechnicalException(GeneralMessage.INTERNAL_SERVER_ERROR)));
        webTestClient.post()
                .uri("/api/v1/events")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HEADER_X_REQUEST_ID, requestId)
                .body(Mono.just(CreateEventRequest.builder()
                        .name("Event1")
                        .place("Medellin")
                        .date("02/02/2027")
                        .capacity("10")
                        .build()), CreateEventRequest.class)
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody(Response.class)
                .value(response -> {
                    Assertions.assertNotNull(response);
                    Assertions.assertEquals(GeneralMessage.INTERNAL_SERVER_ERROR.getExternalCode(), response.getStatus().getCode());
                    Assertions.assertEquals(GeneralMessage.INTERNAL_SERVER_ERROR.getExternalMessage(), response.getStatus().getDescription());
                });
        verify(eventUseCase).create(any());
        verifyNoInteractions(orderUseCase);
    }

    @Test
    void shouldReturnSuccessResponseOnQueryEvent() {
        HashMap<String, String> data = new HashMap<>();
        data.put("capacity", "10");
        data.put("date", "02/02/2027");
        data.put("eventId", "9ed544f3-a1e9-4b17-bc4e-790574ae2efb");
        data.put("name", "Test");
        data.put("place", "Medellin");
        data.put("status", "PUBLISHED");
        given(eventUseCase.queryEvent(any())).willReturn(Mono.just(
                Event.builder()
                        .capacity(data.get("capacity"))
                        .date(data.get("date"))
                        .id(data.get("eventId"))
                        .name(data.get("name"))
                        .place(data.get("place"))
                        .status(data.get("status"))
                        .build()));
        webTestClient.get()
                .uri("/api/v1/events/9ed544f3-a1e9-4b17-bc4e-790574ae2efb")
                .header(HEADER_X_REQUEST_ID, requestId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Response.class)
                .value(response -> {
                    Assertions.assertNotNull(response);
                    Assertions.assertEquals(GeneralMessage.SUCCESS.getExternalCode(), response.getStatus().getCode());
                    Assertions.assertEquals(GeneralMessage.SUCCESS.getExternalMessage(), response.getStatus().getDescription());
                    Assertions.assertEquals(data, response.getData());
                });
        verify(eventUseCase).queryEvent(any());
    }

    @Test
    void shouldReturnBadRequestOnQueryEvent() {
        webTestClient.get()
                .uri("/api/v1/events/123")
                .header(HEADER_X_REQUEST_ID, "")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(Response.class)
                .value(response -> {
                    Assertions.assertNotNull(response);
                    Assertions.assertEquals(GeneralMessage.BAD_REQUEST.getExternalCode(), response.getStatus().getCode());
                    Assertions.assertEquals(GeneralMessage.BAD_REQUEST.getExternalMessage(), response.getStatus().getDescription());
                });
        verifyNoInteractions(eventUseCase);
    }

    @Test
    void shouldReturnInternalServerErrorOnQueryEvent() {
        given(eventUseCase.queryEvent(any())).willReturn(Mono.error(new TechnicalException(GeneralMessage.INTERNAL_SERVER_ERROR)));
        webTestClient.get()
                .uri("/api/v1/events/9ed544f3-a1e9-4b17-bc4e-790574ae2efb")
                .header(HEADER_X_REQUEST_ID, requestId)
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody(Response.class)
                .value(response -> {
                    Assertions.assertNotNull(response);
                    Assertions.assertEquals(GeneralMessage.INTERNAL_SERVER_ERROR.getExternalCode(), response.getStatus().getCode());
                    Assertions.assertEquals(GeneralMessage.INTERNAL_SERVER_ERROR.getExternalMessage(), response.getStatus().getDescription());
                });
        verify(eventUseCase).queryEvent(any());
        verifyNoInteractions(orderUseCase);
    }

    @Test
    void shouldReturnSuccessResponseOnQueryEvents() {
        HashMap<String, String> data = new HashMap<>();
        data.put("capacity", "10");
        data.put("date", "02/02/2027");
        data.put("eventId", "9ed544f3-a1e9-4b17-bc4e-790574ae2efb");
        data.put("name", "Test");
        data.put("place", "Medellin");
        data.put("status", "PUBLISHED");
        given(eventUseCase.queryEvents()).willReturn(Mono.just(
                List.of(Event.builder()
                        .capacity(data.get("capacity"))
                        .date(data.get("date"))
                        .id(data.get("eventId"))
                        .name(data.get("name"))
                        .place(data.get("place"))
                        .status(data.get("status"))
                        .build())));
        webTestClient.get()
                .uri("/api/v1/events")
                .header(HEADER_X_REQUEST_ID, requestId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Response.class)
                .value(response -> {
                    Assertions.assertNotNull(response);
                    Assertions.assertEquals(GeneralMessage.SUCCESS.getExternalCode(), response.getStatus().getCode());
                    Assertions.assertEquals(GeneralMessage.SUCCESS.getExternalMessage(), response.getStatus().getDescription());
                    Assertions.assertEquals(List.of(data), response.getData());
                });
        verify(eventUseCase).queryEvents();
    }

    @Test
    void shouldReturnBadRequestOnQueryEvents() {
        webTestClient.get()
                .uri("/api/v1/events")
                .header(HEADER_X_REQUEST_ID, "")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(Response.class)
                .value(response -> {
                    Assertions.assertNotNull(response);
                    Assertions.assertEquals(GeneralMessage.BAD_REQUEST.getExternalCode(), response.getStatus().getCode());
                    Assertions.assertEquals(GeneralMessage.BAD_REQUEST.getExternalMessage(), response.getStatus().getDescription());
                });
        verifyNoInteractions(eventUseCase);
    }

    @Test
    void shouldReturnInternalServerErrorOnQueryEvents() {
        given(eventUseCase.queryEvents()).willReturn(Mono.error(new TechnicalException(GeneralMessage.INTERNAL_SERVER_ERROR)));
        webTestClient.get()
                .uri("/api/v1/events")
                .header(HEADER_X_REQUEST_ID, requestId)
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody(Response.class)
                .value(response -> {
                    Assertions.assertNotNull(response);
                    Assertions.assertEquals(GeneralMessage.INTERNAL_SERVER_ERROR.getExternalCode(), response.getStatus().getCode());
                    Assertions.assertEquals(GeneralMessage.INTERNAL_SERVER_ERROR.getExternalMessage(), response.getStatus().getDescription());
                });
        verify(eventUseCase).queryEvents();
        verifyNoInteractions(orderUseCase);
    }

    @Test
    void shouldReturnSuccessResponseOnQueryAvailability() {
        HashMap<String, String> data = new HashMap<>();
        data.put("remainingCapacity", "10");
        given(eventUseCase.queryEvent(any())).willReturn(Mono.just(
                Event.builder()
                        .availability(data.get("remainingCapacity"))
                        .build()));
        webTestClient.get()
                .uri("/api/v1/events/9ed544f3-a1e9-4b17-bc4e-790574ae2efb/availability")
                .header(HEADER_X_REQUEST_ID, requestId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Response.class)
                .value(response -> {
                    Assertions.assertNotNull(response);
                    Assertions.assertEquals(GeneralMessage.SUCCESS.getExternalCode(), response.getStatus().getCode());
                    Assertions.assertEquals(GeneralMessage.SUCCESS.getExternalMessage(), response.getStatus().getDescription());
                    Assertions.assertEquals(data, response.getData());
                });
        verify(eventUseCase).queryEvent(any());
    }

    @Test
    void shouldReturnBadRequestOnQueryAvailability() {
        webTestClient.get()
                .uri("/api/v1/events/123/availability")
                .header(HEADER_X_REQUEST_ID, "")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(Response.class)
                .value(response -> {
                    Assertions.assertNotNull(response);
                    Assertions.assertEquals(GeneralMessage.BAD_REQUEST.getExternalCode(), response.getStatus().getCode());
                    Assertions.assertEquals(GeneralMessage.BAD_REQUEST.getExternalMessage(), response.getStatus().getDescription());
                });
        verifyNoInteractions(eventUseCase);
    }

    @Test
    void shouldReturnInternalServerErrorOnQueryAvailability() {
        given(eventUseCase.queryEvent(any())).willReturn(Mono.error(new TechnicalException(GeneralMessage.INTERNAL_SERVER_ERROR)));
        webTestClient.get()
                .uri("/api/v1/events/9ed544f3-a1e9-4b17-bc4e-790574ae2efb/availability")
                .header(HEADER_X_REQUEST_ID, requestId)
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody(Response.class)
                .value(response -> {
                    Assertions.assertNotNull(response);
                    Assertions.assertEquals(GeneralMessage.INTERNAL_SERVER_ERROR.getExternalCode(), response.getStatus().getCode());
                    Assertions.assertEquals(GeneralMessage.INTERNAL_SERVER_ERROR.getExternalMessage(), response.getStatus().getDescription());
                });
        verify(eventUseCase).queryEvent(any());
        verifyNoInteractions(orderUseCase);
    }

    @Test
    void shouldReturnSuccessResponseOnCreateOrder() {
        HashMap<String, String> data = new HashMap<>();
        data.put("id", "74b7499a-72f0-4ba4-8e7f-bb2deb64f93b");
        given(orderUseCase.create(any())).willReturn(Mono.just(data.get("id")));
        webTestClient.post()
                .uri("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HEADER_X_REQUEST_ID, requestId)
                .body(Mono.just(CreateOrderRequest.builder()
                        .eventId("9ed544f3-a1e9-4b17-bc4e-790574ae2efb")
                        .quantity(2)
                        .build()), CreateOrderRequest.class)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(Response.class)
                .value(response -> {
                    Assertions.assertNotNull(response);
                    Assertions.assertEquals(GeneralMessage.CREATED.getExternalCode(), response.getStatus().getCode());
                    Assertions.assertEquals(GeneralMessage.CREATED.getExternalMessage(), response.getStatus().getDescription());
                    Assertions.assertEquals(data, response.getData());
                });
        verify(orderUseCase).create(any());
    }

    @Test
    void shouldReturnBadRequestOnCreateOrder() {
        webTestClient.post()
                .uri("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HEADER_X_REQUEST_ID, "")
                .body(Mono.just(CreateOrderRequest.builder().build()), CreateOrderRequest.class)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(Response.class)
                .value(response -> {
                    Assertions.assertNotNull(response);
                    Assertions.assertEquals(GeneralMessage.BAD_REQUEST.getExternalCode(), response.getStatus().getCode());
                    Assertions.assertEquals(GeneralMessage.BAD_REQUEST.getExternalMessage(), response.getStatus().getDescription());
                });
        verifyNoInteractions(orderUseCase);
    }

    @Test
    void shouldReturnInternalServerErrorOnCreateOrder() {
        given(orderUseCase.create(any())).willReturn(Mono.error(new TechnicalException(GeneralMessage.INTERNAL_SERVER_ERROR)));
        webTestClient.post()
                .uri("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HEADER_X_REQUEST_ID, requestId)
                .body(Mono.just(CreateOrderRequest.builder()
                        .eventId("9ed544f3-a1e9-4b17-bc4e-790574ae2efb")
                        .quantity(2)
                        .build()), CreateOrderRequest.class)
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody(Response.class)
                .value(response -> {
                    Assertions.assertNotNull(response);
                    Assertions.assertEquals(GeneralMessage.INTERNAL_SERVER_ERROR.getExternalCode(), response.getStatus().getCode());
                    Assertions.assertEquals(GeneralMessage.INTERNAL_SERVER_ERROR.getExternalMessage(), response.getStatus().getDescription());
                });
        verify(orderUseCase).create(any());
        verifyNoInteractions(eventUseCase);
    }

    @Test
    void shouldReturnSuccessResponseOnQueryOrder() {
        HashMap<String, String> data = new HashMap<>();
        data.put("status", "RESERVED");
        given(orderUseCase.queryOrder(any())).willReturn(Mono.just(
                Order.builder()
                        .status(data.get("status"))
                        .build()));
        webTestClient.get()
                .uri("/api/v1/orders/74b7499a-72f0-4ba4-8e7f-bb2deb64f93b")
                .header(HEADER_X_REQUEST_ID, requestId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Response.class)
                .value(response -> {
                    Assertions.assertNotNull(response);
                    Assertions.assertEquals(GeneralMessage.SUCCESS.getExternalCode(), response.getStatus().getCode());
                    Assertions.assertEquals(GeneralMessage.SUCCESS.getExternalMessage(), response.getStatus().getDescription());
                    Assertions.assertEquals(data, response.getData());
                });
        verify(orderUseCase).queryOrder(any());
    }

    @Test
    void shouldReturnBadRequestOnQueryOrder() {
        webTestClient.get()
                .uri("/api/v1/orders/123")
                .header(HEADER_X_REQUEST_ID, "")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(Response.class)
                .value(response -> {
                    Assertions.assertNotNull(response);
                    Assertions.assertEquals(GeneralMessage.BAD_REQUEST.getExternalCode(), response.getStatus().getCode());
                    Assertions.assertEquals(GeneralMessage.BAD_REQUEST.getExternalMessage(), response.getStatus().getDescription());
                });
        verifyNoInteractions(orderUseCase);
    }

    @Test
    void shouldReturnInternalServerErrorOnQueryOrder() {
        given(orderUseCase.queryOrder(any())).willReturn(Mono.error(new TechnicalException(GeneralMessage.INTERNAL_SERVER_ERROR)));
        webTestClient.get()
                .uri("/api/v1/orders/74b7499a-72f0-4ba4-8e7f-bb2deb64f93b")
                .header(HEADER_X_REQUEST_ID, requestId)
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody(Response.class)
                .value(response -> {
                    Assertions.assertNotNull(response);
                    Assertions.assertEquals(GeneralMessage.INTERNAL_SERVER_ERROR.getExternalCode(), response.getStatus().getCode());
                    Assertions.assertEquals(GeneralMessage.INTERNAL_SERVER_ERROR.getExternalMessage(), response.getStatus().getDescription());
                });
        verify(orderUseCase).queryOrder(any());
        verifyNoInteractions(eventUseCase);
    }

    @Test
    void shouldReturnSuccessResponseOnPayOrder() {
        given(orderUseCase.payOrder(any())).willReturn(Mono.empty());
        webTestClient.post()
                .uri("/api/v1/orders/74b7499a-72f0-4ba4-8e7f-bb2deb64f93b/pay")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HEADER_X_REQUEST_ID, requestId)
                .exchange()
                .expectStatus().isAccepted()
                .expectBody(Response.class)
                .value(response -> {
                    Assertions.assertNotNull(response);
                    Assertions.assertEquals(GeneralMessage.ACCEPTED.getExternalCode(), response.getStatus().getCode());
                    Assertions.assertEquals(GeneralMessage.ACCEPTED.getExternalMessage(), response.getStatus().getDescription());
                });
        verify(orderUseCase).payOrder(any());
    }

    @Test
    void shouldReturnBadRequestOnPayOrder() {
        webTestClient.post()
                .uri("/api/v1/orders/123/pay")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HEADER_X_REQUEST_ID, "")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(Response.class)
                .value(response -> {
                    Assertions.assertNotNull(response);
                    Assertions.assertEquals(GeneralMessage.BAD_REQUEST.getExternalCode(), response.getStatus().getCode());
                    Assertions.assertEquals(GeneralMessage.BAD_REQUEST.getExternalMessage(), response.getStatus().getDescription());
                });
        verifyNoInteractions(orderUseCase);
    }

    @Test
    void shouldReturnInternalServerErrorOnPayOrder() {
        given(orderUseCase.payOrder(any())).willReturn(Mono.error(new TechnicalException(GeneralMessage.INTERNAL_SERVER_ERROR)));
        webTestClient.post()
                .uri("/api/v1/orders/74b7499a-72f0-4ba4-8e7f-bb2deb64f93b/pay")
                .header(HEADER_X_REQUEST_ID, requestId)
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody(Response.class)
                .value(response -> {
                    Assertions.assertNotNull(response);
                    Assertions.assertEquals(GeneralMessage.INTERNAL_SERVER_ERROR.getExternalCode(), response.getStatus().getCode());
                    Assertions.assertEquals(GeneralMessage.INTERNAL_SERVER_ERROR.getExternalMessage(), response.getStatus().getDescription());
                });
        verify(orderUseCase).payOrder(any());
        verifyNoInteractions(eventUseCase);
    }


    @Test
    void verifyDefaultFallbackMethod() throws Throwable {
        MockServerHttpRequest request = MockServerHttpRequest
                .post("/api/v1/events")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HEADER_X_REQUEST_ID, requestId)
                .build();

        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        ServerRequest serverRequest = ServerRequest.create(exchange, HandlerStrategies.withDefaults().messageReaders());

        Handler target = new Handler(eventUseCase, orderUseCase);
        Method testMethod = target.getClass().getMethod("createEvent", ServerRequest.class);
        FallbackMethod fallbackMethod = FallbackMethod
                .create(FALLBACK_METHOD_NAME, testMethod, new Object[]{serverRequest}, target, target);

        Mono<ServerResponse> responseError = (Mono<ServerResponse>)
                fallbackMethod.fallback(new IllegalStateException("An error has been occurred"));

        StepVerifier.create(responseError)
                .expectNextMatches(response -> {
                    assertThat(response.statusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void verifyFallbackCircuitBreakerMethod() throws Throwable {
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("createEvent");

        MockServerHttpRequest request = MockServerHttpRequest
                .post("/api/v1/events")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HEADER_X_REQUEST_ID, requestId)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        ServerRequest serverRequest = ServerRequest.create(exchange, HandlerStrategies.withDefaults().messageReaders());

        Handler target = new Handler(eventUseCase, orderUseCase);
        Method testMethod = target.getClass().getMethod("createEvent", ServerRequest.class);

        FallbackMethod fallbackMethod = FallbackMethod
                .create(FALLBACK_METHOD_NAME, testMethod, new Object[]{serverRequest}, target, target);

        Mono<ServerResponse> responseError = (Mono<ServerResponse>)
                fallbackMethod.fallback(CallNotPermittedException.createCallNotPermittedException(circuitBreaker));

        StepVerifier.create(responseError)
                .expectNextMatches(response -> {
                    assertThat(response.statusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void verifyFallbackCircuitBreakerTechnicalException() throws Throwable {
        MockServerHttpRequest request = MockServerHttpRequest
                .post("/api/v1/events")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HEADER_X_REQUEST_ID, requestId)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        ServerRequest serverRequest = ServerRequest.create(exchange, HandlerStrategies.withDefaults().messageReaders());

        Handler target = new Handler(eventUseCase, orderUseCase);
        Method testMethod = target.getClass().getMethod("createEvent", ServerRequest.class);

        FallbackMethod fallbackMethod = FallbackMethod
                .create(FALLBACK_METHOD_NAME, testMethod, new Object[]{serverRequest}, target, target);

        Mono<ServerResponse> responseError = (Mono<ServerResponse>)
                fallbackMethod.fallback(new TechnicalException(GeneralMessage.INTERNAL_SERVER_ERROR));

        StepVerifier.create(responseError)
                .expectNextMatches(response -> {
                    assertThat(response.statusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                    return true;
                })
                .verifyComplete();
    }

}
