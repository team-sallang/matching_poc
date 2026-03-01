<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-03-01 | Updated: 2026-03-01 -->

# src

## Purpose
Maven/Gradle 표준 소스 루트. 프로덕션 코드(`main`)와 테스트 코드(`test`)를 분리하여 관리한다.

## Subdirectories

| Directory | Purpose |
|-----------|---------|
| `main/` | 프로덕션 애플리케이션 소스 및 리소스 (see `main/AGENTS.md`) |
| `test/` | JUnit 테스트 소스 및 테스트 전용 리소스 (see `test/AGENTS.md`) |

## For AI Agents

### Working In This Directory
- 이 디렉토리는 컨테이너 역할만 하므로 직접 파일을 추가하지 말 것.
- 신규 클래스는 반드시 `main/java/` 하위 적절한 패키지에 배치.
- 테스트 클래스는 `test/java/` 하위에 동일 패키지 구조로 배치.

<!-- MANUAL: -->
