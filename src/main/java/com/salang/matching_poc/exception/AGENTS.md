<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-03-01 | Updated: 2026-03-01 -->

# exception

## Purpose
도메인 예외 클래스와 전역 예외 핸들러. 비즈니스 규칙 위반을 명확한 예외로 표현하고 일관된 HTTP 에러 응답을 반환한다.

## Key Files

| File | Description |
|------|-------------|
| `GlobalExceptionHandler.java` | `@RestControllerAdvice` 전역 핸들러 — 각 예외를 HTTP 상태 코드와 `ErrorResponse`로 변환 |
| `AlreadyInQueueException.java` | 이미 매칭 대기열에 있는 사용자가 재요청할 때 발생 (HTTP 409) |
| `UserNotFoundException.java` | 존재하지 않는 사용자 ID로 요청 시 발생 (HTTP 404) |
| `UserNotInQueueException.java` | 대기열에 없는 사용자가 취소/상태 조회 시 발생 (HTTP 404) |
| `MatchAlreadyProcessedException.java` | 동시 매칭 경쟁에서 이미 다른 스레드가 매칭을 완료한 경우 발생 — 롤백 트리거 |

## For AI Agents

### Working In This Directory
- 새 비즈니스 예외 추가 시: 예외 클래스 생성 → `GlobalExceptionHandler`에 `@ExceptionHandler` 메서드 추가 → `docs/api_specification.md` 에러 코드 업데이트.
- `MatchAlreadyProcessedException`은 `@Transactional` 롤백을 유발해야 하므로 `RuntimeException` 상속 필수.
- `ErrorResponse`는 `controller/dto/ErrorResponse.java`에 정의.

### Common Patterns
- 예외 클래스: `RuntimeException` 상속, 기본 메시지 생성자 + 커스텀 메시지 생성자.
- 동시성 충돌은 예외로 처리하여 `MatchScheduler`에서 경고 로그만 남기고 다음 사이클로 진행.

## Dependencies

### Internal
- `controller/dto/ErrorResponse.java` — 에러 응답 DTO

<!-- MANUAL: -->
