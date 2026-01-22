package com.salang.matching_poc.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.salang.matching_poc.model.entity.MatchQueue;
import com.salang.matching_poc.repository.MatchQueueRepository;

import lombok.RequiredArgsConstructor;

// 프로필 기반으로 Test를 위한 로직 구현
@Component
@Profile("!test")
@RequiredArgsConstructor
public class MatchQueueMatchFinderPostgres implements MatchQueueMatchFinder {

    private final MatchQueueRepository matchQueueRepository;

    @Override
    public Optional<MatchQueue> findPhase1Match(
            UUID userId,
            String gender,
            String region,
            Integer birthYearMin,
            Integer birthYearMax,
            Integer[] hobbyIds,
            String excludedTier,
            String status) {
        return matchQueueRepository.findPhase1Match(
                userId, gender, region, birthYearMin, birthYearMax, hobbyIds, excludedTier, status);
    }

    @Override
    public Optional<MatchQueue> findPhase2Match(
            UUID userId,
            String gender,
            String region,
            Integer birthYearMin,
            Integer birthYearMax,
            String excludedTier,
            String status) {
        return matchQueueRepository.findPhase2Match(
                userId, gender, region, birthYearMin, birthYearMax, excludedTier, status);
    }

    @Override
    public Optional<MatchQueue> findPhase3Match(
            UUID userId,
            String gender,
            Integer birthYearMin,
            Integer birthYearMax,
            String excludedTier,
            String status) {
        return matchQueueRepository.findPhase3Match(
                userId, gender, birthYearMin, birthYearMax, excludedTier, status);
    }

    @Override
    public Optional<MatchQueue> findPhase4Match(
            UUID userId,
            String gender,
            String excludedTier,
            String status) {
        return matchQueueRepository.findPhase4Match(userId, gender, excludedTier, status);
    }

    @Override
    public Optional<MatchQueue> findPhase5Match(UUID userId, String status) {
        return matchQueueRepository.findPhase5Match(userId, status);
    }
}
