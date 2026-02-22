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
public class EventDto {
    String pk;
    String sk;
    @JsonInclude(NON_NULL)
    String type;
    String status;
    @JsonInclude(NON_NULL)
    Long totalCapacity;
    @JsonInclude(NON_NULL)
    Long availableCount;
    @JsonInclude(NON_NULL)
    String name;
    @JsonInclude(NON_NULL)
    String place;
    @JsonInclude(NON_NULL)
    String date;
}
