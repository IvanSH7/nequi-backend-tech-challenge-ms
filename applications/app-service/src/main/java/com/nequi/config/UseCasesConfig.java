package com.nequi.config;

import com.nequi.model.order.gateways.OrderGateway;
import com.nequi.model.order.gateways.ProcessorGateway;
import com.nequi.model.ticketing.gateways.TicketingGateway;
import com.nequi.usecase.event.EventUseCase;
import com.nequi.usecase.order.OrderUseCase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

@Configuration
@ComponentScan(basePackages = "com.nequi.usecase",
        includeFilters = {
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "^.+UseCase$")
        },
        useDefaultFilters = false)
public class UseCasesConfig {

        @Bean
        public OrderUseCase orderUseCase(EventUseCase eventUseCase,
                                         OrderGateway orderGateway,
                                         ProcessorGateway processorGateway,
                                         TicketingGateway ticketingGateway,
                                         @Value("${use-case.order.time-to-pay}") String timeToPay) {
                return new OrderUseCase(eventUseCase, orderGateway, processorGateway, ticketingGateway, Integer.valueOf(timeToPay));
        }

}
