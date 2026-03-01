<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-03-01 | Updated: 2026-03-01 -->

# src/main/resources

## Purpose
프로덕션 애플리케이션 설정 파일 디렉토리. Spring Boot 설정(YAML)과 Logback 로깅 설정을 포함한다.

## Key Files

| File | Description |
|------|-------------|
| `application.yaml` | 주 설정 파일 — DataSource(HikariCP), JPA, 스케줄러, Actuator/Prometheus, OTLP 트레이싱, 로깅 수준 |
| `logback-spring.xml` | Logback 설정 — 콘솔 출력과 Logstash JSON 인코더(Promtail 연동)를 함께 설정 |

## For AI Agents

### Working In This Directory
- DB 접속 정보는 환경변수(`${SPRING_DATASOURCE_URL}` 등)로 외부화 — 직접 값을 작성하지 말 것.
- `matching.schedule.fixed-delay` 변경 시 스케줄러 동작과 메트릭에 영향.
- `spring.task.scheduling.pool.size: 1` 고정 — 스케줄러 중복 실행 방지. 변경 전 동시성 영향 분석 필수.
- `ddl-auto: validate` — 엔티티 변경 후 반드시 마이그레이션 파일 추가.
- OTLP 엔드포인트 기본값: `http://localhost:4318/v1/traces` (로컬 Tempo).

### Common Patterns
- 환경별 설정 오버라이드: `application-{profile}.yaml` 패턴 사용.
- 테스트 환경은 `src/test/resources/application-test.yaml`에서 별도 관리.

## Dependencies

### External
- Logstash Logback Encoder 7.4 — JSON 로그 포맷
- Micrometer / OpenTelemetry — 메트릭·트레이싱 엔드포인트

<!-- MANUAL: -->
