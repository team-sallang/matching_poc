<!-- Generated: 2026-03-01 | Updated: 2026-03-01 -->

# matching_poc

## Purpose
Spring Boot 3 기반의 소셜 매칭 서비스 POC(Proof of Concept). 사용자를 성별·지역·나이·취미 조건으로 단계적으로 매칭하고, 채팅방을 생성하는 백엔드 API 서버. PostgreSQL(Supabase) 기반 매칭 큐와 스케줄러를 통해 동시성 안전 매칭을 구현한다.

## Key Files

| File | Description |
|------|-------------|
| `build.gradle` | Gradle 빌드 정의 — Spring Boot 3.4, JPA, PostgreSQL, Micrometer, OpenTelemetry, Swagger, Lombok |
| `settings.gradle` | 프로젝트 이름(`matching_poc`) 설정 |
| `compose.yml` | 로컬 개발용 Docker Compose — PostgreSQL, Grafana, Prometheus, Loki, Tempo, Promtail |
| `prometheus.yml` | Prometheus 스크레이프 설정 (actuator `/prometheus` 엔드포인트) |
| `promtail-config.yml` | Promtail 로그 수집 설정 (Loki로 전달) |
| `loki-config.yml` | Loki 로그 집계 서버 설정 |
| `tempo-config.yml` | Tempo 분산 트레이싱 백엔드 설정 |
| `README.md` | 프로젝트 소개 및 설계 문서 링크 |
| `HELP.md` | Spring Initializr 기본 도움말 |
| `.env` | 로컬 환경변수 (DB URL, 자격증명 등 — git 추적 제외) |

## Subdirectories

| Directory | Purpose |
|-----------|---------|
| `src/` | 전체 애플리케이션 소스 코드 (see `src/AGENTS.md`) |
| `docs/` | 설계 문서, API 명세, ERD, 매칭 규칙 (see `docs/AGENTS.md`) |
| `k6/` | k6 부하 테스트 스크립트 (see `k6/AGENTS.md`) |
| `grafana/` | Grafana 대시보드 및 프로비저닝 설정 (see `grafana/AGENTS.md`) |
| `supabase/` | DB 마이그레이션, 시드 데이터, Supabase 설정 (see `supabase/AGENTS.md`) |
| `gradle/` | Gradle 래퍼 (gradlew, gradle-wrapper.jar) |
| `logs/` | 런타임 애플리케이션 로그 (git 추적 제외) |

## For AI Agents

### Working In This Directory
- Java 17 + Spring Boot 3.4 프로젝트. Lombok을 사용하므로 어노테이션 기반 생성 코드를 직접 작성하지 말 것.
- `build.gradle` 수정 후 반드시 Gradle 빌드를 실행하여 의존성 충돌 여부 확인.
- `.env` 파일은 민감 정보 포함 — 절대 커밋하지 말 것.
- DDL 변경은 `supabase/migrations/`에 마이그레이션 파일로 추가, `spring.jpa.hibernate.ddl-auto: validate` 설정이므로 스키마 불일치 시 앱이 기동되지 않음.

### Testing Requirements
- `./gradlew test` — H2 인메모리 DB(`src/test/resources/application-test.yaml`)로 단위/통합 테스트 실행.
- 실제 PostgreSQL 연동 테스트는 Supabase 로컬 인스턴스 필요 (`supabase start`).
- k6 부하 테스트는 `k6/run_test.ps1` 또는 `k6 run k6/concurrent_match_test.js` 실행.

### Common Patterns
- 모든 서비스 레이어에 `@Transactional` 사용. 조회 전용은 `@Transactional(readOnly = true)`.
- Micrometer `MeterRegistry`로 커스텀 메트릭 카운터/게이지 등록.
- `for update skip locked` 네이티브 쿼리로 동시성 충돌 없이 매칭 파트너 선점.
- OpenTelemetry + Tempo로 분산 트레이싱, Promtail + Loki로 로그 집계, Prometheus + Grafana로 메트릭 시각화.

## Dependencies

### Internal
- `src/main/` — 애플리케이션 코어
- `supabase/migrations/` — DB 스키마 정의

### External
- Spring Boot 3.4.11
- Spring Data JPA + PostgreSQL 드라이버
- Micrometer (Prometheus, OpenTelemetry OTLP)
- springdoc-openapi 2.8.8 (Swagger UI)
- Lombok
- Logstash Logback Encoder 7.4

<!-- MANUAL: -->
