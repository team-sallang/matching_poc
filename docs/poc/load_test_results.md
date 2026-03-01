# 부하 테스트 결과 — k6 동시성 테스트

> 작성일: 2026-03-02

---

## 테스트 환경

| 항목 | 값 |
|------|-----|
| 테스트 도구 | k6 v0.34.1 |
| executor | `per-vu-iterations` (vus: 1000, iterations: 1) |
| 데이터셋 | 1000명 (MALE 500 / FEMALE 500, SEOUL, SPROUT) |
| DB | 로컬 Docker PostgreSQL 17 (localhost:5432) |
| 앱 서버 | Spring Boot 3.4.11 (localhost:8080) |
| 관찰 도구 | Grafana + Prometheus (http://localhost:3000) |

---

## 최종 결과 (4차 — Tomcat 튜닝 포함)

### k6 Summary

```
✓ status is 200 or 202
✓ match completed
✓ match status is MATCHED
✓ match completed within timeout

checks.........................: 100.00% ✓ 2505  ✗ 0
http_req_duration..............: avg=182ms   p(90)=341ms  p(95)=365ms
  { name:match_request }.......: avg=182ms   p(90)=341ms  p(95)=369ms  ✓ (<2000ms)
  { name:match_status }........: avg=182ms   p(90)=339ms  p(95)=354ms  ✓ (<1000ms)
http_req_failed................: 0.00%   ✓ 0   ✗ 1535
match_complete_ms..............: avg=521ms   p(90)=567ms  p(95)=581ms  ✓ (<30000ms)
iterations.....................: 1000    (5.6s 내 완료)
```

### 정확성 검증

| 항목 | 결과 |
|------|------|
| 생성 방 수 | **500개** (1000명 / 2) ✅ |
| rooms 내 고유 유저 | **1000명** ✅ |
| 중복 매칭 | **0건** ✅ |

---

## 전체 테스트 히스토리

### 1차 (Supabase Mumbai, HikariPool=30)

| 지표 | 결과 | 임계값 | 판정 |
|------|------|--------|------|
| match_complete_ms avg | 55,500ms | — | 참고 |
| match_complete_ms p95 | 78,000ms | < 30,000ms | ❌ |
| http_req_duration p95 | ~66,000ms | < 500ms | ❌ |
| HTTP 실패율 | 0% | < 1% | ✅ |
| 중복 매칭 | **0건** | — | ✅ |

**원인**: HikariPool 30 → 970 VU 대기. 초기 가설.

---

### 2차 (Supabase Mumbai, HikariPool=48)

| 지표 | 결과 | 임계값 | 판정 |
|------|------|--------|------|
| match_complete_ms avg | ~38,000ms | — | 참고 |
| HTTP 실패율 | 0% | < 1% | ✅ |
| 중복 매칭 | **0건** | — | ✅ |

**결론**: 풀 증가 효과 없음 → 병목은 Supabase 크로스 리전 레이턴시.

---

### 3차 (로컬 Docker PG, HikariPool=48)

| 지표 | 결과 | 임계값 | 판정 |
|------|------|--------|------|
| match_request p95 | **464ms** | < 2,000ms | ✅ |
| match_status p95 | **359ms** | < 1,000ms | ✅ |
| match_complete_ms avg | **768ms** | — | — |
| match_complete_ms p95 | **1,076ms** | < 30,000ms | ✅ |
| HTTP 실패율 | **0%** | < 1% | ✅ |
| 중복 매칭 | **0건** | — | ✅ |
| 전체 소요 | 6.1s | — | — |

**결론**: Supabase 대비 약 72배 빠름. 병목 = 네트워크 레이턴시 확정.

---

### 4차 (로컬 Docker PG, Tomcat 튜닝 추가)

| 지표 | 결과 | 임계값 | 판정 |
|------|------|--------|------|
| match_request p95 | **369ms** | < 2,000ms | ✅ |
| match_status p95 | **354ms** | < 1,000ms | ✅ |
| match_complete_ms avg | **521ms** | — | — |
| match_complete_ms p95 | **581ms** | < 30,000ms | ✅ |
| HTTP 실패율 | **0%** | < 1% | ✅ |
| 중복 매칭 | **0건** | — | ✅ |
| 전체 소요 | **5.6s** | — | — |

**Tomcat threads.max 200→400으로 이전 대비 약 10% 성능 향상.**

---

## 임계값 설계 근거

| 지표 | 임계값 | 근거 |
|------|--------|------|
| match_request p95 | < 2,000ms | Nielsen Norman Group: 2s 이내 = 사용자 흐름 유지 |
| match_status p95 | < 1,000ms | 단순 SELECT 조회, 1s 이내 응답 기준 |
| http_req_failed | < 1% | 서비스 가용성 기준 |
| match_complete_ms p95 | < 30,000ms | Phase 4 전환 시점(30s)과 일치 |
| match_complete_ms p99 | < 40,000ms | k6 maxWaitMs = 40,000ms 기준 |

---

## 주요 설정 변경 이력

| 변경 | 이유 | 효과 |
|------|------|------|
| HikariPool 30 → 48 | Supabase Nano 80% 룰 | Supabase 환경에서 효과 없음 (레이턴시가 병목) |
| DB: Supabase → 로컬 PG | 네트워크 레이턴시 격리 | 72배 성능 향상 |
| Tomcat accept-count: 1000 | k6 1000 VU 동시 TCP 연결 수용 | connection refused 방지 |
| Tomcat threads.max: 400 | 큐잉 요청 처리 속도 향상 | ~10% 성능 향상 |

---

## Grafana 모니터링

테스트 중 `http://localhost:3000` → **Matching POC Dashboard** 에서 확인 가능:
- JVM 힙 메모리 사용량
- HTTP 요청 레이트 및 응답시간 (Spring Boot Actuator → Prometheus)
- match.request.immediate / match.request.enqueued 카운터
- match.queue.waiting.count 게이지
- match.confirm.success / match.confirm.conflict 카운터
- 분산 트레이스: Tempo (`http://localhost:3200`)
