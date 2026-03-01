<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-03-01 | Updated: 2026-03-01 -->

# test/controller

## Purpose
컨트롤러 슬라이스 테스트. `@WebMvcTest`로 웹 레이어만 로드하고 MockMvc로 HTTP 요청/응답을 검증한다.

## Key Files

| File | Description |
|------|-------------|
| `MatchControllerWebMvcTest.java` | `MatchController` 슬라이스 테스트 — 요청 유효성, HTTP 상태 코드, 응답 JSON 구조 검증 |

## For AI Agents

### Working In This Directory
- `@WebMvcTest(MatchController.class)` + `@MockBean(MatchService.class)` 패턴 사용.
- 실제 DB 없이 MockMvc로 컨트롤러 로직만 검증.
- 새 엔드포인트 추가 시 이 디렉토리에 대응 테스트 케이스 추가.

<!-- MANUAL: -->
