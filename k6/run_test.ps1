# k6 동시성 테스트 실행 스크립트 (Windows PowerShell)
# Prometheus 연동 포함

Write-Host "=== k6 동시성 테스트 실행 ===" -ForegroundColor Cyan

# 환경 변수 설정
$env:K6_PROMETHEUS_RW_SERVER_URL = "http://localhost:9090/api/v1/write"
$env:K6_PROMETHEUS_RW_PUSH_INTERVAL = "5s"
$env:K6_PROMETHEUS_RW_TREND_STATS = "avg,p(90),p(95),p(99)"

# API URL 설정 (기본값: localhost:8080)
if (-not $env:API_URL) {
    $env:API_URL = "http://localhost:8080/api/v1/match"
    Write-Host "API_URL가 설정되지 않아 기본값 사용: $env:API_URL" -ForegroundColor Yellow
} else {
    Write-Host "API_URL: $env:API_URL" -ForegroundColor Green
}

# Spring Boot 앱 연결 확인 (Actuator health 엔드포인트 사용)
Write-Host "`nSpring Boot 앱 연결 확인 중..." -ForegroundColor Cyan
$healthUrl = ($env:API_URL -replace '/api/v1/match$', '') + '/actuator/health'
if ($healthUrl -eq '/actuator/health') {
    $healthUrl = 'http://localhost:8080/actuator/health'
}
try {
    $appCheck = Invoke-WebRequest -Uri $healthUrl -Method GET -UseBasicParsing -TimeoutSec 2 -ErrorAction Stop
    if ($appCheck.StatusCode -eq 200) {
        Write-Host "✅ Spring Boot 앱 연결 확인됨 ($healthUrl)" -ForegroundColor Green
    } else {
        Write-Host "⚠️  Spring Boot 앱 응답 이상: Status $($appCheck.StatusCode)" -ForegroundColor Yellow
    }
} catch {
    Write-Host "⚠️  Spring Boot 앱 연결 실패: $healthUrl" -ForegroundColor Yellow
    Write-Host "   앱이 실행 중인지 확인하세요. 테스트는 계속 진행됩니다." -ForegroundColor Yellow
}

Write-Host "`n환경 변수:" -ForegroundColor Cyan
Write-Host "  K6_PROMETHEUS_RW_SERVER_URL: $env:K6_PROMETHEUS_RW_SERVER_URL"
Write-Host "  K6_PROMETHEUS_RW_PUSH_INTERVAL: $env:K6_PROMETHEUS_RW_PUSH_INTERVAL"
Write-Host "  K6_PROMETHEUS_RW_TREND_STATS: $env:K6_PROMETHEUS_RW_TREND_STATS"
Write-Host "  API_URL: $env:API_URL"

# k6.exe 존재 확인
if (-not (Test-Path ".\k6.exe")) {
    Write-Host "`n❌ k6.exe를 찾을 수 없습니다!" -ForegroundColor Red
    Write-Host "xk6로 빌드한 k6.exe가 필요합니다. k6/README.md 참고." -ForegroundColor Yellow
    exit 1
}

# users_rows.csv 존재 확인
if (-not (Test-Path ".\users_rows.csv")) {
    Write-Host "`n❌ users_rows.csv를 찾을 수 없습니다!" -ForegroundColor Red
    exit 1
}

Write-Host "`n✅ k6.exe 및 users_rows.csv 확인 완료" -ForegroundColor Green
Write-Host "`n테스트 시작..." -ForegroundColor Cyan
Write-Host ""

# k6 실행
.\k6.exe run --out experimental-prometheus-rw concurrent_match_test.js

Write-Host "`n=== 테스트 완료 ===" -ForegroundColor Cyan
Write-Host "`n확인 사항:" -ForegroundColor Yellow
Write-Host "  1. Prometheus: http://localhost:9090/graph 에서 'k6_' 접두사 메트릭 확인"
Write-Host "  2. Grafana: http://localhost:3000 에서 'k6 Overview' 대시보드 확인"
Write-Host "     (로그인: admin/admin)"
