package com.salang.matching_poc.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor // 코드 수정 필요.
@AllArgsConstructor // 코드 수정 필요.
public class LeaveQueueRequest {
    @NotNull(message = "userId는 필수입니다")
    private UUID userId;
}
