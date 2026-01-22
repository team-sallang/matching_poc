package com.salang.matching_poc.service;

import java.util.Optional;
import java.util.UUID;

import com.salang.matching_poc.model.entity.MatchQueue;

public interface MatchQueueMatchFinder {

    Optional<MatchQueue> findPhase1Match(
            UUID userId,
            String gender,
            String region,
            Integer birthYearMin,
            Integer birthYearMax,
            Integer[] hobbyIds,
            String excludedTier,
            String status);

    Optional<MatchQueue> findPhase2Match(
            UUID userId,
            String gender,
            String region,
            Integer birthYearMin,
            Integer birthYearMax,
            String excludedTier,
            String status);

    Optional<MatchQueue> findPhase3Match(
            UUID userId,
            String gender,
            Integer birthYearMin,
            Integer birthYearMax,
            String excludedTier,
            String status);

    Optional<MatchQueue> findPhase4Match(
            UUID userId,
            String gender,
            String excludedTier,
            String status);

    Optional<MatchQueue> findPhase5Match(
            UUID userId,
            String status);
}
