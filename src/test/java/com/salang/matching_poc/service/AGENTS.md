<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-03-01 | Updated: 2026-03-01 -->

# test/service

## Purpose
서비스 레이어 테스트. `MatchService`의 비즈니스 로직과 동시성 시나리오를 H2 기반으로 검증한다.

## Key Files

| File | Description |
|------|-------------|
| `MatchServiceTest.java` | `MatchService` 통합 테스트 — 매칭 요청, 취소, 상태 조회, 경쟁 조건 시나리오 |
| `MatchQueueMatchFinderH2.java` | H2 호환 `MatchQueueMatchFinder` 구현체 — PostgreSQL 전용 쿼리 대신 H2에서 동작하는 폴백 로직 |

## For AI Agents

### Working In This Directory
- `MatchQueueMatchFinderH2`는 테스트 전용 구현체. PostgreSQL 네이티브 쿼리 대신 JPQL/메서드 쿼리로 동일 동작 근사.
- 실제 동시성 안전성은 k6 부하 테스트(`k6/concurrent_match_test.js`)로만 완전 검증 가능.
- `confirmMatch` 경쟁 조건 테스트 시 멀티스레드 `ExecutorService` 활용 패턴 사용.

<!-- MANUAL: -->
