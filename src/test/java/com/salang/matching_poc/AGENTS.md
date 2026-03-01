<!-- Parent: ../../../../../../AGENTS.md -->
<!-- Generated: 2026-03-01 | Updated: 2026-03-01 -->

# com.salang.matching_poc (Test Package)

## Purpose
프로덕션 패키지와 동일한 구조의 테스트 루트 패키지. 컨트롤러·서비스·리포지토리·워커 슬라이스 테스트 및 통합 테스트를 포함한다.

## Key Files

| File | Description |
|------|-------------|
| `MatchingPocApplicationTests.java` | Spring Boot 컨텍스트 로드 통합 테스트 |

## Subdirectories

| Directory | Purpose |
|-----------|---------|
| `controller/` | `@WebMvcTest` 기반 컨트롤러 슬라이스 테스트 |
| `repository/` | `@DataJpaTest` 기반 리포지토리 테스트 |
| `service/` | 서비스 단위·통합 테스트 |
| `worker/` | 스케줄러 동작 테스트 |

## For AI Agents

### Working In This Directory
- 모든 테스트는 `application-test` 프로파일(H2 인메모리)로 실행.
- PostgreSQL 특정 기능(배열 연산, `for update skip locked`)은 H2에서 동작하지 않을 수 있으므로 해당 쿼리 테스트는 Mock 처리 또는 실제 DB 환경에서 별도 검증.
- 테스트 클래스명은 대상 클래스명 + `Test` 접미사 규칙.

### Common Patterns
- `@MockBean`으로 외부 의존성 Mock.
- `MockMvc`로 HTTP 레이어 테스트.
- `@Transactional`로 테스트 후 DB 롤백.

<!-- MANUAL: -->
