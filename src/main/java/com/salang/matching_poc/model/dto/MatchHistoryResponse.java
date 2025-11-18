package com.salang.matching_poc.model.dto;

import com.salang.matching_poc.model.entity.MatchHistory;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class MatchHistoryResponse {
    private UUID id;
    private UUID userAId;
    private UUID userBId;
    private LocalDateTime matchedAt;
    private Integer matchDurationMs;

    public MatchHistoryResponse(MatchHistory entity) {
        this.id = entity.getId();
        this.userAId = entity.getUserAId();
        this.userBId = entity.getUserBId();
        this.matchedAt = entity.getMatchedAt();
        this.matchDurationMs = entity.getMatchDurationMs();
    }
}
