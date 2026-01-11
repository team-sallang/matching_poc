# 환경 설정 가이드

## 사전 요구사항

- Java 17 이상
- Gradle 8.14.3 이상
- Docker

## 실행 방법

### 1. 인프라 시작

#### 방법 1: Docker Swarm 모드 (권장 - 리소스 제한 적용)

```bash
# Swarm 모드 초기화 (최초 1회만)
make swarm-init

# 스택 배포
make swarm-start

# 서비스 상태 확인
make swarm-status
```

또는 직접 명령어 사용:

```bash
docker swarm init
docker stack deploy -c compose.yml matching-poc
docker service ls
```

#### 방법 2: 일반 Docker Compose (선택사항)

일반 Docker Compose를 사용할 수도 있지만, 리소스 제한이 적용되지 않습니다:

```bash
docker compose up -d db redis prometheus loki promtail grafana
```

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

## Docker 명령어

### Swarm 모드 사용 시

```bash
# 스택 배포
make swarm-start
# 또는
docker stack deploy -c compose.yml matching-poc

# 스택 제거
make swarm-stop
# 또는
docker stack rm matching-poc

# 서비스 상태 확인
make swarm-status
# 또는
docker service ls

# 특정 서비스 로그 확인
docker service logs matching-poc_db
docker service logs matching-poc_redis
```

### 일반 Docker Compose 사용 시 (선택사항)

Swarm 모드를 사용하지 않는 경우:

```bash
# 전체 인프라 시작
docker compose up -d

# 서비스 중지
docker compose stop

# 볼륨까지 삭제하며 중지
docker compose down -v
```

**참고:** 일반 Docker Compose에서는 리소스 제한이 적용되지 않습니다.

## 네트워크 설정

### host.docker.internal 접근

Swarm 모드에서는 `host.docker.internal`이 기본적으로 작동하지 않을 수 있습니다. 다음 방법으로 해결합니다:

#### Prometheus (로컬 Spring Boot 접근)

- **Swarm 모드**: `compose.yml`의 Prometheus 서비스에 `extra_hosts`가 자동으로 추가되어 `host.docker.internal` 접근 가능
- **일반 Compose**: `prometheus.yml`에서 `host.docker.internal:8080` 사용

#### k6 (부하 테스트)

- **Swarm 모드**: `make test` 실행 시 `--add-host "host.docker.internal:host-gateway"` 옵션이 자동으로 적용됨
- **일반 Compose**: `docker run` 시 `--add-host "host.docker.internal:host-gateway"` 옵션 사용

#### Linux 환경

Linux에서는 `host.docker.internal`이 동작하지 않을 수 있습니다:

1. Docker 브리지 IP 확인: `ip addr show docker0`
2. `prometheus.yml`의 target을 IP 주소로 변경 (예: `172.17.0.1:8080`)
3. 또는 호스트 IP 주소를 직접 사용

## 포트 정보

| 서비스      | 포트 | URL                   |
| ----------- | ---- | --------------------- |
| Spring Boot | 8080 | http://localhost:8080 |
| PostgreSQL  | 5432 | localhost:5432        |
| Redis       | 6379 | localhost:6379        |
| Prometheus  | 9090 | http://localhost:9090 |
| Grafana     | 3000 | http://localhost:3000 |
| Loki        | 3100 | http://localhost:3100 |

## 리소스 권장사항

### Docker 서비스 리소스 권장사항

| 서비스      | CPU   | 메모리 | 비고                         |
| ----------- | ----- | ------ | ---------------------------- |
| PostgreSQL  | 2코어 | 2GB    | 데이터베이스 서버            |
| Redis       | 1코어 | 1GB    | 캐시/큐 서버                 |
| Spring Boot | -     | -      | 로컬 실행 (리소스 제한 없음) |

### 리소스 제한 적용 방법

#### Docker Swarm 모드 사용 (권장)

Swarm 모드를 사용하면 `compose.yml`의 `deploy.resources`가 자동으로 적용됩니다:

```bash
# Swarm 모드 초기화 (최초 1회)
make swarm-init

# 스택 배포 (리소스 제한 자동 적용)
make swarm-start
```

**적용된 리소스 제한:**

- **PostgreSQL**:**
  - Limits: CPU 2코어, 메모리 2GB
  - Reservations: CPU 0.5코어, 메모리 512MB

- **Redis:**
  - Limits: CPU 1코어, 메모리 1GB
  - Reservations: CPU 0.25코어, 메모리 256MB

**참고:**

- Swarm 모드에서는 `deploy.resources`가 적용되어 리소스 제한이 자동으로 설정됩니다.
- 일반 `docker compose up`에서는 리소스 제한이 적용되지 않습니다.
- Swarm 모드에서는 `container_name`, `restart` 옵션이 `deploy` 섹션으로 변경되었습니다.

#### k6 부하 테스트 실행

```bash
make test
```

이 명령은 docker run을 사용하여 k6를 실행하며, `host.docker.internal` 접근을 위해 `--add-host` 옵션이 자동으로 적용됩니다.

## 트러블슈팅

자세한 트러블슈팅은 [트러블슈팅 가이드](./troubleshooting.md)를 참조하세요.
