package com.salang.matching_poc.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor // 코드 수정 필요.
@AllArgsConstructor // 코드 수정 필요.
public class MatchHistoryResponse {
    private UUID id;
    private UUID userAId;
    private UUID userBId;
    private LocalDateTime matchedAt;
    private Integer matchDurationMs;
}
