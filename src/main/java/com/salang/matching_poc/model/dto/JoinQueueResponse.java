package com.salang.matching_poc.model.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class JoinQueueResponse {
    private String status; // "WAITING"

    @Builder
    public JoinQueueResponse(String status) {
        this.status = status;
    }
}
