<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-03-01 | Updated: 2026-03-01 -->

# controller

## Purpose
HTTP REST API 레이어. 매칭 요청·취소·상태 조회 엔드포인트를 노출하며 요청 유효성 검사 후 서비스 레이어에 위임한다.

## Key Files

| File | Description |
|------|-------------|
| `MatchController.java` | `/api/v1/match` REST 컨트롤러 — POST(매칭 요청), DELETE(취소), GET `/status`(상태 조회) |

## Subdirectories

| Directory | Purpose |
|-----------|---------|
| `dto/` | 요청/응답 DTO 클래스 (see `dto/AGENTS.md`) |

## For AI Agents

### Working In This Directory
- `@Valid` 어노테이션으로 요청 본문 검증 — DTO의 제약 어노테이션이 실제 검증에 사용됨.
- 매칭 대기 응답은 HTTP 202 Accepted, 즉시 매칭은 HTTP 200 OK.
- 새 엔드포인트 추가 시 `docs/api_specification.md` 업데이트 필요.
- Swagger UI: `/swagger-ui.html` (로컬 기동 시)

### Testing Requirements
- `@WebMvcTest(MatchController.class)` + `@MockBean(MatchService.class)` 조합 사용.
- `src/test/java/com/salang/matching_poc/controller/MatchControllerWebMvcTest.java` 참고.

### Common Patterns
- `MatchResponse<?>` 제네릭 응답으로 waiting/matched 상태를 단일 타입으로 처리.
- `@RequestParam("user_id")` — 케밥케이스 쿼리 파라미터.

## Dependencies

### Internal
- `service/MatchService.java` — 비즈니스 로직 위임
- `controller/dto/` — 요청·응답 DTO
- `constants/MatchingConstants.java` — 상태 문자열 비교

<!-- MANUAL: -->
