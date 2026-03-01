<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-03-01 | Updated: 2026-03-01 -->

# worker

## Purpose
매칭 스케줄러. 1초 주기로 대기 중인 사용자 목록을 조회하고, 대기 시간에 따라 Phase 1→5로 조건을 완화하며 파트너를 탐색한다. 동시성 경쟁 조건을 감지하고 안전하게 처리한다.

## Key Files

| File | Description |
|------|-------------|
| `MatchScheduler.java` | `@Scheduled(fixedDelay)` 스케줄러 — 대기 큐 조회 → Phase 결정 → 파트너 탐색 → `confirmMatch` 호출 |

## For AI Agents

### Working In This Directory
- 스케줄러는 **단일 스레드**(`spring.task.scheduling.pool.size: 1`)로 실행 — `fixedDelay`로 이전 실행 완료 후 1초 대기.
- `runMatchingLoop()`에는 `@Transactional`이 없음 — 각 `confirmMatch()` 호출이 독립 트랜잭션.
- `MatchAlreadyProcessedException` 발생 시 경고 로그 후 다음 사용자로 진행 (정상 흐름).
- Phase 결정 로직(`resolvePhase`): 대기 5초 미만→Phase 1, 5초→2, 10초→3, 20초→4, 30초→5.
- `waitingQueueGauge` — `@PostConstruct`로 Micrometer 게이지 등록 (`match.queue.waiting.count`).
- Micrometer Observation API로 `match.scheduler.loop` 및 `match.attempt` 스팬 생성 (Tempo 트레이싱).

### Testing Requirements
- `MatchSchedulerTest.java` — 스케줄러 동작 단위 테스트.
- 동시성 테스트는 `k6/concurrent_match_test.js` 부하 테스트로 검증.

### Common Patterns
- `Collections.shuffle(waitingUsers)` — 매 사이클마다 처리 순서 무작위화, 특정 사용자 편중 방지.
- 루프 내 `requester.getStatus() != WAITING` 체크 — 동일 사이클 내 이미 매칭된 경우 스킵.

## Dependencies

### Internal
- `service/MatchService.java` — `confirmMatch()` 호출
- `service/MatchQueueMatchFinder.java` — 파트너 탐색
- `repository/MatchQueueRepository.java` — 대기열 조회
- `constants/MatchingConstants.java` — Phase 쿼리 파라미터

### External
- Micrometer `MeterRegistry`, `ObservationRegistry` — 메트릭 및 트레이싱

<!-- MANUAL: -->
