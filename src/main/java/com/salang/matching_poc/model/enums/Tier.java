package com.salang.matching_poc.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Tier {
    FERTILIZER("거름"), // -21점 이하 거름회원
    WILTING("시들"),    // -20 ~ -11점 시들회원
    SPROUT("새싹"),     // -10 ~ +10점 새싹회원
    PETAL("꽃잎"),      // 11 ~ 20점 꽃잎회원
    FRUIT("열매");      // 21점 이상 열매회원

    private final String description;
}
