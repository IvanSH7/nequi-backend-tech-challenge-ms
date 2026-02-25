package com.nequi.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Response {
    Status status;
    Object data;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Status {
        String code;
        String description;
    }
}
