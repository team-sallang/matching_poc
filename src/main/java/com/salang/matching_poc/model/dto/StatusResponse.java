package com.salang.matching_poc.model.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
public class StatusResponse {
    private String status; // "IDLE" | "WAITING" | "MATCHED"
    private UUID matchedWith; // optional, MATCHED일 때만 존재

    @Builder
    public StatusResponse(String status, UUID matchedWith) {
        this.status = status;
        this.matchedWith = matchedWith;
    }
}
