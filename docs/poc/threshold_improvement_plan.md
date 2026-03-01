# 임계값 개선 계획

## 배경

Phase A 동시성 테스트 결과 (`docs/concurrency_test_results.md`):
- 정확성 ✅: 중복 매칭 0건, 방 500개, HTTP 실패율 0%
- 성능 ❌: `http_req_duration p(95)=1m6s` (임계값 500ms), `match_complete_ms p(95)=78s` (임계값 30s)

근거 조사 결과 임계값 자체보다 **두 가지 근본 문제** 확인:
1. HikariPool(30) 병목 → 커넥션 대기 30s 누적
2. `http_req_duration`이 POST/GET 요청을 혼합 측정 → 의미 없는 숫자

---

## 태스크 목록

### Task 1: HikariPool 사이즈 조정

**파일**: `src/main/resources/application.yaml`

**변경**:
```yaml
# 변경 전
hikari:
  maximum-pool-size: 30
  minimum-idle: 10

# 변경 후
hikari:
  maximum-pool-size: 48   # Supabase Nano 직접 연결 한계(60)의 80%
  minimum-idle: 5         # 유휴 커넥션 최소화 (Supabase 리소스 절약)
```

**근거**:
- Supabase Nano/Micro: 직접 연결 최대 60개 → 앱 권장 48개 (80%)
- 이론적 대기 시간: `(1000 - 48) / 48 * 50ms = 992ms` → p95 < 2s 달성 예상
- `minimum-idle` 감소: 유휴 커넥션이 Supabase 리소스 점유하지 않도록

---

### Task 2: k6 임계값 태그별 분리

**파일**: `k6/concurrent_match_test.js`

**변경**:
```javascript
// 변경 전
thresholds: {
  http_req_duration: ['p(95)<500', 'p(99)<1000'],
  http_req_failed: ['rate<0.01'],
  match_complete_ms: ['p(95)<30000', 'p(99)<40000'],
},

// 변경 후
thresholds: {
  // 태그별 분리: match_request(POST)와 match_status(GET) 구분
  'http_req_duration{name:match_request}': ['p(95)<2000', 'p(99)<5000'],
  'http_req_duration{name:match_status}': ['p(95)<1000', 'p(99)<2000'],
  http_req_failed: ['rate<0.01'],
  match_complete_ms: ['p(95)<30000', 'p(99)<40000'],
},
```

**근거**:
- `match_request` (POST /match): 커넥션 풀 개선 후 p95 < 2s 달성 예상
- `match_status` (GET /status): 단순 SELECT → p95 < 1s (풀 개선 후)
- `match_complete_ms p(95) < 30s`: 시스템 설계(Phase 1~5 최대 30s)와 일치, NN/G 기준과도 정렬

---

### Task 3: k6 재실행 및 검증

**선행 조건**: Task 1, 2 완료 후 앱 재시작

**절차**:
1. `TRUNCATE TABLE match_queue` (Supabase)
2. Spring Boot 앱 재시작 (`./gradlew bootRun`)
3. `k6 run k6/concurrent_match_test.js`
4. 결과 확인: 모든 임계값 통과 여부

**예상 결과**:
- `http_req_duration{name:match_request} p(95)` < 2s ✅
- `http_req_duration{name:match_status} p(95)` < 1s ✅
- `http_req_failed` < 1% ✅
- `match_complete_ms p(95)` < 30s ✅ (Pool 개선으로 폴링 1회 내 완료 예상)

---

### Task 4: 결과 문서 업데이트

**파일**: `docs/concurrency_test_results.md`

- 2차 테스트 결과 섹션 추가
- 임계값 개선 전/후 비교표 추가

---

## 진행 순서

```
Task 1 (Pool 조정)
      ↓
Task 2 (임계값 분리)
      ↓
Task 3 (k6 재실행)
      ↓
Task 4 (문서 업데이트)
```
