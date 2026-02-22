package com.nequi.api.validator;

import com.nequi.api.dto.request.CreateEventRequest;
import com.nequi.api.dto.request.CreateOrderRequest;
import com.nequi.model.enums.GeneralMessage;
import com.nequi.validator.ValidatorUtility;
import lombok.experimental.UtilityClass;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@UtilityClass
public class HandlerValidator {

    private record ValidationRule(Mono<Boolean> validation, GeneralMessage generalMessage) {}

    public static Mono<Errors> validateCreateEvent(CreateEventRequest createEventRequest, String requestId) {
        List<ValidationRule> rules = new ArrayList<>();

        rules.add(new ValidationRule(ValidatorUtility.isValidUuid(requestId), GeneralMessage.INVALID_REQUEST_ID));

        rules.add(new ValidationRule(ValidatorUtility.isValidDefault(createEventRequest.getName()), GeneralMessage.INVALID_EVENT_NAME));
        rules.add(new ValidationRule(ValidatorUtility.isValidDate(createEventRequest.getDate()), GeneralMessage.INVALID_EVENT_DATE));
        rules.add(new ValidationRule(ValidatorUtility.isValidDefault(createEventRequest.getPlace()), GeneralMessage.INVALID_EVENT_PLACE));
        rules.add(new ValidationRule(ValidatorUtility.isValidNumeric(createEventRequest.getCapacity()), GeneralMessage.INVALID_EVENT_CAPACITY));

        return processValidations(rules, "createEvent");
    }

    public static Mono<Errors> validateQueryEvents(String requestId) {
        List<ValidationRule> rules = new ArrayList<>();

        rules.add(new ValidationRule(ValidatorUtility.isValidUuid(requestId), GeneralMessage.INVALID_REQUEST_ID));

        return processValidations(rules, "queryEvents");
    }

    public static Mono<Errors> validateQuery(String eventId, String requestId) {
        List<ValidationRule> rules = new ArrayList<>();

        rules.add(new ValidationRule(ValidatorUtility.isValidUuid(requestId), GeneralMessage.INVALID_REQUEST_ID));
        rules.add(new ValidationRule(ValidatorUtility.isValidUuid(eventId), GeneralMessage.INVALID_ID));

        return processValidations(rules, "validateQuery");
    }

    public static Mono<Errors> validateCreateOrder(CreateOrderRequest createOrderRequest, String requestId) {
        List<ValidationRule> rules = new ArrayList<>();

        rules.add(new ValidationRule(ValidatorUtility.isValidUuid(requestId), GeneralMessage.INVALID_REQUEST_ID));

        rules.add(new ValidationRule(ValidatorUtility.isValidUuid(createOrderRequest.getEventId()), GeneralMessage.INVALID_ID));
        rules.add(new ValidationRule(ValidatorUtility.isValidQuantity(createOrderRequest.getQuantity()), GeneralMessage.INVALID_ORDER_QUANTITY));

        return processValidations(rules, "createOrder");
    }


    private static Mono<Errors> processValidations(List<ValidationRule> rules, String objectName) {
        return Flux.fromIterable(rules)
                .flatMap(rule -> rule.validation()
                        .filter(isValid -> !isValid)
                        .map(isValid -> rule.generalMessage())
                )
                .collect(() -> new BeanPropertyBindingResult(new Object(), objectName),
                        (errors, errorMessage) -> errors.reject(errorMessage.getExternalCode(), errorMessage.getExternalMessage())
                );
    }

    private static <T, R> ValidationRule createRule(T parent, Function<T, R> getter, Function<R, Mono<Boolean>> validator, GeneralMessage errorMessage) {
        Mono<Boolean> validationMono = Optional.ofNullable(parent)
                .map(getter)
                .map(validator)
                .orElse(Mono.just(Boolean.FALSE));
        return new ValidationRule(validationMono, errorMessage);
    }

}