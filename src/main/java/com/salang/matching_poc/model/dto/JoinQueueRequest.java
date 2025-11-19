package com.salang.matching_poc.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
public class JoinQueueRequest {
    @NotNull(message = "userId는 필수입니다")
    private UUID userId;

    @NotBlank(message = "gender는 필수입니다")
    private String gender; // "male" 또는 "female"

    @Builder
    public JoinQueueRequest(UUID userId, String gender) {
        this.userId = userId;
        this.gender = gender;
    }
}
