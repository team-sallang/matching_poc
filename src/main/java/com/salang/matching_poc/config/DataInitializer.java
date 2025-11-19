package com.salang.matching_poc.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 테스트용 사용자 데이터 초기화
 * 실제 프로덕션에서는 필요 없을 수 있음
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    // 테스트용 사용자 ID 목록 (필요시 사용)
    public static final List<UUID> TEST_USER_IDS = new ArrayList<>();

    @Override
    public void run(ApplicationArguments args) {
        // 테스트용 사용자 ID 생성 (1000명)
        for (int i = 0; i < 1000; i++) {
            TEST_USER_IDS.add(UUID.randomUUID());
        }
        log.info("Initialized {} test user IDs", TEST_USER_IDS.size());
    }
}

