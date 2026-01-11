# 트러블슈팅 가이드

## Spring Boot 애플리케이션

### 애플리케이션이 시작되지 않음

**증상**: 애플리케이션 실행 시 에러 발생

**해결 방법**:

1. PostgreSQL이 실행 중인지 확인:
   - Swarm 모드: `docker service ls` 또는 `docker service ps matching-poc_db`
   - 일반 Compose: `docker compose ps db`
2. Redis가 실행 중인지 확인:
   - Swarm 모드: `docker service ps matching-poc_redis`
   - 일반 Compose: `docker compose ps redis`
3. 포트 충돌 확인 (8080, 5432, 6379)
4. 로그 확인: `logs/application.log`

### 데이터베이스 연결 실패

**증상**: `Connection refused` 또는 `Connection timeout`

**해결 방법**:

1. `application.yaml`의 데이터베이스 설정 확인
2. PostgreSQL이 실행 중인지 확인:
   - Swarm 모드: `docker service ps matching-poc_db`
   - 일반 Compose: `docker compose ps db`
3. 네트워크 연결 확인:
   - Swarm 모드: `docker service logs matching-poc_db`
   - 일반 Compose: `docker compose exec db psql -U myuser -d mydatabase`

### Redis 연결 실패

**증상**: Redis 관련 에러

**해결 방법**:

1. Redis가 실행 중인지 확인:
   - Swarm 모드: `docker service ps matching-poc_redis`
   - 일반 Compose: `docker compose ps redis`
2. Redis 연결 테스트:
   - Swarm 모드: `docker service logs matching-poc_redis`
   - 일반 Compose: `docker compose exec redis redis-cli ping`
3. `application.yaml`의 Redis 설정 확인

## k6 테스트

### k6가 Spring Boot 앱에 연결할 수 없음

**증상**: `Connection refused` 또는 타임아웃

**해결 방법**:

1. Spring Boot 앱이 실행 중인지 확인
2. Docker에서 실행하는 경우: `BASE_URL=http://host.docker.internal:8080`
3. 로컬에서 실행하는 경우: `BASE_URL=http://localhost:8080`
4. 방화벽 설정 확인

### 테스트 사용자 파일 생성 실패

**증상**: `test-users.json` 파일이 생성되지 않음

**해결 방법**:

1. `scripts/k6/` 디렉토리 권한 확인
2. Docker 볼륨 마운트 확인
3. `.env` 파일의 `VU` 환경변수 확인

### 매칭 성공률이 낮음

**증상**: Threshold 실패 (match_success_rate < 80%)

**해결 방법**:

1. Worker가 정상 실행 중인지 확인 (Spring Boot 로그에서 "MatchingWorker started" 확인)
2. 큐에 충분한 사용자가 있는지 확인 (Grafana 대시보드에서 Queue Length 확인)
3. 성별 비율 확인 (테스트 사용자 생성 로직 확인, 50:50)
4. Redis 상태 확인 (메모리 사용량, 연결 상태)
5. 타임아웃 설정 확인 (`TIMEOUT` 환경변수, 기본값: 30초)
6. 사용자 행동 시뮬레이션 설정 확인 (매칭 성공 후 대기 시간 등)

### k6 Rate 메트릭이 잘못된 값 표시

**증상**: `match_timeout_rate` 또는 `match_success_rate`가 예상과 다른 값 표시

**원인:**

- k6의 `Rate` 메트릭은 0이 아닌 모든 값(양수 및 음수 포함)을 성공으로 카운트합니다.
- `matchTimeoutRate.add(-1)` 같은 패턴은 메트릭을 왜곡시킵니다.

**해결 방법:**

1. `add(-1)` 패턴 제거
2. 각 iteration마다 명시적으로 `add(0)` 또는 `add(1)` 호출
3. 타임아웃 시 실제 상태 확인 후 올바른 메트릭 기록

**예시:**

```javascript
// 잘못된 방법
matchTimeoutRate.add(1); // 타임아웃 기록
// ... 나중에 성공하면
matchTimeoutRate.add(-1); // 취소 시도 (하지만 -1도 성공으로 카운트됨)

// 올바른 방법
if (matched) {
  matchSuccessRate.add(1);
  matchTimeoutRate.add(0);
} else {
  matchSuccessRate.add(0);
  matchTimeoutRate.add(1);
}
```

### RAMP_DOWN 단계에서 타임아웃 발생

**증상**: 테스트 종료 시 많은 타임아웃 에러 발생

**원인:**

- RAMP_DOWN 단계에서 많은 VU가 동시에 cleanup을 시도
- 서버 부하로 인한 타임아웃

**해결 방법:**

1. cleanup 타임아웃을 짧게 설정 (기본값: 0.5초)
2. cleanup 로직을 단순화 (status 확인 제거, 바로 leave/ack 시도)
3. 에러 발생 시 무시하고 계속 진행 (try-catch 사용)

**예시:**

```javascript
function cleanupUser(userId, timeout = '0.5s') {
    // status 확인 없이 바로 leave 시도
    try {
        http.post(`${BASE_URL}/queue/leave`, ...);
    } catch (e) {
        // leave 실패 시 ack 시도
        try {
            http.post(`${BASE_URL}/queue/ack`, ...);
        } catch (e2) {
            // 둘 다 실패해도 무시
        }
    }
}
```

### 같은 사용자가 여러 번 cleanup됨

**증상**: 로그에서 같은 사용자 ID가 여러 번 cleanup되는 것처럼 보임

**원인:**

- 여러 VU가 같은 사용자를 cleanup하려고 시도
- 또는 cleanup 로직이 중복 실행

**해결 방법:**

1. cleanup 로직을 `finally` 블록에 한 번만 배치
2. cleanup 시 에러를 무시하고 계속 진행 (이미 cleanup된 사용자는 에러 발생)
3. `console.log` 제거하여 중복 로그 방지

### 타임아웃 후에도 큐에 사용자가 남아있음

**증상**: 타임아웃 후에도 사용자가 큐에 남아있어 다음 매칭 시도 시 409 에러 발생

**원인:**

- 타임아웃 시 `LeaveQueue`를 호출하지 않음

**해결 방법:**

1. 타임아웃 시 `POST /queue/leave` 호출 추가
2. 현실적인 사용자 행동 시뮬레이션: 타임아웃 시 큐에서 떠남

**예시:**

```javascript
if (actualStatus === 'WAITING') {
    http.post(`${BASE_URL}/queue/leave`, ...);
}
```

### 테스트가 너무 빠르게 종료됨

**증상**: 예상보다 빠르게 테스트가 종료되거나 iteration 수가 매우 높음

**원인:**

- 타임아웃 후 재시도 전 대기 시간이 없음
- 매칭 성공 후 대기 시간이 없음

**해결 방법:**

1. 타임아웃 후 재시도 전 대기 시간 추가 (5~10초)
2. 매칭 성공 후 대기 시간 추가 (30초~5분)
3. 현실적인 사용자 행동 시뮬레이션 환경변수 설정 확인

**환경변수:**

- `MATCH_SUCCESS_WAIT_MIN`: 매칭 성공 후 최소 대기 시간 (기본값: 30초)
- `MATCH_SUCCESS_WAIT_MAX`: 매칭 성공 후 최대 대기 시간 (기본값: 300초)
- `TIMEOUT_RETRY_WAIT_MIN`: 타임아웃 후 재시도 전 최소 대기 시간 (기본값: 5초)
- `TIMEOUT_RETRY_WAIT_MAX`: 타임아웃 후 재시도 전 최대 대기 시간 (기본값: 10초)

## Prometheus

### 메트릭이 수집되지 않음

**증상**: Prometheus에서 메트릭이 표시되지 않음

**해결 방법**:

1. Prometheus 타겟 상태 확인
   - Prometheus UI > Status > Targets
   - `spring-boot-app` 타겟이 "UP" 상태인지 확인
2. Spring Boot Actuator 엔드포인트 확인
   - `http://localhost:8080/actuator/prometheus` 접속 가능한지 확인
3. 네트워크 연결 확인
   - Docker에서 실행하는 경우 `host.docker.internal` 사용
   - Linux의 경우 호스트 IP 주소 사용
4. `prometheus.yml` 설정 확인

### Linux에서 메트릭 수집 실패

**증상**: Linux 환경에서 `host.docker.internal`이 동작하지 않음

**해결 방법**:

1. Docker 브리지 IP 확인: `ip addr show docker0`
2. `prometheus.yml`의 target을 IP 주소로 변경: `172.17.0.1:8080`
3. 또는 Docker 네트워크 모드 host 사용

## Grafana

### 대시보드가 표시되지 않음

**증상**: Grafana에서 대시보드가 보이지 않음

**해결 방법**:

1. 프로비저닝 설정 확인: `grafana/provisioning/dashboards/dashboard.yml`
2. 대시보드 JSON 파일 확인: `grafana/dashboards/matching-system.json`
3. 볼륨 마운트 확인: `compose.yml`의 grafana 서비스 볼륨 설정
4. Grafana 로그 확인:
   - Swarm 모드: `docker service logs matching-poc_grafana`
   - 일반 Compose: `docker compose logs grafana`

### 데이터소스 연결 실패

**증상**: Grafana에서 Prometheus 데이터를 가져올 수 없음

**해결 방법**:

1. Prometheus가 실행 중인지 확인
2. 데이터소스 설정 확인: Grafana > Configuration > Data Sources, URL이 `http://prometheus:9090`인지 확인
3. 네트워크 연결 확인: 같은 Docker 네트워크에 있는지 확인
4. `grafana/provisioning/datasources/prometheus.yml` 확인

### 메트릭 값이 0으로 표시됨

**증상**: 대시보드에 메트릭이 0으로 표시됨

**해결 방법**:

1. Prometheus에서 직접 쿼리하여 확인: `matching_match_queue_length`
2. Spring Boot 앱이 실행 중인지 확인
3. Worker가 정상 동작하는지 확인
4. 시간 범위 확인 (과거 데이터 조회 시)

## 로그 확인

### Spring Boot 로그

```bash
tail -f logs/application.log
```

### Docker 로그

**Swarm 모드:**

```bash
# 전체 서비스 목록
docker service ls

# 특정 서비스 로그
docker service logs matching-poc_prometheus
docker service logs matching-poc_grafana
docker service logs matching-poc_redis
```

**일반 Docker Compose:**

```bash
# 전체 로그
docker compose logs

# 특정 서비스 로그
docker compose logs prometheus
docker compose logs grafana
docker compose logs redis
```

### k6 로그

k6 실행 시 콘솔에 실시간으로 출력됩니다.

## 성능 최적화

### Worker Tick Latency가 높음

1. Redis 연결 풀 설정 확인
2. 데이터베이스 쿼리 최적화
3. JVM 힙 메모리 증가

### Redis Latency가 높음

1. Redis 메모리 사용량 확인
2. Redis 연결 수 확인
3. 네트워크 지연 확인

### Queue Length가 계속 증가

1. Worker가 정상 실행 중인지 확인
2. 매칭 조건 확인 (성별 비율 등)
3. 시스템 리소스 확인 (CPU, 메모리)

## 추가 확인 사항

문제가 지속되면 다음을 확인하세요:

1. 모든 서비스가 실행 중인지 확인:
   - Swarm 모드: `docker service ls`
   - 일반 Compose: `docker compose ps`
2. 포트 충돌 확인
3. 시스템 리소스 확인 (CPU, 메모리, 디스크)
4. 네트워크 연결 확인
