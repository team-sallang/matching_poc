# 실 서비스 도입 가이드

> 작성일: 2026-03-02
> 기반: matching_poc Phase A 검증 결과

---

## 1. 프로덕션 적용 전 체크리스트

### 필수 (Must)

- [ ] **동일 리전 PostgreSQL 사용**: Supabase 유료 플랜 → Seoul 리전(ap-northeast-2) 또는 자체 PG 서버
- [ ] **HikariCP 사이즈 조정**: DB max_connections의 80% 이하로 설정
- [ ] **Tomcat accept-count 조정**: 예상 동시 접속자 수에 맞게 설정
- [ ] **스케줄러 단일 인스턴스 보장**: 다중 인스턴스 배포 시 분산 락 필요 (→ [3.1 참고](#31-다중-인스턴스-배포))
- [ ] **DB 마이그레이션 관리**: `supabase/migrations/` 폴더의 SQL 적용 순서 보장

### 권장 (Should)

- [ ] 커넥션 풀 모니터링 (`hikaricp_connections_active` 메트릭 알림 설정)
- [ ] 매칭 큐 대기열 모니터링 (`match.queue.waiting.count` 게이지 알림)
- [ ] 매칭 충돌 모니터링 (`match.confirm.conflict` 급증 시 알림)
- [ ] Sentry 또는 유사 도구로 `MatchAlreadyProcessedException` 추적

---

## 2. 인프라 권장 사양

### 애플리케이션 서버

| 항목 | 개발/POC | 프로덕션 (초기) |
|------|----------|----------------|
| CPU | 1 core | 2 core 이상 |
| 메모리 | 512MB | 1GB 이상 |
| JVM Heap | 256MB | 512MB ~ 1GB |
| Tomcat threads.max | 400 | 200 (기본값, 모니터링 후 조정) |
| Tomcat accept-count | 1000 | 예상 동시 사용자 수 |

### PostgreSQL

| 항목 | 권장 |
|------|------|
| 리전 | 앱 서버와 동일 리전 |
| 최대 연결 수 | `HikariPool max-connections / 0.8` 이상 |
| 인덱스 | `match_queue(status, created_at)`, `match_queue(user_id)` |

### HikariCP 설정 공식

```
max-pool-size = min(DB max_connections × 0.8, 예상 동시 쿼리 수)
```

예시 (Supabase Pro, max_connections=200):
```yaml
hikari:
  maximum-pool-size: 160   # 200 × 0.8
  minimum-idle: 10
  connection-timeout: 30000
  idle-timeout: 600000
  max-lifetime: 1800000
```

---

## 3. 주요 확장 시나리오

### 3.1 다중 인스턴스 배포

현재 스케줄러는 단일 스레드(`pool.size: 1`)로 동작. **다중 인스턴스 배포 시 스케줄러 중복 실행 문제 발생**.

**해결 방법 (선택)**:

| 방법 | 설명 | 복잡도 |
|------|------|--------|
| `ShedLock` | DB 기반 분산 락 라이브러리 | 낮음 |
| `@ConditionalOnProperty` | 스케줄러 전용 인스턴스 분리 | 중간 |
| Kubernetes Leader Election | k8s 환경에서 리더만 실행 | 높음 |

ShedLock 추가 예시:
```java
@Scheduled(fixedDelay = 1000)
@SchedulerLock(name = "matchScheduler", lockAtMostFor = "PT5S")
public void runMatchingLoop() { ... }
```

### 3.2 매칭 알림 (현재 미구현)

현재 `POST /match` → 202 응답 후 클라이언트가 `GET /match/status` 폴링.
프로덕션에서는 폴링 대신 **WebSocket 또는 SSE 푸시**로 전환 권장.

```
현재:  클라이언트 → 5초 간격 폴링 → GET /match/status
권장:  스케줄러 → WebSocket Push → 클라이언트
```

### 3.3 낙관적 락 비교 (향후 과제)

현재 구현: 비관적 락 (`FOR UPDATE SKIP LOCKED`)
향후 비교 대상: 낙관적 락 (`@Version` + `RetryableTransaction`)

| 전략 | 장점 | 단점 | 적합한 경우 |
|------|------|------|-------------|
| 비관적 락 | 충돌 시 재시도 없음, 예측 가능 | DB 락 유지 비용 | 충돌 빈도 높을 때 |
| 낙관적 락 | 락 대기 없음, 처리량 높음 | 충돌 시 재시도 오버헤드 | 충돌 빈도 낮을 때 |

---

## 4. 모니터링 설정

### Prometheus 알림 규칙 (예시)

```yaml
groups:
  - name: matching
    rules:
      - alert: MatchQueueBacklog
        expr: match_queue_waiting_count > 100
        for: 30s
        annotations:
          summary: "매칭 대기열 100명 초과"

      - alert: MatchConflictRate
        expr: rate(match_confirm_conflict_total[1m]) > 0.1
        for: 1m
        annotations:
          summary: "매칭 충돌 발생률 높음 (분당 0.1건 초과)"

      - alert: HikariPoolExhausted
        expr: hikaricp_connections_pending > 10
        for: 15s
        annotations:
          summary: "HikariCP 커넥션 대기 10개 초과"
```

### 핵심 Grafana 패널

| 패널 | 메트릭 | 의미 |
|------|--------|------|
| 매칭 요청 처리율 | `match_request_immediate_total` + `match_request_enqueued_total` | 즉시 매칭 vs 대기 비율 |
| 대기열 크기 | `match_queue_waiting_count` | 현재 대기 중인 사용자 수 |
| 매칭 충돌률 | `match_confirm_conflict_total` | 동시성 충돌 빈도 |
| HTTP 응답시간 | `http_server_requests_seconds` p95 | 서비스 응답성 |

---

## 5. 배포 환경 변수

```bash
# 필수
SPRING_DATASOURCE_URL=jdbc:postgresql://<host>:<port>/<db>
SPRING_DATASOURCE_USERNAME=<user>
SPRING_DATASOURCE_PASSWORD=<password>
OTLP_ENDPOINT=http://<tempo-host>:4318/v1/traces

# 선택 (기본값 있음)
MATCHING_SCHEDULE_FIXED_DELAY=1000
```

---

## 6. 알려진 한계 및 TODO

| 항목 | 현재 상태 | 권장 개선 |
|------|-----------|-----------|
| 매칭 알림 | 폴링 방식 | WebSocket/SSE 전환 |
| 다중 인스턴스 | 단일 인스턴스만 안전 | ShedLock 도입 |
| 낙관적 락 비교 | 미구현 | Phase B에서 검증 |
| 채팅방 구현 | Room 생성까지만 | 실제 채팅 기능 필요 |
| 사용자 탈퇴/취소 | 미구현 | match_queue 취소 API 필요 |
