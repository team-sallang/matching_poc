# POC 완성 로드맵

## 목표
Spring Boot 3.4 + PostgreSQL 기반 소셜 매칭 서비스 POC의 동시성 안전성을 검증하고,
부하 테스트 결과를 기록한 뒤, 실 서비스 도입 준비를 완료한다.

---

## 현재 상태 (검증 결과 기록)

> 2026-03-02 업데이트

| 항목 | 상태 | 비고 |
|------|------|------|
| `./gradlew test` | ✅ BUILD SUCCESSFUL | 수정 불필요 |
| Docker API 버전 | ✅ v29.1.3 / API v1.52 | 업그레이드 불필요 |
| Docker Compose 스택 기동 | ✅ 전체 Running/Healthy | Grafana·Prometheus·Loki·Tempo |
| `./gradlew compileJava` | ✅ BUILD SUCCESSFUL | 수정 불필요 |
| Phase A 동시성 테스트 | ✅ 완료 | 중복 매칭 0건, 방 500개, 실패율 0% |

---

## Phase 0: 환경 수정

**목표**: 가정이 아닌 실제 오류를 확인하고, 체크리스트를 작성하기 위한 근거 수집

### 검증 명령
```bash
./gradlew test 2>&1          # 실제로 실패하는 테스트 목록 확인
docker version               # Docker API 버전 확인
docker compose up -d 2>&1    # Docker Compose 스택 기동 오류 확인
./gradlew compileJava 2>&1   # 컴파일 오류 여부 확인
```

### 예상 발견 항목 (탐색 기반 — 검증 후 확정)
- `MatchServiceTest`: 예외 타입 불일치 가능성
  - 예상: `IllegalStateException` → 실제: `MatchAlreadyProcessedException`
- Docker: API v1.24 (클라이언트) → v1.44+ 업그레이드 필요
- Docker Compose: 업그레이드 후 스택 기동 재시도 필요

---

## Phase 1: 동시성 테스트

**목표**: `concurrency_testing_checklist.md`의 Phase A~D 완료 + 결과 기록

**선행 조건**: 로컬 Supabase(또는 Docker PostgreSQL) 실행, Docker Compose 스택 기동

```
Phase A: 기본 동시성 (비관적 락) — FOR UPDATE SKIP LOCKED
Phase B: 낙관적 락 구현 비교 (이번 범위 제외 — TODO 문서 추가)
Phase C: 스케줄러 중복 실행 방지 검증
Phase D: 최대 동시 부하 (k6 일부 실행)
```

결과 기록 위치: `docs/concurrency_test_results.md` (신규 생성 예정)

---

## Phase 2: 부하 테스트

**목표**: k6 1000 VU 시나리오 실행 + Grafana 결과 캡처

### 실행 방법 (Grafana 연동 — xk6 커스텀 빌드)

```bash
# 1. xk6 설치 (Go 필요)
go install go.k6.io/xk6/cmd/xk6@latest

# 2. Prometheus remote write 확장 포함 빌드
xk6 build --with github.com/grafana/xk6-output-prometheus-remote@latest

# 3. Prometheus remote write 활성화 후 실행
K6_PROMETHEUS_RW_SERVER_URL=http://localhost:9090/api/v1/write \
./k6 run --out experimental-prometheus-rw k6/concurrent_match_test.js
```

> Go 미설치 시 대안: Docker로 xk6 빌드
> ```bash
> docker run --rm -v $(pwd):/out grafana/xk6 build \
>   --with github.com/grafana/xk6-output-prometheus-remote@latest
> ```

### 임계값 설정 근거

| 지표 | 설정값 | 판정 | 근거 |
|------|--------|------|------|
| HTTP p95 응답시간 | < 500ms | ✅ 적정 | Nielsen Norman Group: 500ms = 사용자 흐름 유지 한계 |
| HTTP p99 응답시간 | < 1000ms | ✅ 적정 | 1초 이내 = 사용자 주의 유지 (Nielsen Norman) |
| 실패율 | < 1% | ⚠️ 주의 | `MatchAlreadyProcessedException`(409)가 포함되는지 확인 필요 |
| 매칭 완료 p95 | < 30s | ✅ 적정 | Phase 4 전환 시점(30s)과 일치 |
| 매칭 완료 p99 | < 40s | ✅ 적정 | k6 maxWaitMs = 40000ms 기준 |

---

## Phase 3: 결과 문서화

**목표**: 실행 결과를 3개 .md 파일로 정리

| 문서 | 내용 |
|------|------|
| `docs/poc_completion_report.md` | POC 종합 보고서 |
| `docs/load_test_results.md` | 부하 테스트 상세 결과 |
| `docs/adoption_guide.md` | 실 서비스 도입 가이드 |

---

## 향후 과제 (이번 범위 제외)

- **낙관적 락(Optimistic Lock) 비교 구현**
  - `@Version` 필드 추가 + RetryableTransaction 패턴
  - 비관적 락 vs 낙관적 락 성능 비교 테스트
  - → `docs/adoption_guide.md`에 TODO 항목으로 명시
