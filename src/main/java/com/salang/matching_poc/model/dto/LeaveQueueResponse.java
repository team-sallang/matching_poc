package com.salang.matching_poc.model.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LeaveQueueResponse {
    private String status; // "IDLE"

    @Builder
    public LeaveQueueResponse(String status) {
        this.status = status;
    }
}
