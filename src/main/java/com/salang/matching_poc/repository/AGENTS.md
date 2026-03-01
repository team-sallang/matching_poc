<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-03-01 | Updated: 2026-03-01 -->

# repository

## Purpose
Spring Data JPA 리포지토리 인터페이스. 기본 CRUD 외에 매칭 단계별 네이티브 쿼리와 원자적 상태 업데이트 쿼리를 포함한다.

## Key Files

| File | Description |
|------|-------------|
| `MatchQueueRepository.java` | 핵심 리포지토리 — Phase 1~5 네이티브 쿼리(`for update skip locked`), 원자적 상태 업데이트(`updateStatusIf`) |
| `UserRepository.java` | 사용자 조회 리포지토리 |
| `RoomRepository.java` | 채팅방 조회 — `findFirstByUser1IdOrUser2Id` |
| `UserHobbyRepository.java` | 사용자-취미 연결 조회 |
| `HobbyRepository.java` | 취미 목록 조회 |

## For AI Agents

### Working In This Directory
- `MatchQueueRepository`의 네이티브 쿼리는 PostgreSQL 전용 (`integer[]` 배열 연산 `&&`, `for update skip locked`).
- H2에서는 이 쿼리들이 동작하지 않으므로 해당 메서드 테스트는 Mock 또는 실제 PostgreSQL 환경에서 수행.
- `updateStatusIf` — `WAITING` 상태인 2명을 `MATCHED`로 원자적 업데이트. 반환값이 2가 아니면 경쟁 조건 발생으로 `MatchAlreadyProcessedException` 던짐.
- 새 Phase 조건 추가 시 `MatchQueueMatchFinder` 인터페이스와 구현체(`MatchQueueMatchFinderPostgres`)도 함께 수정.

### Common Patterns
- Phase 쿼리 패턴: `status`, `user_id <>`, `gender <>`, `tier <>` 조건 공통 + Phase마다 조건 완화.
  - Phase 1: 성별 반대 + 지역 동일 + 나이 범위 + 취미 겹침
  - Phase 2: 성별 반대 + 지역 동일 + 나이 범위
  - Phase 3: 성별 반대 + 나이 범위
  - Phase 4: 성별 반대
  - Phase 5: 조건 없음 (아무나)
- `order by created_at limit 1` — 가장 오래 기다린 사용자 우선 매칭.
- `@Lock(LockModeType.PESSIMISTIC_WRITE)` — JPQL 비관적 잠금.

## Dependencies

### Internal
- `model/entity/MatchQueue.java`
- `model/enums/MatchStatus.java`
- `service/MatchQueueMatchFinder.java` — 리포지토리 메서드를 인터페이스로 위임

### External
- PostgreSQL — 네이티브 쿼리 배열 연산 및 잠금 기능

<!-- MANUAL: -->
