<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-03-01 | Updated: 2026-03-01 -->

# test/repository

## Purpose
리포지토리 슬라이스 테스트. `@DataJpaTest`로 JPA 레이어만 로드하고 H2 인메모리 DB로 쿼리 동작을 검증한다.

## Key Files

| File | Description |
|------|-------------|
| `MatchQueueRepositoryTest.java` | `MatchQueueRepository` 테스트 — CRUD 및 상태 조회 검증 (H2 호환 쿼리만) |

## For AI Agents

### Working In This Directory
- H2에서 동작하지 않는 PostgreSQL 네이티브 쿼리(`for update skip locked`, `integer[]` 배열 `&&` 연산)는 이 테스트에서 검증 불가.
- PostgreSQL 전용 쿼리 테스트는 실제 DB 환경(Supabase 로컬)에서 별도 수행.
- `findByStatus`, `findByUserId`, `updateStatusIf` 등 H2 호환 쿼리는 여기서 검증 가능.

<!-- MANUAL: -->
