package com.salang.matching_poc.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor // 코드 수정 필요.
@AllArgsConstructor // 코드 수정 필요.
public class StatusResponse {
    private String status; // "IDLE" | "WAITING" | "MATCHED"
    private UUID matchedWith; // optional, MATCHED일 때만 존재
}
