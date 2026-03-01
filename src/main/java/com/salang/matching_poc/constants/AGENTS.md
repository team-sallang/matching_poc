<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-03-01 | Updated: 2026-03-01 -->

# constants

## Purpose
매칭 알고리즘 전체에서 공유하는 전역 상수 정의. 매직 리터럴을 제거하고 단일 수정 지점을 제공한다.

## Key Files

| File | Description |
|------|-------------|
| `MatchingConstants.java` | 매칭 관련 상수 — 나이 허용 범위(`AGE_TOLERANCE_YEARS`), 제외 티어(`EXCLUDED_TIER`), 대기 상태 문자열(`WAITING_STATUS`), 서울 타임존(`ZONE_ASIA_SEOUL`) |

## For AI Agents

### Working In This Directory
- 매칭 조건(나이 허용 범위, 제외 티어 등)을 변경할 때 이 파일만 수정하면 전체 서비스·스케줄러·리포지토리에 자동 반영.
- 새 상수 추가 시 `MatchingConstants.java`에 `public static final`로 추가.
- `WAITING_STATUS`는 네이티브 쿼리의 `status` 파라미터로 전달되므로 `MatchStatus.WAITING.name()`과 값이 일치해야 함.

## Dependencies

### Internal
- `model/enums/MatchStatus.java` — 상수값이 열거형과 동기화되어야 함
- `model/enums/Tier.java` — `EXCLUDED_TIER` 값 참조

<!-- MANUAL: -->
