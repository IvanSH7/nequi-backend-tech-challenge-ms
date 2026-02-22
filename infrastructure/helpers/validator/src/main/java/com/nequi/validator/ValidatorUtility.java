package com.nequi.validator;

import io.micrometer.common.util.StringUtils;
import lombok.experimental.UtilityClass;
import reactor.core.publisher.Mono;

import java.util.regex.Pattern;

import static com.nequi.validator.util.Constants.*;

@UtilityClass
public class ValidatorUtility {

    public static Mono<Boolean> isValidUuid(String uuid) {
        return Mono.fromCallable(() -> {
            try {
                java.util.UUID.fromString(uuid);
                return true;
            } catch (Exception e) {
                return false;
            }
        });
    }

    public static Mono<Boolean> isValidDefault(String value) {
        return isValidFieldMatchesRegularPhrase(value, REGULAR_PHRASE_DEFAULT);
    }

    public static Mono<Boolean> isValidNumeric(String value) {
        return isValidFieldMatchesRegularPhrase(value, REGULAR_PHRASE_NUMERIC);
    }

    public static Mono<Boolean> isValidDate(String value) {
        return isValidFieldMatchesRegularPhrase(value, REGULAR_PHRASE_DATE);
    }

    private static Mono<Boolean> isValidFieldMatchesRegularPhrase(String field, String regularPhrase){
        return Mono.defer(() -> Mono.just(StringUtils.isNotBlank(field) &&
                Pattern.compile(regularPhrase).matcher(field).matches()));
    }
}
