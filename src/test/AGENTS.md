<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-03-01 | Updated: 2026-03-01 -->

# src/test

## Purpose
JUnit 테스트 소스 루트. 프로덕션 코드와 동일한 패키지 구조를 유지하며 단위 테스트 및 슬라이스 테스트를 포함한다.

## Subdirectories

| Directory | Purpose |
|-----------|---------|
| `java/` | 테스트 Java 소스 코드 |
| `resources/` | 테스트 전용 설정 (`application-test.yaml` — H2 인메모리 DB) |

## For AI Agents

### Working In This Directory
- 테스트는 H2 인메모리 DB를 사용 (`spring.profiles.active=test`).
- `@WebMvcTest` — 컨트롤러 슬라이스 테스트.
- `@DataJpaTest` — 리포지토리 슬라이스 테스트.
- `@SpringBootTest` — 통합 테스트 (H2로 전체 컨텍스트 기동).

<!-- MANUAL: -->
