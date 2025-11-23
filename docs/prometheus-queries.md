# Prometheus 쿼리 가이드

## 사용 가능한 메트릭

### Counter 메트릭

- `matching_match_success_count_total` - 매칭 성공 횟수
- `matching_match_fail_count_total` - 매칭 실패 횟수

### Gauge 메트릭

- `matching_match_queue_length` - 현재 큐 길이

### Summary 메트릭 (Timer)

- `matching_worker_tick_latency_seconds_*` - Worker tick 지연 시간
- `matching_redis_zrange_latency_seconds_*` - ZRANGE 지연 시간
- `matching_redis_lua_latency_seconds_*` - Lua Script 지연 시간

## 기본 쿼리 예제

### 현재 큐 길이

```
matching_match_queue_length
```

### 매칭 성공률 (%)

```
(rate(matching_match_success_count_total[1m]) / (rate(matching_match_success_count_total[1m]) + rate(matching_match_fail_count_total[1m]))) * 100
```

### Worker Tick 평균 지연 시간 (ms)

```
(rate(matching_worker_tick_latency_seconds_sum[5m]) / rate(matching_worker_tick_latency_seconds_count[5m])) * 1000
```

### Worker Tick 최대 지연 시간 (ms)

```
matching_worker_tick_latency_seconds_max * 1000
```

### Redis ZRANGE 평균 지연 시간 (ms)

```
(rate(matching_redis_zrange_latency_seconds_sum[5m]) / rate(matching_redis_zrange_latency_seconds_count[5m])) * 1000
```

### Redis Lua Script 평균 지연 시간 (ms)

```
(rate(matching_redis_lua_latency_seconds_sum[5m]) / rate(matching_redis_lua_latency_seconds_count[5m])) * 1000
```

## Prometheus UI 사용 방법

1. 브라우저에서 접속: http://localhost:9090
2. Graph 탭 클릭
3. 쿼리 입력창에 위의 쿼리 중 하나를 입력
4. Execute 버튼 클릭 또는 Enter 키 입력
5. Graph 또는 Console 탭에서 결과 확인

## 문제 해결

자세한 트러블슈팅은 [트러블슈팅 가이드](./troubleshooting.md)를 참조하세요.

**빠른 해결:**

- 쿼리 결과가 비어있음: 시간 범위 확인, 타겟 상태 확인 (Status > Targets)
- 자동완성이 작동하지 않음: 메트릭 이름 직접 입력
