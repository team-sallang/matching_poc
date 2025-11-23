# k6 부하 테스트 가이드

## 개요

k6는 매칭 시스템의 부하 테스트를 수행하는 도구입니다. Docker Compose를 통해 실행합니다.

## 사전 요구사항

- Docker
- Spring Boot 애플리케이션이 localhost:8080에서 실행 중

## 테스트 사용자 생성

테스트 실행 전에 `scripts/k6/test-users.json` 파일을 생성해야 합니다.

```bash
make regenerate-users
```

이 명령은 `.env` 파일의 `VU` 환경변수를 읽어서 해당 수만큼의 테스트 사용자를 생성합니다.

- 사용자 ID: UUID 형식
- 성별: 50:50 비율 (male/female)

## 실행 방법

1. `env.example` 파일을 `.env`로 복사하고 값 수정
2. 실행:

```bash
make test
```

또는

```bash
docker compose run --rm k6 run /scripts/matching.js
```

## 환경변수

| 변수                     | 기본값                | 설명                                      |
| ------------------------ | --------------------- | ----------------------------------------- |
| `VU`                     | 1000                  | 동시 사용자 수 (Virtual Users)            |
| `DURATION`               | 5m                    | 테스트 지속 시간                          |
| `RAMP_UP`                | 30s                   | Ramp-up 시간                              |
| `RAMP_DOWN`              | 30s                   | Ramp-down 시간                            |
| `BASE_URL`               | http://localhost:8080 | Spring Boot 앱 주소                       |
| `POLLING_INTERVAL`       | 0.1                   | Status polling 간격 (초)                  |
| `TIMEOUT`                | 30                    | 타임아웃 (초)                             |
| `MATCH_SUCCESS_WAIT_MIN` | 30                    | 매칭 성공 후 최소 대기 시간 (초)          |
| `MATCH_SUCCESS_WAIT_MAX` | 300                   | 매칭 성공 후 최대 대기 시간 (초, 5분)     |
| `TIMEOUT_RETRY_WAIT_MIN` | 5                     | 타임아웃 후 재시도 전 최소 대기 시간 (초) |
| `TIMEOUT_RETRY_WAIT_MAX` | 10                    | 타임아웃 후 재시도 전 최대 대기 시간 (초) |

## 테스트 시나리오

### 기본 시나리오 (v1)

각 VU는 다음 플로우를 수행합니다:

1. **Join Queue**: `POST /queue/join` 호출
2. **Status Polling**: `GET /queue/status/{userId}` 반복 호출 (MATCHED까지 대기)
3. **ACK**: 매칭 성공 시 `POST /queue/ack` 호출

### 현실적인 사용자 행동 시뮬레이션 시나리오 (v2) - 2025.11.24

각 VU는 무한 루프로 다음 플로우를 반복 수행합니다:

1. **Join Queue**: `POST /queue/join` 호출
2. **Status Polling**: `GET /queue/status/{userId}` 반복 호출 (MATCHED까지 대기 또는 타임아웃)
3. **성공 시나리오**:
   - `POST /queue/ack` 호출
   - 30초~5분 랜덤 대기
   - 다음 매칭 시도로 이동
4. **타임아웃 시나리오**:
   - `POST /queue/leave` 호출 (큐에서 떠남)
   - 5~10초 랜덤 대기
   - 다음 매칭 시도로 이동
5. **RAMP_DOWN 단계**: 테스트 종료 시 자동으로 사용자 정리 (Leave 또는 ACK)

## 성능 기준 (Threshold)

다음 기준을 만족해야 테스트가 성공으로 간주됩니다:

- `http_req_duration`: 95% 요청이 500ms 이하
- `http_req_failed`: 에러율 1% 미만
- `match_success_rate`: 매칭 성공률 80% 이상

## 주요 메트릭

- `http_req_duration`: HTTP 요청 지연 시간
- `http_req_failed`: 실패한 요청 비율
- `match_success_rate`: 매칭 성공률
- `match_timeout_rate`: 타임아웃 비율
- `match_latency`: 매칭 소요 시간

## 트러블슈팅

자세한 트러블슈팅은 [트러블슈팅 가이드](./troubleshooting.md)를 참조하세요.

**빠른 해결:**

- Spring Boot 앱 연결 실패: `BASE_URL` 확인 (Docker: `http://host.docker.internal:8080`, 로컬: `http://localhost:8080`)
- 타임아웃 발생: `TIMEOUT` 환경변수 증가
- 매칭 성공률 낮음: Worker 실행 상태 및 큐 길이 확인

---

## 테스트 결과

### 테스트 환경

- VU: 50명
- DURATION: 1분
- RAMP_UP: 30초
- RAMP_DOWN: 30초
- TIMEOUT: 30초

### 테스트 1: 초기 상태 (개선 전)

| 메트릭                  | 값               | 목표   | 상태 |
| ----------------------- | ---------------- | ------ | ---- |
| match_success_rate      | 41.98%           | >80%   | 실패 |
| match_timeout_rate      | 100.00% (9713건) | -      | 실패 |
| http_req_duration p(95) | 13.42ms          | <500ms | 성공 |
| http_req_failed         | 1.83%            | <1%    | 실패 |

**주요 문제점:**

- 매칭 성공률이 매우 낮음 (41.98%)
- 타임아웃이 대량 발생 (9,713건)
- Worker가 한 번에 한 쌍만 매칭하여 처리 속도가 느림

### 테스트 2: 개선 후 (Worker 최적화)

**개선 사항:**

1. Worker가 한 tick에서 최대 10쌍 매칭 (이전: 1쌍)
2. 자동 정리 로직 추가 (20초 이상 대기 사용자 자동 제거)

| 메트릭                  | 이전          | 현재          | 변화     | 목표   | 상태      |
| ----------------------- | ------------- | ------------- | -------- | ------ | --------- |
| match_success_rate      | 41.98%        | 68.79%        | +26.81%p | >80%   | 개선 필요 |
| match_timeout_rate      | 100% (9713건) | 100% (2224건) | -77%     | -      | 개선 필요 |
| http_req_duration p(95) | 13.42ms       | 2.07s         | +154x    | <500ms | 실패      |
| http_req_failed         | 1.83%         | 3.88%         | +2.05%p  | <1%    | 실패      |

**개선된 부분:**

- 매칭 성공률이 크게 개선됨 (41.98% → 68.79%)
- 타임아웃 수가 대폭 감소함 (9,713건 → 2,224건, -77%)

**여전히 문제인 부분:**

- 매칭 성공률이 목표(80%)에 미달 (68.79%)
- HTTP 요청 지연이 크게 증가함 (p(95) 13.42ms → 2.07s)
- HTTP 실패율이 증가함 (1.83% → 3.88%)

**원인 분석:**

- Postgres INSERT가 동기적으로 실행되어 Worker tick 시간 증가
- Worker가 한 번에 여러 쌍을 매칭하면서 부하 증가

### 테스트 3: 현실적인 사용자 행동 시뮬레이션 (v2) - 2025년 11월

**개선 사항:**

1. 매칭 성공 후 30초~5분 랜덤 대기
2. 타임아웃 시 큐에서 떠나고 재시도 전 5~10초 대기
3. 연속 매칭 시도 (무한 루프)
4. RAMP_DOWN 단계에서 자동으로 사용자 정리 (타임아웃 0.5초)
5. k6 Rate 메트릭 수정: `matchTimeoutRate.add(-1)` 제거, 타임아웃 시 실제 상태 확인 후 메트릭 기록

**테스트 환경:**

- VU: 1000명
- DURATION: 2분
- RAMP_UP: 30초
- RAMP_DOWN: 30초
- TIMEOUT: 30초
- MATCH_SUCCESS_WAIT_MIN: 30초
- MATCH_SUCCESS_WAIT_MAX: 300초 (5분)
- TIMEOUT_RETRY_WAIT_MIN: 5초
- TIMEOUT_RETRY_WAIT_MAX: 10초

**테스트 결과:**

| 메트릭                  | 값              | 목표   | 상태 |
| ----------------------- | --------------- | ------ | ---- |
| match_success_rate      | 99.02% (1118건) | >80%   | 통과 |
| match_timeout_rate      | 100.00% (11건)  | -      | -    |
| http_req_duration p(95) | 9.42ms          | <500ms | 통과 |
| http_req_failed         | 0.08%           | <1%    | 통과 |
| match_latency p(95)     | 823ms           | -      | -    |

**상세 결과:**

- 총 iterations: 86,289건
- 총 HTTP 요청: 93,787건
- 평균 iteration duration: 243.47ms
- 실행 시간: 2분 29.8초
- 모든 threshold 통과

**분석:**

- 매칭 성공률이 목표(80%)를 크게 상회 (99.02%)
- HTTP 요청 지연 시간이 매우 낮음 (p(95) 9.42ms)
- HTTP 실패율이 매우 낮음 (0.08%)
- 타임아웃 발생 건수는 매우 적음 (11건)
- 현실적인 사용자 행동 시뮬레이션이 효과적으로 작동

---
