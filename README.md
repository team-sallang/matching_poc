# matching_poc

소셜 매칭 서비스 동시성 검증 POC — Spring Boot 3.4 + PostgreSQL

---

## 개요

1000명 동시 매칭 요청에서 **중복 매칭 없이 완전한 1:1 매칭**이 가능한지 검증하는 POC입니다.

- **락 전략**: `FOR UPDATE SKIP LOCKED` (비관적 락)
- **검증 결과**: 중복 매칭 0건, 500쌍 완전 매칭, HTTP 실패율 0% ✅
- **성능**: 로컬 환경 기준 match_complete_ms p95 = **581ms** (1000 VU 동시 요청)

---

## 기술 스택

| 분류 | 기술 |
|------|------|
| Backend | Spring Boot 3.4.11, Java 17 |
| Database | PostgreSQL 17, Spring Data JPA |
| Connection Pool | HikariCP (max: 48) |
| 모니터링 | Micrometer + Prometheus + Grafana |
| 분산 트레이싱 | OpenTelemetry OTLP + Grafana Tempo |
| 로그 | Logback + Loki + Promtail |
| 부하 테스트 | k6 v0.34.1 |
| API 문서 | springdoc-openapi (Swagger UI) |

---

## 빠른 시작

### 사전 요구사항

- Java 17+
- Docker Desktop
- k6 (`choco install k6`)

### 1. 인프라 시작

```bash
# 모니터링 스택 + 로컬 PostgreSQL 시작
docker compose up -d
```

### 2. 환경 변수 설정

```bash
# .env.local 파일 생성 (gitignored)
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/postgres
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres
OTLP_ENDPOINT=http://localhost:14318/v1/traces
```

### 3. 애플리케이션 시작

```bash
# Windows PowerShell
$env:SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/postgres"
$env:SPRING_DATASOURCE_USERNAME="postgres"
$env:SPRING_DATASOURCE_PASSWORD="postgres"
$env:OTLP_ENDPOINT="http://localhost:14318/v1/traces"
./gradlew bootRun

# bash
export $(cat .env.local | grep -v '^#' | xargs) && ./gradlew bootRun
```

### 4. 동시성 테스트 실행

```bash
# CSV 데이터 준비 (최초 1회)
# 로컬 DB에서 users 테이블 export → k6/users_rows.csv

k6 run k6/concurrent_match_test.js
```

---

## 프로젝트 구조

```
matching_poc/
├── src/
│   ├── main/java/com/salang/matching_poc/
│   │   ├── controller/        # MatchController (POST /match, GET /match/status)
│   │   ├── service/           # MatchService, MatchQueueMatchFinder
│   │   ├── worker/            # MatchScheduler (1초 주기)
│   │   ├── model/             # Entity (User, MatchQueue, Room, ...)
│   │   ├── repository/        # JPA Repository (native query)
│   │   └── exception/         # 커스텀 예외
│   └── main/resources/
│       └── application.yaml
├── k6/
│   ├── concurrent_match_test.js  # 1000 VU 동시 매칭 테스트
│   └── README.md
├── postgres/
│   └── init/                  # 로컬 DB 초기화 SQL (스키마 + 시드)
├── grafana/
│   ├── dashboards/            # Matching POC 대시보드 JSON
│   └── provisioning/
├── docs/
│   ├── design/                # 설계 문서 (ERD, API 명세, 도메인 규칙)
│   └── poc/                   # POC 결과 (완료 보고서, 테스트 결과, 도입 가이드)
├── supabase/
│   └── migrations/            # DB 마이그레이션 SQL
└── compose.yml                # Prometheus, Grafana, Loki, Tempo, PostgreSQL
```

---

## API

| Method | URL | 설명 |
|--------|-----|------|
| POST | `/api/v1/match` | 매칭 요청 (200: 즉시 매칭, 202: 대기열 등록) |
| GET | `/api/v1/match/status?user_id={id}` | 매칭 상태 조회 |

Swagger UI: `http://localhost:8080/swagger-ui.html`

---

## 매칭 알고리즘

```
MatchController → MatchService → MatchQueueMatchFinder (interface)
                                      ↓ (PostgreSQL impl)
                              MatchQueueRepository (FOR UPDATE SKIP LOCKED)
MatchScheduler (fixedDelay 1s) → MatchService.confirmMatch()
```

**단계적 매칭 조건 완화** (대기 시간 기준):

| 단계 | 대기 시간 | 조건 |
|------|-----------|------|
| Phase 1 | 0~5s | 성별·지역·나이·취미 모두 일치 |
| Phase 2 | 5~10s | 나이 조건 완화 |
| Phase 3 | 10~20s | 취미 조건 완화 |
| Phase 4 | 20~30s | 지역 조건 완화 |
| Phase 5 | 30s+ | 성별만 일치 |

---

## 모니터링

| 서비스 | URL | 계정 |
|--------|-----|------|
| Grafana | http://localhost:3000 | admin / admin |
| Prometheus | http://localhost:9090 | — |
| Swagger UI | http://localhost:8080/swagger-ui.html | — |

**커스텀 메트릭:**

| 메트릭 | 타입 | 설명 |
|--------|------|------|
| `match.request.immediate` | Counter | 즉시 매칭 성공 수 |
| `match.request.enqueued` | Counter | 대기열 등록 수 |
| `match.confirm.success` | Counter | 스케줄러 매칭 확정 수 |
| `match.confirm.conflict` | Counter | 동시성 충돌(재시도) 수 |
| `match.queue.waiting.count` | Gauge | 현재 대기 중인 유저 수 |

---

## 테스트

```bash
./gradlew test          # 단위/통합 테스트 (H2 인메모리)
./gradlew compileJava   # 컴파일 검증
```

> PostgreSQL 전용 쿼리(`FOR UPDATE SKIP LOCKED`)는 H2에서 동작하지 않습니다.
> `MatchQueueMatchFinderH2` (test 패키지)가 테스트 전용 폴백 구현체로 동작합니다.

---

## 문서

| 문서 | 경로 |
|------|------|
| POC 완료 보고서 | [docs/poc/poc_completion_report.md](docs/poc/poc_completion_report.md) |
| 부하 테스트 결과 | [docs/poc/load_test_results.md](docs/poc/load_test_results.md) |
| 실 서비스 도입 가이드 | [docs/poc/adoption_guide.md](docs/poc/adoption_guide.md) |
| 동시성 테스트 체크리스트 | [docs/poc/concurrency_testing_checklist.md](docs/poc/concurrency_testing_checklist.md) |
| ERD | [docs/design/ERD.md](docs/design/ERD.md) |
| API 명세 | [docs/design/api_specification.md](docs/design/api_specification.md) |
| 매칭 도메인 구조 | [docs/design/matching_structure.md](docs/design/matching_structure.md) |
