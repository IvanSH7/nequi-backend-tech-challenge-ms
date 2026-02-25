package com.nequi.sqs.listener.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProcessOrderMessage {
    String id;
    String eventId;
    String quantity;
    String status;
}
