package com.salang.matching_poc.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MatchStatus {
    WAITING("대기중"),
    MATCHED("매칭됨");

    private final String description;
}
