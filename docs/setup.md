# 환경 설정 가이드

## 사전 요구사항

- Java 17 이상
- Gradle 8.14.3 이상
- Docker

## 실행 방법

### 1. 인프라 시작 (Docker Compose)

```bash
docker compose up -d db redis prometheus loki promtail grafana
```

시작되는 서비스:

- PostgreSQL (포트: 5432)
- Redis (포트: 6379)
- Prometheus (포트: 9090)
- Loki (포트: 3100)
- Promtail
- Grafana (포트: 3000)

### 2. Spring Boot 애플리케이션 실행

```bash
./gradlew bootRun
```

또는 IDE에서 `MatchingPocApplication`을 실행합니다.

애플리케이션은 `http://localhost:8080`에서 실행됩니다.

### 확인 사항

- Spring Boot Actuator: `http://localhost:8080/actuator/health`
- Prometheus 메트릭: `http://localhost:8080/actuator/prometheus`
- Swagger UI: `http://localhost:8080/swagger-ui.html`

## Docker Compose 명령어

### 전체 인프라 시작

```bash
docker compose up -d
```

### 특정 서비스만 시작

```bash
# 데이터베이스와 Redis만
docker compose up -d db redis

# 모니터링 스택만
docker compose up -d prometheus loki promtail grafana
```

### 서비스 중지

```bash
# 볼륨 유지하며 중지
docker compose stop

# 볼륨까지 삭제하며 중지
docker compose down -v
```

## 네트워크 설정

### Windows/Mac

`prometheus.yml`에서 `host.docker.internal:8080`을 사용하여 로컬에서 실행 중인 Spring Boot 앱에 접근합니다.

### Linux

Linux에서는 `host.docker.internal`이 동작하지 않을 수 있습니다:

1. Docker 브리지 IP 확인: `ip addr show docker0`
2. `prometheus.yml`의 target을 IP 주소로 변경 (예: `172.17.0.1:8080`)
3. 또는 Docker 네트워크 모드 host 사용

## 포트 정보

| 서비스      | 포트 | URL                   |
| ----------- | ---- | --------------------- |
| Spring Boot | 8080 | http://localhost:8080 |
| PostgreSQL  | 5432 | localhost:5432        |
| Redis       | 6379 | localhost:6379        |
| Prometheus  | 9090 | http://localhost:9090 |
| Grafana     | 3000 | http://localhost:3000 |
| Loki        | 3100 | http://localhost:3100 |

## 트러블슈팅

자세한 트러블슈팅은 [트러블슈팅 가이드](./troubleshooting.md)를 참조하세요.
