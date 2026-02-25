package com.nequi.api;

import com.nequi.model.enums.Operation;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
public class RouterRest {
    @Bean
    public RouterFunction<ServerResponse> routerFunction(Handler handler) {
        return route()
                .POST(Operation.CREATE_EVENT.getPath(), handler::createEvent)
                .GET(Operation.QUERY_EVENTS.getPath(), handler::queryEvents)
                .GET(Operation.QUERY_EVENT.getPath(), handler::queryEvent)
                .GET(Operation.QUERY_AVAILABILITY.getPath(), handler::queryAvailability)
                .POST(Operation.CREATE_ORDER.getPath(), handler::createOrder)
                .GET(Operation.QUERY_ORDER.getPath(), handler::queryOrder)
                .POST(Operation.PAY_ORDER.getPath(), handler::payOrder)
                .build();
    }
}
