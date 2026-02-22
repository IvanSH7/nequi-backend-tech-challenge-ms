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
    String name;
    String date;
    String place;
    String capacity;
    String eventId;
}
