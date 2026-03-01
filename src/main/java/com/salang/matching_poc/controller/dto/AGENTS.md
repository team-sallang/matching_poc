<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-03-01 | Updated: 2026-03-01 -->

# controller/dto

## Purpose
REST API 요청·응답 DTO(Data Transfer Object) 클래스. 컨트롤러 레이어의 입출력 계약을 정의하며, JSON 직렬화/역직렬화 형식을 명시한다.

## Key Files

| File | Description |
|------|-------------|
| `MatchRequest.java` | 매칭 요청 DTO — `userId` (UUID) 필드, `@NotNull` 검증 |
| `MatchResponse.java` | 제네릭 매칭 응답 DTO — `status` + `data` 구조; 정적 팩토리 `matched()`, `waiting()` |
| `ErrorResponse.java` | 전역 에러 응답 DTO — HTTP 에러 시 일관된 응답 형식 |

## For AI Agents

### Working In This Directory
- `MatchResponse<T>`는 제네릭으로 waiting/matched 두 상태를 단일 타입으로 처리.
  - `matched()` → `MatchedData(roomId, matchedAt)` — JSON: `room_id`, `matched_at`
  - `waiting()` → `WaitingData(message, queuedAt)` — JSON: `message`, `queued_at`
- `@JsonInclude(NON_NULL)` — null 필드는 JSON 응답에서 제외.
- `@JsonProperty`로 Java camelCase → JSON snake_case 매핑.
- 새 응답 필드 추가 시 `docs/api_specification.md` 함께 업데이트.

### Common Patterns
- 정적 팩토리 메서드 패턴으로 DTO 생성 — 생성자 직접 노출 금지(`AccessLevel.PRIVATE`).
- 내부 정적 클래스(`MatchedData`, `WaitingData`)로 상태별 데이터 구조 분리.

## Dependencies

### Internal
- `constants/MatchingConstants.java` — `MATCHED_STATUS`, `WAITING_STATUS` 상수

<!-- MANUAL: -->
