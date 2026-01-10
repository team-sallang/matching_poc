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
    private Long lastJoinAt; // optional, MATCHED일 때만 존재, 매칭 시작 시간 (epoch milliseconds)

    @Builder
    public StatusResponse(String status, UUID matchedWith, Long lastJoinAt) {
        this.status = status;
        this.matchedWith = matchedWith;
        this.lastJoinAt = lastJoinAt;
    }
}
