<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-03-01 | Updated: 2026-03-01 -->

# grafana

## Purpose
Grafana 대시보드 정의와 프로비저닝 설정. Docker Compose 기동 시 자동으로 데이터소스(Prometheus, Loki, Tempo)와 대시보드가 프로비저닝된다.

## Subdirectories

| Directory | Purpose |
|-----------|---------|
| `dashboards/` | Grafana 대시보드 JSON 파일 |
| `provisioning/` | Grafana 자동 프로비저닝 설정 (datasources, dashboards) |

## Key Files

| File | Description |
|------|-------------|
| `dashboards/matching_dashboard.json` | 매칭 서비스 메트릭 대시보드 — 큐 대기 수, 매칭 성공/실패 카운터, HTTP 레이턴시, 트레이스 링크 |
| `provisioning/dashboards/dashboards.yml` | 대시보드 프로비저닝 경로 설정 |
| `provisioning/datasources/datasources.yml` | Prometheus·Loki·Tempo 데이터소스 자동 등록 |

## For AI Agents

### Working In This Directory
- 대시보드 수정은 Grafana UI에서 편집 후 JSON을 Export하여 `dashboards/` 에 덮어쓰는 방식 권장.
- `datasources.yml`의 URL은 `compose.yml`의 서비스 이름과 일치해야 함.
- 신규 패널 추가 시 Prometheus 메트릭 이름이 `application.yaml`의 `management.observations` 설정과 일치하는지 확인.

### Common Patterns
- 애플리케이션 커스텀 메트릭: `match.request.immediate`, `match.request.enqueued`, `match.confirm.success`, `match.confirm.conflict`, `match.queue.waiting.count`
- HTTP 서버 메트릭: `http.server.requests` (히스토그램, 퍼센타일 포함)

## Dependencies

### External
- Grafana (Docker 이미지)
- Prometheus — 메트릭 소스
- Loki — 로그 소스
- Tempo — 트레이스 소스

<!-- MANUAL: -->
