package com.nequi.dynamodb.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketDto {
    String pk;
    String sk;
    String type;
    String status;
    Integer version;
    @JsonInclude(NON_NULL)
    String orderId;
}
