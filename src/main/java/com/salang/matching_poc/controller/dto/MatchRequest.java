package com.salang.matching_poc.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record MatchRequest(
    @NotNull
    @JsonProperty("user_id")
    UUID userId
) {}
