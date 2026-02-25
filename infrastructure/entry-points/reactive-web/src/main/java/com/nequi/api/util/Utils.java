package com.nequi.api.util;


import com.nequi.model.enums.GeneralMessage;
import com.nequi.model.enums.Operation;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.validation.Errors;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import static com.nequi.api.mapper.HandlerMapper.MAPPER;
import static com.nequi.api.util.Constants.EXCEPTION;
import static com.nequi.api.util.Constants.HEADER_X_REQUEST_ID;
import static net.logstash.logback.argument.StructuredArguments.kv;

@Slf4j
@UtilityClass
public class Utils {

    public static String getRequestId(ServerRequest serverRequest) {
        return serverRequest.headers().firstHeader(HEADER_X_REQUEST_ID);
    }

    public static void logRequest(Operation operation, String path, @Nullable Object request) {
        log.info(operation.getNameRequest(), kv("path", path), kv(operation.getKvRequest(), request));
    }

    public static Mono<ServerResponse> buildResponse(Operation operation, GeneralMessage generalMessage, String requestId) {
        return buildResponse(operation, generalMessage, requestId, null, null);
    }

    public static Mono<ServerResponse> buildResponse(Operation operation, GeneralMessage generalMessage,
                                                     String requestId, Object data) {
        return buildResponse(operation, generalMessage, requestId, data, null);
    }

    public static Mono<ServerResponse> buildResponse(Operation operation, GeneralMessage generalMessage,
                                                     String requestId, Errors errors) {
        return buildResponse(operation, generalMessage, requestId, null, errors);
    }

    public static Mono<ServerResponse> buildResponse(Operation operation, GeneralMessage generalMessage,
                                                     String requestId, @Nullable Object data, @Nullable Errors errors) {
        return Mono.fromSupplier(() -> MAPPER.toResponse(generalMessage, data))
                .flatMap(response -> buildGenericResponse(operation, response, generalMessage, requestId, errors));
    }

    private <T> Mono<ServerResponse> buildGenericResponse(Operation operation, T response,
                                                          GeneralMessage generalMessage,
                                                          String requestId, @Nullable Errors errors) {
        return Mono.defer(() -> {
            HttpStatusCode httpStatusCode = HttpStatus.valueOf(Integer.parseInt(generalMessage.getCode()));
            return Mono.just(httpStatusCode)
                    .filter(HttpStatusCode::is2xxSuccessful)
                    .doOnNext(code -> log.info(operation.getNameResponse(), kv(operation.getKvResponse(), response)))
                    .switchIfEmpty(Mono.defer(() -> {
                        log.info(operation.getNameResponse().concat(" With Exception"),
                                Stream.of(kv(operation.getKvResponse().concat(EXCEPTION), response),
                                                Optional.ofNullable(errors)
                                                        .map(e -> kv("Errors", e.getAllErrors()))
                                                        .orElse(null))
                                        .filter(Objects::nonNull)
                                        .toArray());
                        return Mono.empty();
                    }))
                    .then(ServerResponse.status(httpStatusCode)
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(HEADER_X_REQUEST_ID, requestId)
                            .bodyValue(response));
        });
    }

}
