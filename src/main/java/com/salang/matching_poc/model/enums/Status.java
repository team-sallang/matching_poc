package com.salang.matching_poc.model.enums;

public enum Status {
    IDLE,      // 매칭 큐에 참여하지 않은 기본 상태
    WAITING,   // 매칭을 위해 Redis ZSET 큐에 등록된 상태
    MATCHED    // 매칭이 성사된 상태
}

