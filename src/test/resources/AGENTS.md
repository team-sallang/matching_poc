<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-03-01 | Updated: 2026-03-01 -->

# src/test/resources

## Purpose
테스트 전용 설정 디렉토리. H2 인메모리 DB를 사용하는 테스트 프로파일 설정을 포함한다.

## Key Files

| File | Description |
|------|-------------|
| `application-test.yaml` | 테스트 프로파일 설정 — H2 인메모리 DB, DDL auto-create, 불필요한 외부 의존성 비활성화 |

## For AI Agents

### Working In This Directory
- H2는 PostgreSQL 배열 타입(`integer[]`)을 지원하지 않으므로 배열 관련 네이티브 쿼리 테스트는 별도 처리 필요.
- 테스트에서 PostgreSQL 특정 기능(네이티브 쿼리, `for update skip locked`)을 사용하는 경우 실제 DB 환경에서 별도 검증.

<!-- MANUAL: -->
