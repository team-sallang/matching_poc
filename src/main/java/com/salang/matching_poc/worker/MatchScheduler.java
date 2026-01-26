package com.salang.matching_poc.worker;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import static com.salang.matching_poc.constants.MatchingConstants.AGE_TOLERANCE_YEARS;
import static com.salang.matching_poc.constants.MatchingConstants.EXCLUDED_TIER;
import static com.salang.matching_poc.constants.MatchingConstants.WAITING_STATUS;
import com.salang.matching_poc.exception.MatchAlreadyProcessedException;
import com.salang.matching_poc.model.entity.MatchQueue;
import com.salang.matching_poc.model.enums.MatchStatus;
import com.salang.matching_poc.repository.MatchQueueRepository;
import com.salang.matching_poc.service.MatchQueueMatchFinder;
import com.salang.matching_poc.service.MatchService;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class MatchScheduler {

    private final MatchQueueRepository matchQueueRepository;
    private final MatchService matchService;
    private final MatchQueueMatchFinder matchQueueMatchFinder;
    private final MeterRegistry meterRegistry;
    private final ObservationRegistry observationRegistry;
    private final AtomicInteger waitingQueueGauge = new AtomicInteger(0);

    @PostConstruct
    void initMetrics() {
        io.micrometer.core.instrument.Gauge.builder(
                "match.queue.waiting.count", waitingQueueGauge, AtomicInteger::get)
                .description("Current number of users waiting in match queue")
                .register(meterRegistry);
    }

    /*
     * 실행 주기 1초 : 판단 근거 아자르 주기 참고.
     * 트랜잭션 없음 : 매칭 확정·룸 생성은 MatchService.confirmMatch()에서 단건별 트랜잭션 처리.
     */
    @Scheduled(fixedDelayString = "${matching.schedule.fixed-delay}")
    public void runMatchingLoop() {
        Observation loopObservation = Observation.start("match.scheduler.loop", observationRegistry);
        try (Observation.Scope scope = loopObservation.openScope()) {
            List<MatchQueue> waitingUsers = matchQueueRepository.findByStatus(MatchStatus.WAITING);
            waitingQueueGauge.set(waitingUsers.size());
            if (waitingUsers.isEmpty()) {
                return;
            }

            log.info("매칭 스케줄러 실행. 대기열 사용자 수: {}명", waitingUsers.size());
            Collections.shuffle(waitingUsers);

            for (MatchQueue requester : waitingUsers) {
                // 이미 다른 스레드에서 처리되었을 수 있으므로 상태를 다시 확인
                if (requester.getStatus() != MatchStatus.WAITING) {
                    continue;
                }

                try {
                    findAndProcessMatch(requester);
                } catch (MatchAlreadyProcessedException e) {
                    log.warn("매칭 경합 감지. 사용자 ID: {}", requester.getUserId());
                } catch (Exception e) {
                    log.error("매칭 처리 중 오류 발생. 사용자 ID: {}", requester.getUserId(), e);
                }
            }
        } catch (Exception e) {
            loopObservation.error(e);
            throw e;
        } finally {
            loopObservation.stop();
        }
    }

    /* 매칭 처리: 대기 시간에 따른 Phase로 파트너 조회 → 1회만 매칭 확정 */
    private void findAndProcessMatch(MatchQueue requester) {
        int phase = resolvePhase(requester);
        Observation attemptObservation = Observation.start("match.attempt", observationRegistry)
                .lowCardinalityKeyValue("phase", String.valueOf(phase));
        try (Observation.Scope scope = attemptObservation.openScope()) {
            Optional<MatchQueue> partner = findPartnerByPhase(requester, phase);
            if (partner.isPresent()) {
                matchService.confirmMatch(requester.getUserId(), partner.get().getUserId());
                attemptObservation.lowCardinalityKeyValue("result", "matched");
            } else {
                attemptObservation.lowCardinalityKeyValue("result", "none");
            }
        } catch (MatchAlreadyProcessedException e) {
            attemptObservation.lowCardinalityKeyValue("result", "conflict");
            attemptObservation.error(e);
            throw e;
        } catch (Exception e) {
            attemptObservation.lowCardinalityKeyValue("result", "error");
            attemptObservation.error(e);
            throw e;
        } finally {
            attemptObservation.stop();
        }
    }

    /** 대기 시간(초)에 따라 Phase 1~5 중 하나의 쿼리만 수행. 호출당 DB 1회. */
    private int resolvePhase(MatchQueue requester) {
        long s = ChronoUnit.SECONDS.between(requester.getCreatedAt(), LocalDateTime.now());

        if (s >= 30) {
            return 5;
        }
        if (s >= 20) {
            return 4;
        }
        if (s >= 10) {
            return 3;
        }
        if (s >= 5) {
            return 2;
        }
        return 1;
    }

    private Optional<MatchQueue> findPartnerByPhase(MatchQueue requester, int phase) {
        if (phase == 5) {
            return matchQueueMatchFinder.findPhase5Match(requester.getUserId(), WAITING_STATUS);
        }
        if (phase == 4) {
            return matchQueueMatchFinder.findPhase4Match(
                    requester.getUserId(), requester.getGender().name(), EXCLUDED_TIER, WAITING_STATUS);
        }
        if (phase == 3) {
            return matchQueueMatchFinder.findPhase3Match(
                    requester.getUserId(), requester.getGender().name(),
                    requester.getBirthYear() - AGE_TOLERANCE_YEARS, requester.getBirthYear() + AGE_TOLERANCE_YEARS,
                    EXCLUDED_TIER, WAITING_STATUS);
        }
        if (phase == 2) {
            return matchQueueMatchFinder.findPhase2Match(
                    requester.getUserId(), requester.getGender().name(), requester.getRegion().name(),
                    requester.getBirthYear() - AGE_TOLERANCE_YEARS, requester.getBirthYear() + AGE_TOLERANCE_YEARS,
                    EXCLUDED_TIER, WAITING_STATUS);
        }
        return matchQueueMatchFinder.findPhase1Match(
                requester.getUserId(), requester.getGender().name(), requester.getRegion().name(),
                requester.getBirthYear() - AGE_TOLERANCE_YEARS, requester.getBirthYear() + AGE_TOLERANCE_YEARS,
                requester.getHobbyIds(), EXCLUDED_TIER, WAITING_STATUS);
    }
}
