<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-03-01 | Updated: 2026-03-01 -->

# model

## Purpose
JPA 엔티티와 열거형(enum) 정의. 데이터베이스 스키마를 Java 객체로 매핑하며, 도메인 모델의 핵심을 구성한다.

## Subdirectories

| Directory | Purpose |
|-----------|---------|
| `entity/` | JPA `@Entity` 클래스 — DB 테이블 매핑 (see `entity/AGENTS.md`) |
| `enums/` | 도메인 열거형 — Gender, Region, Tier, MatchStatus (see `enums/AGENTS.md`) |

## For AI Agents

### Working In This Directory
- 엔티티 변경(컬럼 추가/삭제/타입 변경) 시 반드시 `supabase/migrations/`에 마이그레이션 SQL 추가.
- `ddl-auto: validate`이므로 마이그레이션 없이 엔티티만 변경하면 앱 기동 실패.
- 열거형 값 추가 시 DB의 enum 타입 또는 string 컬럼과 일치 확인.

## Dependencies

### Internal
- `supabase/migrations/` — 실제 DB 스키마와 동기화 필수

<!-- MANUAL: -->
