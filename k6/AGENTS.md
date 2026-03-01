<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-03-01 | Updated: 2026-03-01 -->

# k6

## Purpose
k6 부하 테스트 스크립트 디렉토리. 매칭 API의 동시 요청 처리 성능과 동시성 안전성을 검증하기 위한 시나리오를 포함한다.

## Key Files

| File | Description |
|------|-------------|
| `concurrent_match_test.js` | 동시 매칭 요청 부하 테스트 시나리오 (Virtual Users 기반) |
| `run_test.ps1` | Windows PowerShell 테스트 실행 스크립트 |
| `users_rows.csv` | 테스트용 사용자 데이터 (CSV 형식, VU당 userId 할당) |
| `README.md` | k6 테스트 실행 방법 및 시나리오 설명 |

## For AI Agents

### Working In This Directory
- k6가 로컬에 설치되어 있어야 함: `k6 run k6/concurrent_match_test.js`
- `users_rows.csv`의 userId는 실제 DB에 존재하는 사용자여야 함 (시드 데이터 필요).
- 테스트 실행 전 애플리케이션이 기동 중이어야 함.
- 부하 테스트 결과는 `docs/concurrency_testing.md`에 기록.

### Testing Requirements
- VU(Virtual User) 수, 지속 시간, 임계값(`thresholds`)은 `concurrent_match_test.js` 내 `options` 객체에서 조정.
- Grafana 대시보드를 열어놓고 테스트 실행 시 실시간 메트릭 확인 가능.

### Common Patterns
- k6 SharedArray로 CSV 사용자 데이터를 로드하여 VU 간 공유.
- `http.post`로 `/api/v1/match` 엔드포인트에 동시 요청 발송.

## Dependencies

### External
- k6 (https://k6.io) — 부하 테스트 프레임워크

<!-- MANUAL: -->
