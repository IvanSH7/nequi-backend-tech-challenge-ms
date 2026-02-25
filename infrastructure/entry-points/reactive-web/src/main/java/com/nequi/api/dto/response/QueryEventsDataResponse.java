package com.nequi.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QueryEventsDataResponse {
    String eventId;
    String name;
    String status;
    String place;
    String date;
    String capacity;
}
