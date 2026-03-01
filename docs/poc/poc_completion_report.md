# POC 완료 보고서 — 소셜 매칭 서비스 동시성 검증

> 작성일: 2026-03-02

---

## 1. 개요

Spring Boot 3.4 + PostgreSQL 기반 소셜 매칭 서비스 POC의 동시성 안전성 검증 완료 보고서.

| 항목 | 내용 |
|------|------|
| 프로젝트 | matching_poc |
| 기술 스택 | Spring Boot 3.4, Java 17, PostgreSQL 17, HikariCP |
| 락 전략 | 비관적 락 (`FOR UPDATE SKIP LOCKED`) |
| 테스트 도구 | k6 v0.34.1 (per-vu-iterations, 1000 VU) |
| 테스트 기간 | 2026-03-02 |

---

## 2. 검증 목표

1. **정확성**: 1000명 동시 매칭 요청에서 중복 매칭이 발생하지 않아야 한다
2. **완전성**: 모든 사용자가 매칭되어야 한다 (1000명 → 500쌍)
3. **안정성**: HTTP 실패율 < 1%

---

## 3. 최종 검증 결과 (로컬 Docker PostgreSQL)

### 정확성 검증

| 검증 항목 | 기댓값 | 실측값 | 판정 |
|-----------|--------|--------|------|
| 생성된 방 수 | 500개 | **500개** | ✅ |
| rooms 내 고유 유저 수 | 1000명 | **1000명** | ✅ |
| 중복 매칭 (한 유저가 2개 이상 방 소속) | 0건 | **0건** | ✅ |
| HTTP 실패율 | < 1% | **0%** | ✅ |
| k6 checks 통과율 | 100% | **100%** | ✅ |

### 성능 지표

| 지표 | 결과 | 임계값 | 판정 |
|------|------|--------|------|
| match_request p(95) | **369ms** | < 2,000ms | ✅ |
| match_status p(95) | **354ms** | < 1,000ms | ✅ |
| match_complete_ms avg | **521ms** | — | — |
| match_complete_ms p(95) | **581ms** | < 30,000ms | ✅ |
| 전체 테스트 소요 시간 | **5.6s** | — | — |

---

## 4. 환경별 성능 비교

| 환경 | match_complete_ms avg | match_complete_ms p95 | 임계값 통과 |
|------|-----------------------|-----------------------|-------------|
| Supabase (Mumbai, free tier) | 55,500ms | 78,000ms | ❌ |
| 로컬 Docker PG (localhost) | **521ms** | **581ms** | ✅ |
| **차이** | **~107배** | **~134배** | |

**병목 원인**: Supabase free tier의 Seoul→Mumbai 크로스 리전 레이턴시 (~2s/쿼리).
동일 코드·락 전략에서 DB 위치만 변경 시 모든 임계값 통과 → 코드/로직 문제 없음.

---

## 5. 핵심 기술 결정 사항

### 5.1 비관적 락 (`FOR UPDATE SKIP LOCKED`)

```sql
SELECT * FROM match_queue
WHERE status = 'WAITING'
FOR UPDATE SKIP LOCKED
LIMIT :batchSize
```

- 동시 스케줄러 실행 시 같은 행을 두 번 처리하는 것을 방지
- `SKIP LOCKED`: 이미 잠긴 행을 기다리지 않고 건너뜀 → 데드락 없음
- 1000 VU 동시 부하에서 중복 매칭 0건 검증 완료

### 5.2 원자적 상태 업데이트 (`updateStatusIf`)

```java
// WAITING → MATCHED 상태 전환을 원자적으로 수행
int updated = matchQueueRepository.updateStatusIf(ids, "WAITING", "MATCHED");
if (updated != 2) throw new MatchAlreadyProcessedException();
```

- 낙관적 검증 레이어: bulk update 결과가 2가 아니면 이미 처리된 것으로 판단
- `FOR UPDATE SKIP LOCKED`와 조합하여 이중 안전 보장

### 5.3 스케줄러 단일 스레드

```yaml
task:
  scheduling:
    pool:
      size: 1  # 단일 스레드: 중복 실행 방지
```

- `fixedDelay: 1000ms`로 이전 실행 완료 후 1초 대기
- 스케줄러 자체의 동시 실행 가능성 원천 차단

### 5.4 HikariCP 설정

```yaml
hikari:
  maximum-pool-size: 48  # Supabase Nano 직접 연결 한계(60)의 80%
  minimum-idle: 5
```

### 5.5 Tomcat 동시 연결 설정

```yaml
server:
  tomcat:
    accept-count: 1000  # TCP backlog (1000 VU 동시 부하 대응)
    threads:
      max: 400
```

---

## 6. 단계별 진행 이력

| 단계 | 내용 | 결과 |
|------|------|------|
| Phase 0 | 환경 검증 (테스트·빌드·Docker 스택) | ✅ 전체 정상 |
| Phase A | 비관적 락 동시성 테스트 (Supabase) | ✅ 정확성 확인, 성능 병목 발견 |
| 병목 분석 | HikariPool vs 크로스 리전 레이턴시 격리 | ✅ 병목 = 네트워크 레이턴시 확정 |
| Phase A (로컬) | 로컬 Docker PG 비교 테스트 | ✅ 모든 임계값 통과 |

---

## 7. 결론

> `FOR UPDATE SKIP LOCKED` 기반 비관적 락은 1000명 동시 매칭 환경에서
> **중복 매칭 0건, 100% 매칭 완전성, 0% HTTP 실패율**을 달성했다.
>
> 코드와 락 전략은 프로덕션 적용 준비가 완료되었다.
> 성능 목표 달성을 위해서는 **동일 리전 PostgreSQL** 사용이 필수다.
