package com.nequi.dynamodb.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDto {
    String pk;
    String sk;
    String type;
    String status;
    Long expiresAt;
}
