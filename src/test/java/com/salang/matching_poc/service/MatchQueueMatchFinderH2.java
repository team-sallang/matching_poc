package com.salang.matching_poc.service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.salang.matching_poc.model.entity.MatchQueue;
import com.salang.matching_poc.model.enums.MatchStatus;
import com.salang.matching_poc.repository.MatchQueueRepository;

@Component
@Profile("test")
public class MatchQueueMatchFinderH2 implements MatchQueueMatchFinder {

        private final MatchQueueRepository matchQueueRepository;

        public MatchQueueMatchFinderH2(MatchQueueRepository matchQueueRepository) {
                this.matchQueueRepository = matchQueueRepository;
        }

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
                List<MatchQueue> candidates = matchQueueRepository.findByStatus(MatchStatus.valueOf(status));
                Set<Integer> requesterHobbySet = hobbyIds != null
                                ? Arrays.stream(hobbyIds).collect(Collectors.toSet())
                                : Set.of();

                return candidates.stream()
                                .filter(mq -> !mq.getUserId().equals(userId))
                                .filter(mq -> !mq.getGender().name().equals(gender))
                                .filter(mq -> !mq.getTier().name().equals(excludedTier))
                                .filter(mq -> mq.getRegion().name().equals(region))
                                .filter(mq -> mq.getBirthYear() != null
                                                && mq.getBirthYear() >= birthYearMin
                                                && mq.getBirthYear() <= birthYearMax)
                                .filter(mq -> hasHobbyIntersection(mq.getHobbyIds(), requesterHobbySet))
                                .sorted((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                                .findFirst();
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
                List<MatchQueue> candidates = matchQueueRepository.findByStatus(MatchStatus.valueOf(status));

                return candidates.stream()
                                .filter(mq -> !mq.getUserId().equals(userId))
                                .filter(mq -> !mq.getGender().name().equals(gender))
                                .filter(mq -> !mq.getTier().name().equals(excludedTier))
                                .filter(mq -> mq.getRegion().name().equals(region))
                                .filter(mq -> mq.getBirthYear() != null
                                                && mq.getBirthYear() >= birthYearMin
                                                && mq.getBirthYear() <= birthYearMax)
                                .sorted((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                                .findFirst();
        }

        @Override
        public Optional<MatchQueue> findPhase3Match(
                        UUID userId,
                        String gender,
                        Integer birthYearMin,
                        Integer birthYearMax,
                        String excludedTier,
                        String status) {
                List<MatchQueue> candidates = matchQueueRepository.findByStatus(MatchStatus.valueOf(status));

                return candidates.stream()
                                .filter(mq -> !mq.getUserId().equals(userId))
                                .filter(mq -> !mq.getGender().name().equals(gender))
                                .filter(mq -> !mq.getTier().name().equals(excludedTier))
                                .filter(mq -> mq.getBirthYear() != null
                                                && mq.getBirthYear() >= birthYearMin
                                                && mq.getBirthYear() <= birthYearMax)
                                .sorted((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                                .findFirst();
        }

        @Override
        public Optional<MatchQueue> findPhase4Match(
                        UUID userId,
                        String gender,
                        String excludedTier,
                        String status) {
                List<MatchQueue> candidates = matchQueueRepository.findByStatus(MatchStatus.valueOf(status));

                return candidates.stream()
                                .filter(mq -> !mq.getUserId().equals(userId))
                                .filter(mq -> !mq.getGender().name().equals(gender))
                                .filter(mq -> !mq.getTier().name().equals(excludedTier))
                                .sorted((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                                .findFirst();
        }

        @Override
        public Optional<MatchQueue> findPhase5Match(UUID userId, String status) {
                List<MatchQueue> candidates = matchQueueRepository.findByStatus(MatchStatus.valueOf(status));

                return candidates.stream()
                                .filter(mq -> !mq.getUserId().equals(userId))
                                .sorted((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                                .findFirst();
        }

        private boolean hasHobbyIntersection(Integer[] partnerHobbies, Set<Integer> requesterHobbySet) {
                if (partnerHobbies == null || partnerHobbies.length == 0) {
                        return false;
                }
                return Arrays.stream(partnerHobbies)
                                .anyMatch(requesterHobbySet::contains);
        }
}
