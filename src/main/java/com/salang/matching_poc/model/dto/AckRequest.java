package com.salang.matching_poc.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
public class AckRequest {
    @NotNull(message = "userId는 필수입니다")
    private UUID userId;

    @Builder
    public AckRequest(UUID userId) {
        this.userId = userId;
    }
}
