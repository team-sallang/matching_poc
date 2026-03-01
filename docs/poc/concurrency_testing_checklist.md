# 동시성 테스트 체크리스트 (매칭)

## 목적
- 비관적 락(`FOR UPDATE`) 기반 매칭의 중복 매칭 방지 여부 검증
- 낙관적 락 전환 시 재시도 비용/충돌률 비교를 위한 기준 확보

## 구체적 실행 계획

### 현재 전제
- 비관적 락(`FOR UPDATE SKIP LOCKED`): 현재 구현됨
- 낙관적 락: **미구현** (`MatchQueue`에 `@Version` 필드 없음, 재시도 로직 없음)

### 브랜치 전략
- 브랜치 생성: `feature/concurrency-test-optimistic-lock` (또는 적절한 이름)
- 베이스 브랜치(`main` 등)에서 분기 후, 아래 Phase A~D를 순서대로 진행

### 단계별 구체적 작업

#### Phase A: 비관적 락 테스트 (현행 유지)
- 브랜치 생성 후, **먼저** 비관적 락 그대로 두고 시나리오 1·2 테스트
- 환경 전제·데이터셋·모니터링 체크리스트대로 준비 및 실행
- 결과를 리포팅 템플릿 "시나리오 1 결과", "시나리오 2 결과"에 기록

#### Phase B: 낙관적 락 구현 (향후 과제 — 현재 범위 제외)
> **현재 브랜치에서는 비관적 락(Phase A) 검증만 진행. 낙관적 락은 별도 브랜치에서 구현 예정.**
- `match_queue` 테이블: `version` 컬럼 추가 (마이그레이션 파일 생성)
- `MatchQueue` 엔티티: `@Version` 필드 추가
- `MatchQueueRepository`: Phase 1~5 매칭 쿼리에서 `FOR UPDATE SKIP LOCKED` 제거 → 일반 SELECT로 변경
- `MatchService` / `MatchScheduler`: `OptimisticLockException` 처리 및 재시도 로직 추가 (Spring Retry 또는 수동 구현)
- 단위·통합 테스트 보강 후, 동작 확인

#### Phase C: 낙관적 락 테스트 (향후 과제 — 현재 범위 제외)
> **Phase B 완료 후 진행.**
- 동일 데이터셋·동일 시나리오(1·2)로 낙관적 락 구현 기준 테스트
- "시나리오 1/2 (낙관적 락)" 체크리스트 및 리포팅 템플릿대로 실행·기록

#### Phase D: 비교 분석 및 정리 (향후 과제 — 현재 범위 제외)
> **Phase A + Phase C 결과 모두 나온 후 진행.**
- "비교 분석 (비관적 락 vs 낙관적 락)" 체크리스트 진행
- 리포팅 템플릿 "비교 분석 결과", "판정" 작성
- 필요 시 `main` 머지 또는 PR 생성

### 변경 대상 파일 (Phase B 구현 시)
- `supabase/migrations/` (supabase migration new add_version_match_queue 후 생성)
- `src/main/java/com/salang/matching_poc/model/entity/MatchQueue.java`
- `src/main/java/com/salang/matching_poc/repository/MatchQueueRepository.java`
- `src/main/java/com/salang/matching_poc/service/MatchService.java`
- `src/main/java/com/salang/matching_poc/worker/MatchScheduler.java`
- `build.gradle` (Spring Retry 사용 시)

## 환경 전제 (Supabase/Postgres)
- [x] DB: Supabase(Postgres 매니지드) 준비 (런타임 확인 필요)
- [ ] 테스트 프로젝트 구성하여 준비 (낙관적 락 계획 실행 시 진행)
- [ ] 테스트 실행 시각/부하 조건 기록 (런타임 실행 시 기록 필요)
- [x] PostgreSQL 버전 확인(ver.17.6) (런타임 확인 필요)
- [ ] 연결 풀 설정 확인 (런타임 확인 필요)
- [x] 낙관적 락 구현 여부 확인 (미구현 확인됨: `MatchQueue` 엔티티에 `@Version` 필드 없음)
- [x] `MatchQueue` 엔티티에 `@Version` 필드 존재 여부 확인 (없음 확인됨: `src/main/java/com/salang/matching_poc/model/entity/MatchQueue.java` 확인)
- [x] Spring Boot Actuator Prometheus 엔드포인트 설정 확인 (`/actuator/prometheus` 활성화 확인됨: `application.yaml`의 `management.endpoints.web.exposure.include: prometheus`)
- [x] Prometheus remote write receiver 설정 확인 (`--web.enable-remote-write-receiver` 플래그 확인됨: `compose.yml`)
- [x] Prometheus Spring Boot 앱 스크랩 설정 확인 (`prometheus.yml`에 `spring-boot-app` job 설정 확인됨)

## 데이터셋 구성 (대규모 합성 데이터)
### 사용자 데이터
- [x] 동시 사용자 수 결정 전략: 1000명, 1분
- [x] 성별 분포 정의 및 고정 ( 남/여 50:50 )
- [x] 지역(`region`) (서울로 고정)
- [x] 나이(`birth_year`) (1995년 01월 01로 고정)
- [x] 취미(`hobby_ids`) (축구, 영화 시청, 독서/ 즉 3가지의 취미로 매칭될 수 있게 구성)
- [x] 등급(`tier`) 분포 정의 (SPROUT로 고정)

### 큐/룸 데이터
- [ ] 매칭 대기열(`match_queue`) 초기 상태 수량 설정 (예: 대기 중인 사용자 수)
- [ ] 기존 매칭된 룸(`rooms`) 데이터 존재 여부 확인
- [ ] 중복 사용자/중복 큐 삽입 방지 조건 확인

## 모니터링(최소 세트)
### 앱 응답시간
- [ ] 평균 응답시간 측정
- [ ] 95 백분위수(95p) 응답시간 측정
- [ ] 측정 도구/방법 확정 (예: 애플리케이션 로그, APM)

### 트랜잭션 실패/재시도
- [ ] 트랜잭션 실패 횟수 카운트
- [ ] 재시도 횟수 카운트
- [ ] 실패 원인 분류 (락 실패, 데이터 충돌 등)
- [ ] `OptimisticLockException` 발생 횟수 (낙관적 락)
- [ ] 재시도 성공률 (낙관적 락)

### 중복 매칭/큐 삽입
- [ ] 중복 매칭 발생 건수 확인 (0건 목표)
- [ ] 중복 큐 삽입 발생 건수 확인 (0건 목표)
- [ ] 검증 쿼리/로직 준비

### 락 대기/실패
- [ ] `NOWAIT` 옵션 사용 시 실패 건수 측정
- [ ] 락 대기 시간 로그 수집 (대기 발생 시)
- [ ] `pg_locks` 뷰로 락 상태 확인

### 주요 매칭 쿼리 지연
- [ ] Phase 1-5 매칭 쿼리별 평균 지연 측정
- [ ] Phase 1-5 매칭 쿼리별 95p 지연 측정
- [ ] `pg_stat_statements` 또는 쿼리 로그 활용

## 시나리오 1: 동시 매칭 요청 경합
### 실행 전 준비
- [x] k6 설치 확인: `k6 version` (또는 Docker 사용)
- [x] 모니터링 스택 시작: `docker compose up -d prometheus grafana`
- [x] Prometheus 접근 확인: `http://localhost:9090`
- [x] Grafana 접근 확인: `http://localhost:3000` (admin/admin)
- [x] Grafana 데이터 소스: **프로비저닝으로 자동 구성됨** (`grafana/provisioning/datasources/datasources.yml` → Prometheus, Loki)
  - 수동 추가 시: Configuration → Data Sources → Prometheus URL `http://prometheus:9090`
- [x] k6 메트릭용 Grafana 대시보드: **프로비저닝됨** `grafana/dashboards/k6_overview.json` (대시보드: k6 Overview)
  - **주의:** 기본 k6·grafana/k6 이미지에는 `experimental-prometheus-rw` 출력이 없음. k6 Overview에 데이터가 쌓이려면 **xk6-output-prometheus-remote** 확장을 넣어 빌드한 k6로 `--out experimental-prometheus-rw` 사용해야 함. (자세한 단계: `k6/README.md`)
  - 필수 패널: 응답시간 (50p, 95p, 99p), 실패율, 처리량 (RPS), 활성 VU 수
  - Grafana 대시보드 쿼리 예시:
    - 응답시간 (95p): `histogram_quantile(0.95, rate(k6_http_req_duration_bucket[1m]))`
    - 실패율: `rate(k6_http_req_failed[1m])`
    - 처리량 (RPS): `rate(k6_http_reqs[1m])`
    - 활성 VU 수: `k6_vus`
  - 또는 k6 기본 대시보드 사용
- [x] 동시 사용자 수 결정: **1000명** (고정)
- [x] k6 시나리오 executor 선택: `per-vu-iterations` (VU당 1회 실행)
  - 설정: `vus: 1000, iterations: 1`
  - 이유: 매칭은 사용자당 1번만 가능. `constant-vus`는 동일 사용자가 반복 요청 → 409 발생. `per-vu-iterations`로 각 VU가 정확히 1번만 실행해야 함.
- [x] k6 스크립트 작성:
  - 스크립트 파일: `k6/concurrent_match_test.js` (완료)
  - `per-vu-iterations` executor 설정 완료
  - 동일 조건 사용자 ID 목록 로드 (`SharedArray`) 완료
  - Threshold 설정 완료: `http_req_duration: ['p(95)<500', 'p(99)<1000']`, `http_req_failed: ['rate<0.01']`, `match_complete_ms: ['p(95)<30000', 'p(99)<40000']`
  - 참고: `k6/README.md`에서 사용 방법 확인
- [x] k6 실행 명령어 준비:
  ```bash
  # 프로젝트 루트에서 실행 (Grafana 연동 없이 콘솔 출력)
  k6 run k6/concurrent_match_test.js
  ```
- [x] 테스트 시작 전 큐 초기화: `TRUNCATE TABLE match_queue`
  - 이유: 순수 경합 테스트를 위해 기존 큐 데이터 제거
- [x] 동일 조건 사용자 1000명 데이터 준비 (SQL 스크립트)
  - 동일 조건 파라미터 고정값 설정:
    - 성별(`gender`): 예) `MALE` 또는 `FEMALE` (단일 값)
    - 지역(`region`): 예) `SEOUL` (단일 값)
    - 나이(`birth_year`): 예) `1995` (단일 값, 또는 ±2년 범위)
    - 취미(`hobby_ids`): 예) `[1, 2, 3]` (동일 배열)
  - 사용자 ID 목록 준비: JSON 파일 (예: `user_ids.json`) => Supabase DB에서 userID를 뽑아서 생성함.
- [x] 기준 시각 기록 (2026-03-02 02:30 KST)

### 실행 중
- [x] k6 테스트 실행: 위 명령어로 실행
- [ ] Grafana 대시보드에서 실시간 모니터링 (테스트 실행 중): _(xk6 미사용으로 k6 메트릭 미수집 — 스킵)_
- [x] k6 콘솔 출력 확인 (실시간 진행 상황)
- [ ] Prometheus에서 메트릭 수집 확인: _(xk6 미사용 — 스킵)_

### 실행 후 검증
#### 핵심 검증: 중복 매칭 방지 (최우선)
- [x] 동일 사용자 중복 매칭 0건 확인 (SQL 쿼리): **결과: 0건 ✅**
  ```sql
  SELECT COUNT(*) as duplicate_room_users
  FROM rooms r1
  INNER JOIN rooms r2 ON (
    (r1.user1_id = r2.user1_id OR r1.user1_id = r2.user2_id) OR
    (r1.user2_id = r2.user1_id OR r1.user2_id = r2.user2_id)
  )
  WHERE r1.room_id != r2.room_id
    AND r1.created_at >= NOW() - INTERVAL '5 minutes'
    AND r2.created_at >= NOW() - INTERVAL '5 minutes';
  ```
- [ ] match_queue 상태 일관성 확인 (rooms에 있는 사용자는 MATCHED여야 함): _(선택 검증)_
  ```sql
  SELECT COUNT(*) as inconsistent_status_count
  FROM (
    SELECT user1_id as user_id FROM rooms WHERE created_at >= NOW() - INTERVAL '5 minutes'
    UNION
    SELECT user2_id as user_id FROM rooms WHERE created_at >= NOW() - INTERVAL '5 minutes'
  ) room_users
  LEFT JOIN match_queue mq ON room_users.user_id = mq.user_id
  WHERE mq.status != 'MATCHED' OR mq.user_id IS NULL;
  ```

#### 매칭 결과 통계
- [x] 매칭된 방 수 확인: **500개 ✅**
  ```sql
  SELECT COUNT(*) as total_rooms_created
  FROM rooms
  WHERE created_at >= NOW() - INTERVAL '5 minutes';
  ```
- [ ] 매칭된 사용자 수 확인:
  ```sql
  SELECT COUNT(DISTINCT user_id) as matched_users_count
  FROM (
    SELECT user1_id as user_id FROM rooms WHERE created_at >= NOW() - INTERVAL '5 minutes'
    UNION
    SELECT user2_id as user_id FROM rooms WHERE created_at >= NOW() - INTERVAL '5 minutes'
  ) matched_users;
  ```
- [ ] 매칭 실패 인원 수 확인:
  ```sql
  SELECT COUNT(*) as unmatched_users_count
  FROM match_queue
  WHERE status = 'WAITING'
    AND created_at >= NOW() - INTERVAL '5 minutes';
  ```
- [ ] 전체 매칭 성공률 계산:
  ```sql
  WITH total_requests AS (
    SELECT COUNT(*) as total_requested
    FROM match_queue
    WHERE created_at >= NOW() - INTERVAL '5 minutes'
  ),
  matched_users AS (
    SELECT COUNT(DISTINCT user_id) as total_matched
    FROM (
      SELECT user1_id as user_id FROM rooms WHERE created_at >= NOW() - INTERVAL '5 minutes'
      UNION
      SELECT user2_id as user_id FROM rooms WHERE created_at >= NOW() - INTERVAL '5 minutes'
    ) m
  )
  SELECT
    tr.total_requested,
    mu.total_matched,
    tr.total_requested - mu.total_matched as total_unmatched,
    ROUND(100.0 * mu.total_matched / tr.total_requested, 2) as match_success_rate_percent
  FROM total_requests tr, matched_users mu;
  ```

#### 매칭 처리 시간 통계 (Spring Boot 모니터링)
- [ ] Prometheus에서 `match_confirm_seconds_*`, `match_request_seconds_*` 메트릭 확인
- [ ] 매칭 확정 처리 시간 (confirmMatch) 통계:
  - 평균: `rate(match_confirm_seconds_sum[5m]) / rate(match_confirm_seconds_count[5m])`
  - 95p: `histogram_quantile(0.95, rate(match_confirm_seconds_bucket[5m]))`
  - 99p: `histogram_quantile(0.99, rate(match_confirm_seconds_bucket[5m]))`
- [ ] 매칭 요청 처리 시간 (requestMatch) 통계:
  - 평균: `rate(match_request_seconds_sum[5m]) / rate(match_request_seconds_count[5m])`
  - 95p: `histogram_quantile(0.95, rate(match_request_seconds_bucket[5m]))`
  - 99p: `histogram_quantile(0.99, rate(match_request_seconds_bucket[5m]))`
- 참고: 매칭 성공 인원 기준의 처리 시간은 `confirmMatch` 메트릭이 가장 직접적임

#### 보조 지표: 성능 및 HTTP 응답
- [x] k6 summary 확인 및 기록: **결과: `docs/concurrency_test_results.md` 참조**
  - http_req_duration avg=36.54s, p(95)=1m6s
  - 실패율 0% ✅
  - 총 요청 1505건 / 1000 iterations 완료
- [ ] Grafana에서 최종 메트릭 확인 (선택):
  - 평균/95p/99p 응답시간
  - 총 처리량 (RPS)
- 참고: HTTP 오류율은 중복 매칭 방지와 직접 관련 없음 (네트워크/서버 상태 확인용)

## 시나리오 2: 큐 삽입 vs 매칭 처리 경합
### 실행 전 준비
- [ ] 큐 삽입 요청과 매칭 처리 트랜잭션의 동시 실행 조건 정의
- [ ] 테스트 시작 시 큐 상태 기록 (대기 중인 사용자 수)
- [ ] 큐 삽입 요청 수준 정의 (예: 초당 N건)
- [ ] 매칭 스케줄러 실행 주기 확인
- [ ] 모니터링 도구 준비 및 시작
- [ ] 기준 시각 기록

### 실행 중
- [ ] 큐 삽입과 매칭 처리 동시 수행
- [ ] 실패/재시도/락 대기 로그 실시간 수집
- [ ] DB 락 상태 모니터링 (`pg_locks` 확인)
- [ ] 큐 상태 변화 모니터링 (삽입/매칭/삭제 추적)

### 실행 후 검증
- [ ] 큐 중복 삽입 0건 확인
- [ ] 매칭 실패/충돌 최소화 확인
- [ ] 평균/95p 응답시간 계산
- [ ] 실패/재시도 횟수 집계
- [ ] 락 대기/실패 건수 집계
- [ ] 큐 삽입 성공률 계산

## 시나리오 1/2 (낙관적 락)
### 실행 전 준비
- [ ] 비관적 락과 동일한 시나리오 준비

### 실행 후 검증
- [ ] 중복 매칭/큐 삽입 0건 확인
- [ ] `OptimisticLockException` 발생 횟수 집계
- [ ] 재시도 횟수 및 재시도 성공률 계산
- [ ] 평균/95p 응답시간 계산 (재시도 포함)

## 비교 분석 (비관적 락 vs 낙관적 락)
- [ ] 응답시간 비교 (비관적: 락 대기 vs 낙관적: 재시도 오버헤드)
- [ ] 실패/재시도 횟수 비교
- [ ] 충돌률에 따른 적합성 판단 (< 5%: 낙관적 유리, > 20%: 비관적 유리)

## 판정 기준
- [ ] 중복 매칭/큐 삽입 0건 (비관적/낙관적 락 모두)
- [ ] 실패/재시도 비율이 허용 범위 내 (예: < 5%)
- [ ] 응답시간/쿼리 지연이 목표 범위 내 (예: 평균 < 100ms, 95p < 500ms)
- [ ] 재시도 성공률 허용 범위 내 (낙관적 락, 확정 필요)

## 테스트 결과 리포팅 템플릿
### 요약
- 테스트 일시: YYYY-MM-DD HH:mm:ss
- 테스트 환경: Supabase(Postgres 매니지드)
- 데이터셋 규모: 사용자 N명, 초기 큐 M명

### 시나리오 1 결과
- 동시 요청 수: N명
- 중복 매칭: 0건 ✓
- 평균 응답시간: X ms
- 95p 응답시간: Y ms
- 매칭 확정 처리 시간 (confirmMatch): avg X ms / 95p Y ms / 99p Z ms
- 실패/재시도 횟수: Z건
- 락 대기/실패: W건

### 시나리오 2 결과
- 큐 삽입 요청 수: N건
- 중복 큐 삽입: 0건 ✓
- 평균 응답시간: X ms
- 95p 응답시간: Y ms
- 매칭 확정 처리 시간 (confirmMatch): avg X ms / 95p Y ms / 99p Z ms
- 실패/재시도 횟수: Z건
- 락 대기/실패: W건

### 주요 매칭 쿼리 지연
- Phase 1 평균/95p: X ms / Y ms
- Phase 2 평균/95p: X ms / Y ms
- Phase 3 평균/95p: X ms / Y ms
- Phase 4 평균/95p: X ms / Y ms
- Phase 5 평균/95p: X ms / Y ms

### 시나리오 1/2 (낙관적 락) 결과
- 동시 요청 수: N명
- 중복 매칭/큐 삽입: 0건 ✓
- `OptimisticLockException` 발생 횟수: M건
- 재시도 횟수: K회
- 재시도 성공률: X%
- 평균 응답시간: A ms (재시도 포함)
- 95p 응답시간: B ms (재시도 포함)

### 비교 분석 결과
- 비관적 락 vs 낙관적 락 비교표 (응답시간, 재시도/락 대기, 처리량)
- 충돌률에 따른 적합성 판정

### 판정
- [ ] 모든 판정 기준 통과
- [ ] 비관적 락 기반 매칭 안정성 확인
- [ ] 낙관적 락 전환 검토 가능 여부

## 참고 (PostgreSQL / Spring Data JPA)
- `FOR UPDATE`: 조회한 행에 대한 행 락을 획득. 다른 트랜잭션의 `UPDATE/DELETE/SELECT FOR UPDATE`는 대기 또는 실패(`NOWAIT`) 처리됨.
- `NOWAIT`: 락을 즉시 획득하지 못하면 에러 반환 (대기 없음)
- `SKIP LOCKED`: 잠긴 행을 건너뛰어 처리. 큐 같은 다중 소비자 패턴에 유용하지만 일관성 뷰가 아님 (PostgreSQL 문서 참고)
- `@Version`: 낙관적 락 구현 (JPA가 자동으로 버전 체크). `OptimisticLockException` 발생 시 재시도 필요. 비관적 락과 달리 락 대기 없음

## k6 테스트 스크립트 및 가이드
- k6 스크립트 템플릿: `k6/concurrent_match_test.js`
- k6 사용 가이드: `k6/README.md`
- Grafana 대시보드 쿼리 예시는 위 "실행 전 준비" 섹션 참고
