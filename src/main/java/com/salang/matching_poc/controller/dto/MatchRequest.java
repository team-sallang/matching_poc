package com.salang.matching_poc.controller.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record MatchRequest(
    @NotNull
    UUID userId
) {}
