# k6 동시성 테스트 스크립트

## 사용 방법

### 1. 사용자 데이터 준비

`k6/users_rows.csv`를 준비합니다. 첫 열은 `id`(UUID)여야 하고, 헤더 예시는 다음과 같습니다:

```
id,nickname,gender,birth_date,region,total_score,tier,created_at,updated_at,deleted_at
00000000-0000-0000-0000-000000002000,load_user_1000,FEMALE,1995-01-01,SEOUL,0,SPROUT,...
```

seed 후 Supabase에서 `users`를 CSV로 추출하거나 동일 형식으로 만듭니다. 1000 VU 1:1 매핑 시 1000건 이상 권장.

### 2. k6 실행 방법

**⚠️ 중요:** Grafana k6 Overview 대시보드(`grafana/dashboards/k6_overview.json`)는 **k6가 Prometheus에 `k6_*` 메트릭을 보낼 때만** 데이터가 표시됩니다.  
**기본 k6 바이너리**와 **grafana/k6 Docker 이미지**에는 `experimental-prometheus-rw` 출력이 **포함되어 있지 않습니다**.  
사용 가능한 출력: `cloud`, `csv`, `influxdb`, `json`, `statsd`.

#### 2‑1. 확장 없이 실행 (Grafana 대시보드 미사용)

```bash
# API URL (선택, 기본: http://localhost:8080/api/v1/match)
export API_URL="http://localhost:8080/api/v1/match"

k6 run concurrent_match_test.js
```

- 콘솔에 응답시간(avg, p95, p99), 실패율, RPS 등이 출력됩니다.
- (선택) 요약만 JSON으로 저장:  
  `k6 run --summary-export=summary.json concurrent_match_test.js`
- **⚠️ Grafana k6 Overview 대시보드에는 데이터가 표시되지 않습니다.** (Prometheus에 `k6_*` 메트릭이 없기 때문)

#### 2‑2. Prometheus 실시간 연동 (Grafana 대시보드 사용 시 필수)

**Grafana k6 Overview 대시보드에 데이터를 표시하려면** `experimental-prometheus-rw` 출력이 필요합니다.  
이를 사용하려면 **xk6-output-prometheus-remote** 확장을 넣은 **커스텀 k6 바이너리**가 필요합니다.

**1) xk6로 커스텀 k6 빌드**

```bash
# Go 설치 후
xk6 build --with github.com/grafana/xk6-output-prometheus-remote
# 생성된 ./k6 (또는 ./k6.exe) 사용
```

Docker로 빌드 (Go 없을 때):

```bash
docker run --rm -u "$(id -u):$(id -g)" -v "${PWD}:/xk6" -w /xk6 grafana/xk6 build \
  --with github.com/grafana/xk6-output-prometheus-remote
# Windows PowerShell: (id -u) 대신 1000, (id -g) 대신 1000 등 해당 값 사용
```

**2) 환경 변수와 실행**

**Windows PowerShell (권장):**
```powershell
# 실행 스크립트 사용 (자동으로 환경 변수 설정)
.\run_test.ps1

# 또는 수동 설정
$env:K6_PROMETHEUS_RW_SERVER_URL="http://localhost:9090/api/v1/write"
$env:K6_PROMETHEUS_RW_PUSH_INTERVAL="5s"
$env:K6_PROMETHEUS_RW_TREND_STATS="avg,p(90),p(95),p(99)"
$env:API_URL="http://localhost:8080/api/v1/match"  # 선택사항, 기본값 사용 시 생략 가능
.\k6.exe run --out experimental-prometheus-rw concurrent_match_test.js
```

**Linux/macOS:**
```bash
export K6_PROMETHEUS_RW_SERVER_URL="http://localhost:9090/api/v1/write"
export K6_PROMETHEUS_RW_PUSH_INTERVAL="5s"
export K6_PROMETHEUS_RW_TREND_STATS="avg,p(90),p(95),p(99)"
export API_URL="http://localhost:8080/api/v1/match"  # 선택사항
./k6 run --out experimental-prometheus-rw concurrent_match_test.js
```

- Prometheus는 `--web.enable-remote-write-receiver` 플래그 필요. `compose.yml`에 이미 포함되어 있으면 `http://localhost:9090/api/v1/write` 사용.
- Docker 내부에서 호스트 Prometheus로 보낼 때:  
  `K6_PROMETHEUS_RW_SERVER_URL="http://host.docker.internal:9090/api/v1/write"`  
  단, **grafana/k6** 이미지에는 이 확장이 없으므로, **위에서 빌드한 커스텀 k6 바이너리**를 쓰는 Docker 이미지를 따로 만든 뒤 실행해야 합니다.

**✅ 실행 후 확인:**
- Prometheus: `http://localhost:9090/graph`에서 `k6_http_req_duration_bucket`, `k6_http_reqs`, `k6_http_req_failed`, `k6_vus` 등 `k6_*` 메트릭이 보여야 합니다.
- Grafana: k6 Overview 대시보드에 응답시간, RPS, 실패율, VU 수가 표시됩니다.

## 설정 변경

### 동시 사용자 수 변경

`concurrent_match_test.js` 파일에서 `vus` 값을 수정:

```javascript
export const options = {
  scenarios: {
    concurrent_match: {
      executor: 'constant-vus',
      vus: 1000, // 여기 수정
      duration: '1m',
    },
  },
};
```

### 테스트 지속 시간 변경

`duration` 값을 수정:

```javascript
duration: '1m', // 예: '30s', '2m', '5m'
```

## 참고

- k6 공식 문서: https://k6.io/docs/
- Prometheus Remote Write (xk6 확장): https://k6.io/docs/results-output/real-time/prometheus-remote-write/  
  → `experimental-prometheus-rw`는 **xk6-output-prometheus-remote** 포함 빌드에서만 사용 가능.
- `grafana/dashboards/k6_overview.json`(k6 Overview)의 응답시간·RPS 등 패널은 **experimental-prometheus-rw**로 메트릭을 보낼 때만 Prometheus에 데이터가 쌓입니다.
- `invalid output type 'experimental-prometheus-rw', available types are: cloud, csv, influxdb, json, statsd` 에러는 기본 k6에 해당 출력이 없을 때 발생합니다. 위 “2‑2. Prometheus 실시간 연동”대로 xk6로 빌드한 바이너리를 사용하세요.
