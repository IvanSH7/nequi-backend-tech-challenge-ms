package com.nequi.model.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Order {
    String id;
    String eventId;
    Integer quantity;
    String status;
    Long expiresAt;
}
