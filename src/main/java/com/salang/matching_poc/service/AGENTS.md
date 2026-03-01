<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-03-01 | Updated: 2026-03-01 -->

# service

## Purpose
핵심 비즈니스 로직 레이어. 매칭 요청 처리(즉시 매칭 또는 대기열 등록), 매칭 확정(원자적 상태 전환), 취소, 상태 조회를 담당한다. 단계별 매칭 파트너 탐색 전략(`MatchQueueMatchFinder`)을 추상화한다.

## Key Files

| File | Description |
|------|-------------|
| `MatchService.java` | 주 서비스 — `requestMatch`, `confirmMatch`, `cancelMatch`, `getMatchStatus` 구현 |
| `MatchQueueMatchFinder.java` | 매칭 파트너 탐색 전략 인터페이스 — Phase 1~5 메서드 정의 |
| `MatchQueueMatchFinderPostgres.java` | `MatchQueueMatchFinder` PostgreSQL 구현체 — `MatchQueueRepository`의 네이티브 쿼리 위임 |

## For AI Agents

### Working In This Directory
- `MatchService.requestMatch()` — 트랜잭션 내에서 즉시 매칭(인터셉트) 또는 대기열 등록을 결정.
- `MatchService.confirmMatch()` — `updateStatusIf()` 반환값이 2가 아니면 `MatchAlreadyProcessedException` 발생 → 트랜잭션 롤백. 경쟁 조건 안전 처리.
- `MatchQueueMatchFinder`는 인터페이스이므로 테스트 환경(H2)을 위한 별도 구현체 작성 가능 (`MatchQueueMatchFinderH2`가 테스트에 존재).
- Micrometer 카운터: `match.request.immediate`, `match.request.enqueued`, `match.confirm.success`, `match.confirm.conflict`.

### Testing Requirements
- `MatchServiceTest.java` — `@SpringBootTest` + H2.
- `MatchQueueMatchFinderH2.java` — H2용 폴백 구현체 (테스트 패키지에 위치).

### Common Patterns
- 인터셉트 매칭: `requestMatch` 진입 시 즉시 파트너 탐색 → 있으면 Room 생성 + 파트너 MATCHED.
- 대기열 등록: 파트너 없으면 `MatchQueue` 저장 → 스케줄러가 이후 처리.
- `@Transactional(readOnly = true)` — `getMatchStatus` 조회 전용.

## Dependencies

### Internal
- `repository/` — 모든 DB 접근
- `model/entity/` — MatchQueue, Room, User, UserHobby
- `exception/` — 비즈니스 예외
- `constants/MatchingConstants.java` — 알고리즘 파라미터

### External
- Micrometer `MeterRegistry` — 비즈니스 메트릭

<!-- MANUAL: -->
