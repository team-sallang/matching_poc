package com.salang.matching_poc.model.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AckResponse {
    private String status; // "IDLE"

    @Builder
    public AckResponse(String status) {
        this.status = status;
    }
}
