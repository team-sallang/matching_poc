<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-03-01 | Updated: 2026-03-01 -->

# docs

## Purpose
프로젝트 설계 문서 모음. API 명세, 데이터 모델(ERD), 매칭 알고리즘 규칙, 동시성 테스트 계획 등 개발·기획 참고 자료를 포함한다.

## Key Files

| File | Description |
|------|-------------|
| `design_document.md` | 전체 시스템 설계 문서 (아키텍처, 컴포넌트, 데이터 흐름) |
| `api_specification.md` | REST API 엔드포인트 명세 (요청/응답 스키마) |
| `ERD.md` | 엔티티 관계 다이어그램 — User, MatchQueue, Room, Hobby, UserHobby |
| `matching_rule.md` | 매칭 알고리즘 단계별 규칙 (Phase 1~5 조건) |
| `user_score_rule.md` | 사용자 점수(Tier) 산정 규칙 |
| `category.md` | 취미/카테고리 분류 목록 |
| `concurrency_testing.md` | 동시성 테스트 설계 및 결과 기록 |
| `concurrency_testing_checklist.md` | 동시성 테스트 체크리스트 |

## For AI Agents

### Working In This Directory
- 문서 변경 시 연관 코드(매칭 로직, API)도 함께 업데이트했는지 확인.
- `matching_rule.md`의 Phase 조건이 `MatchScheduler`/`MatchQueueRepository`의 쿼리와 일치해야 함.
- `ERD.md`의 스키마는 `supabase/migrations/`의 실제 마이그레이션과 동기화되어야 함.

### Common Patterns
- 한국어로 작성된 문서가 다수. 일관성 유지를 위해 신규 문서도 한국어 작성.

<!-- MANUAL: -->
