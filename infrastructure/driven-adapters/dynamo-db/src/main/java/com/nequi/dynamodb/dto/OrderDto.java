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
public class OrderDto {
    String pk;
    String sk;
    @JsonInclude(NON_NULL)
    String type;
    String status;
    @JsonInclude(NON_NULL)
    String eventId;
    @JsonInclude(NON_NULL)
    Integer quantity;
    @JsonInclude(NON_NULL)
    Long expiresAt;
}
