package com.nequi.validator;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

class ValidatorUtilityTest {

    @Test
    void shouldReturnValidUuid() {
        Mono<Boolean> result = ValidatorUtility.isValidUuid(String.valueOf(UUID.randomUUID()));
        StepVerifier.create(result)
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void shouldReturnInValidUuid() {
        Mono<Boolean> result = ValidatorUtility.isValidUuid("12345678h01");
        StepVerifier.create(result)
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void shouldReturnValidDefault() {
        Mono<Boolean> result = ValidatorUtility.isValidDefault("string");
        StepVerifier.create(result)
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void shouldReturnInValidDefault() {
        Mono<Boolean> result = ValidatorUtility.isValidDefault("$@12345678h01");
        StepVerifier.create(result)
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void shouldReturnValidQuantity() {
        Mono<Boolean> result = ValidatorUtility.isValidQuantity(1);
        StepVerifier.create(result)
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void shouldReturnInValidQuantity() {
        Mono<Boolean> result = ValidatorUtility.isValidQuantity(5);
        StepVerifier.create(result)
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void shouldReturnInValidQuantity2() {
        Mono<Boolean> result = ValidatorUtility.isValidQuantity(0);
        StepVerifier.create(result)
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void shouldReturnValidNumeric() {
        Mono<Boolean> result = ValidatorUtility.isValidNumeric("12345");
        StepVerifier.create(result)
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void shouldReturnInValidNumeric() {
        Mono<Boolean> result = ValidatorUtility.isValidNumeric("12345sasfads");
        StepVerifier.create(result)
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void shouldReturnValidDate() {
        Mono<Boolean> result = ValidatorUtility.isValidDate("02/02/2026");
        StepVerifier.create(result)
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void shouldReturnInValidDate() {
        Mono<Boolean> result = ValidatorUtility.isValidDate("12345sasfads@@@@");
        StepVerifier.create(result)
                .expectNext(false)
                .verifyComplete();
    }

}
