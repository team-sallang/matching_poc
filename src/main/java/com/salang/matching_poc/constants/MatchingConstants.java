package com.salang.matching_poc.constants;

import java.time.ZoneId;

import com.salang.matching_poc.model.enums.MatchStatus;
import com.salang.matching_poc.model.enums.Tier;

public final class MatchingConstants {

    private MatchingConstants() {
    }

    /** 1~4단계 매칭에서 제외하는 등급 (거름 회원) */
    public static final String EXCLUDED_TIER = Tier.FERTILIZER.name();

    /** 매칭 대기 조회 시 사용하는 상태 */
    public static final String WAITING_STATUS = MatchStatus.WAITING.name();

    /** API 응답 타임스탬프용 타임존 (한국) */
    public static final ZoneId ZONE_ASIA_SEOUL = ZoneId.of("Asia/Seoul");
}
