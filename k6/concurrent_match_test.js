import http from 'k6/http';
import { SharedArray } from 'k6/data';
import { check, sleep } from 'k6';
import { Trend } from 'k6/metrics';

// 매칭 완료까지의 end-to-end 시간 측정 (ms)
const matchCompleteMs = new Trend('match_complete_ms');

// 요청 타임아웃 설정 (k6 기본 60s). 필요 시 HTTP_TIMEOUT 환경변수로 변경 가능.
const requestTimeout = __ENV.HTTP_TIMEOUT || '120s';
// 상태 조회 폴링에서 최대 기다리는 시간
const maxWaitMs = 40000;
// 상태 조회 폴링 간격
const intervalMs = 5000;

// 동일 조건 사용자: k6/users_rows.csv (헤더: id,nickname,...). 스크립트와 같은 디렉터리. 프로젝트 루트에서: k6 run k6/concurrent_match_test.js
const userIds = new SharedArray('userIds', function () {
  const raw = open('./users_rows.csv');
  const lines = raw.replace(/\r\n/g, '\n').replace(/\r/g, '\n').trim().split('\n');
  return lines
    .slice(1) // skip header
    .map((line) => line.split(',')[0].trim())
    .filter((id) => id.length > 0);
});

if (userIds.length === 0) {
  throw new Error('users_rows.csv가 비어 있거나 id 컬럼이 없습니다. seed 후 k6/users_rows.csv를 준비하세요.');
}

export const options = {
  scenarios: {
    concurrent_match: {
      executor: 'per-vu-iterations',
      vus: 1000,
      iterations: 1,
    },
  },
  thresholds: {
    // match_request(POST /match): 커넥션 풀 개선 후 p95 < 2s 목표
    'http_req_duration{name:match_request}': ['p(95)<2000', 'p(99)<5000'],
    // match_status(GET /match/status): 단순 SELECT, p95 < 1s 목표
    'http_req_duration{name:match_status}': ['p(95)<1000', 'p(99)<2000'],
    http_req_failed: ['rate<0.01'], // 실패율 < 1%
    match_complete_ms: ['p(95)<30000', 'p(99)<40000'], // 매칭 완료까지 95p < 30s, 99p < 40s
  },
};

export default function () {
  if (__VU > userIds.length) {
    throw new Error(`VU 수(${__VU})가 userIds 길이(${userIds.length})를 초과했습니다. 중복 없이 1회씩 실행하려면 users_rows.csv를 늘리거나 vus를 줄이세요.`);
  }
  const userId = userIds[__VU - 1];
  // 호스트 실행 기본: localhost. Docker 내 k6→호스트 앱: API_URL=http://host.docker.internal:8080/api/v1/match
  const url = __ENV.API_URL || 'http://localhost:8080/api/v1/match';
  const statusUrl = __ENV.STATUS_URL || url.replace(/\/match$/, '/match/status');

  // 매칭 시작 시간 기록
  const matchStartTime = Date.now();

  const payload = JSON.stringify({ user_id: userId });
  const params = {
    headers: { 'Content-Type': 'application/json' },
    timeout: requestTimeout,
    tags: { name: 'match_request' },
  };

  const response = http.post(url, payload, params);

  check(response, {
    'status is 200 or 202': (r) => r.status === 200 || r.status === 202,
  });

  if (response.status !== 200 && response.status !== 202) {
    console.error(`Failed for user ${userId}: ${response.status} - ${response.body}`);
    return;
  }

  // 즉시 매칭 완료 (200 응답)
  if (response.status === 200) {
    const matchCompleteTime = Date.now() - matchStartTime;
    matchCompleteMs.add(matchCompleteTime);
    check({ matchCompleteTime }, {
      'match completed': () => true,
    });
    return;
  }

  // 대기 후 매칭 완료 (202 응답)
  if (response.status === 202) {
    let waited = 0;
    while (waited < maxWaitMs) {
      const statusRes = http.get(`${statusUrl}?user_id=${userId}`, {
        tags: { name: 'match_status' },
        timeout: requestTimeout,
      });
      if (statusRes.status === 200) {
        const body = statusRes.json();
        if (body && body.status === 'MATCHED') {
          const matchCompleteTime = Date.now() - matchStartTime;
          matchCompleteMs.add(matchCompleteTime);
          check(body, {
            'match status is MATCHED': (b) => b.status === 'MATCHED',
            'match completed within timeout': () => true,
          });
          return;
        }
      } else if (statusRes.status === 404) {
        console.error(`Status not found for user ${userId}: ${statusRes.status} - ${statusRes.body}`);
        return;
      }
      sleep(intervalMs / 1000);
      waited += intervalMs;
    }
    console.error(`Timed out waiting for match status for user ${userId} after ${maxWaitMs}ms`);
  }
}
