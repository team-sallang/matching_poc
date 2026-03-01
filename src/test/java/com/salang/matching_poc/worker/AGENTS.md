<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-03-01 | Updated: 2026-03-01 -->

# test/worker

## Purpose
매칭 스케줄러 단위 테스트. Phase 결정 로직, 대기열 처리 흐름, 경쟁 조건 처리를 검증한다.

## Key Files

| File | Description |
|------|-------------|
| `MatchSchedulerTest.java` | `MatchScheduler` 테스트 — Phase 전환, 빈 큐 처리, `MatchAlreadyProcessedException` 처리 등 |

## For AI Agents

### Working In This Directory
- 스케줄러는 `@SpringBootTest` + `@MockBean`으로 의존성을 Mock하여 단위 테스트.
- 실제 스케줄 주기(1초)는 `@Scheduled` 비활성화 후 `runMatchingLoop()` 직접 호출로 테스트.
- Phase 결정(`resolvePhase`) 로직 변경 시 이 테스트의 타이밍 시나리오도 함께 업데이트.

<!-- MANUAL: -->
