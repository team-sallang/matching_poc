<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-03-01 | Updated: 2026-03-01 -->

# supabase

## Purpose
Supabase 프로젝트 설정 및 데이터베이스 마이그레이션. 로컬 개발 환경에서 `supabase start`로 PostgreSQL 인스턴스를 기동하며, 마이그레이션으로 스키마를 버전 관리한다.

## Key Files

| File | Description |
|------|-------------|
| `config.toml` | Supabase 프로젝트 설정 (로컬 포트, Auth, Storage 등) |
| `seed.sql` | 초기 테스트 데이터 — 사용자, 취미, 매칭 큐 시드 데이터 |
| `.gitignore` | Supabase 임시 파일 제외 설정 |

## Subdirectories

| Directory | Purpose |
|-----------|---------|
| `migrations/` | 순차 SQL 마이그레이션 파일 (스키마 변경 이력) |

## Key Migration Files

| File | Description |
|------|-------------|
| `migrations/20260122154230_create_core_tables.sql` | 핵심 테이블 생성 — users, hobbies, user_hobbies, match_queue, rooms |
| `migrations/20260123100000_drop_flyway_schema_history.sql` | Flyway 스키마 히스토리 테이블 제거 (Supabase 마이그레이션으로 전환) |

## For AI Agents

### Working In This Directory
- 스키마 변경 시 반드시 새 마이그레이션 파일 생성 (기존 파일 수정 금지).
- 파일명 형식: `YYYYMMDDHHMMSS_description.sql`
- `supabase db reset`으로 로컬 DB를 마이그레이션 + 시드 기준으로 초기화.
- Spring 앱의 `ddl-auto: validate` 설정으로 인해 엔티티와 스키마 불일치 시 앱 기동 실패.

### Testing Requirements
- 마이그레이션 적용 후 `supabase db diff`로 스키마 드리프트 확인.
- `seed.sql` 변경 시 k6 테스트의 `users_rows.csv`도 업데이트 필요.

### Common Patterns
- PostgreSQL `integer[]` 배열 타입 — `hobby_ids` 컬럼 (Hibernate `@JdbcTypeCode(SqlTypes.ARRAY)`)
- `for update skip locked` — 동시 매칭 경쟁 조건 방지용 비관적 잠금

## Dependencies

### External
- Supabase CLI
- PostgreSQL 15+

<!-- MANUAL: -->
