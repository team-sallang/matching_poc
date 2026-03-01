<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-03-01 | Updated: 2026-03-01 -->

# model/enums

## Purpose
도메인 열거형 정의. 매칭 조건 필드의 허용 값 집합을 타입 안전하게 표현한다.

## Key Files

| File | Description |
|------|-------------|
| `MatchStatus.java` | 매칭 큐 상태 — `WAITING`(대기중), `MATCHED`(매칭됨) |
| `Gender.java` | 성별 — 매칭 시 반대 성별 파트너 탐색에 사용 |
| `Region.java` | 지역 — Phase 1·2 동일 지역 조건에 사용 |
| `Tier.java` | 사용자 등급 — `SPROUT`(새싹) 등; Phase 1~4에서 특정 Tier 제외 조건 |

## For AI Agents

### Working In This Directory
- 열거형 값 추가 시 DB 컬럼(문자열 저장)과의 호환성 확인 — 기존 데이터 마이그레이션 고려.
- `MatchStatus` 값은 `MatchingConstants.WAITING_STATUS`와 동기화 필수 (`MatchStatus.WAITING.name()` 일치).
- `Tier`의 `EXCLUDED_TIER` 값은 `MatchingConstants.EXCLUDED_TIER`에서 네이티브 쿼리 파라미터로 사용.
- 새 열거형 추가 시 매칭 Phase 쿼리에 해당 조건 반영 여부 검토.

### Common Patterns
- `@EnumType.STRING` — DB에 이름(name())으로 저장, 순서 변경에 안전.
- 일부 열거형에 `description` 필드(한국어 설명) 포함 — Lombok `@Getter`, `@RequiredArgsConstructor`.

## Dependencies

### Internal
- `constants/MatchingConstants.java` — EXCLUDED_TIER, WAITING_STATUS와 값 동기화
- `repository/MatchQueueRepository.java` — 네이티브 쿼리에서 `.name()` 문자열로 사용

<!-- MANUAL: -->
