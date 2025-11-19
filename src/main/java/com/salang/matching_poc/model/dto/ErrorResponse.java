package com.salang.matching_poc.model.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ErrorResponse {
    private String error; // "ALREADY_IN_QUEUE" | "TOO_MANY_REQUESTS" | "CANNOT_LEAVE_MATCHED"

    @Builder
    public ErrorResponse(String error) {
        this.error = error;
    }
}
