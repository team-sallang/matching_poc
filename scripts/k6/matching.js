import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// 환경변수 설정 (기본값)
const BASE_URL = __ENV.BASE_URL || 'http://host.docker.internal:8080';
const VU = parseInt(__ENV.VU || '1000');
const DURATION = __ENV.DURATION || '5m';
const RAMP_UP = __ENV.RAMP_UP || '30s';
const RAMP_DOWN = __ENV.RAMP_DOWN || '30s'; // VU 감소 전 큐 비우기 시간
const POLLING_INTERVAL = parseFloat(__ENV.POLLING_INTERVAL || '0.1');
const TIMEOUT = parseInt(__ENV.TIMEOUT || '30');

// 커스텀 메트릭
const matchSuccessRate = new Rate('match_success_rate');
const matchTimeoutRate = new Rate('match_timeout_rate');
const matchLatency = new Trend('match_latency');

// 테스트 사용자 데이터 로드
let testUsers = [];
const TEST_USERS_FILE = '/scripts/test-users.json';

// test-users.json 파일에서 사용자 데이터 로드 (필수)
try {
    const fileContent = open(TEST_USERS_FILE);
    if (!fileContent) {
        throw new Error(`Test users file not found: ${TEST_USERS_FILE}`);
    }
    testUsers = JSON.parse(fileContent);
    if (!Array.isArray(testUsers) || testUsers.length === 0) {
        throw new Error(`Invalid test users file: ${TEST_USERS_FILE} (empty or invalid format)`);
    }
    console.log(`Loaded ${testUsers.length} test users from file`);
} catch (e) {
    console.error(`Failed to load test users file: ${e.message}`);
    console.error(`Please run "make generate-users" to create the test-users.json file first.`);
    throw e;
}

export function setup() {
    return { users: testUsers };
}

export const options = {
    stages: [
        { duration: RAMP_UP, target: VU },
        { duration: DURATION, target: VU },
        { duration: RAMP_DOWN, target: 0 }, // VU 감소 전 큐 비우기 시간 확보
    ],
    thresholds: {
        'http_req_duration': ['p(95)<500'],
        'http_req_failed': ['rate<0.01'],
        'match_success_rate': ['rate>0.8'],
    },
};

export default function (data) {
    const users = data.users;
    if (!users || users.length === 0) {
        console.error('No test users available');
        return;
    }
    
    // VU ID 기반으로 고유한 사용자 선택
    const userIndex = ((__VU - 1) + __ITER) % users.length;
    const user = users[userIndex];
    const userId = user.userId;
    const gender = user.gender;
    
    const joinStartTime = Date.now();
    let matched = false;
    let matchedWith = null;
    
    // 1. Join Queue
    const joinResponse = http.post(
        `${BASE_URL}/queue/join`,
        JSON.stringify({ userId: userId, gender: gender }),
        {
            headers: { 'Content-Type': 'application/json' },
            timeout: `${TIMEOUT}s`,
        }
    );
    
    // Join 실패 처리
    if (joinResponse.status === 0) {
        const matchLatencyMs = Date.now() - joinStartTime;
        matchLatency.add(matchLatencyMs);
        matchTimeoutRate.add(1);
        matchSuccessRate.add(0);
        return;
    }
    
    // 409 ALREADY_IN_QUEUE는 정상 (이전 반복에서 큐에 남아있을 수 있음)
    // 200이 아니고 409도 아니면 실패
    if (joinResponse.status !== 200 && joinResponse.status !== 409) {
        const matchLatencyMs = Date.now() - joinStartTime;
        matchLatency.add(matchLatencyMs);
        matchTimeoutRate.add(1);
        matchSuccessRate.add(0);
        return;
    }
    
    // Join 성공 체크 (메트릭용)
    check(joinResponse, {
        'join status is 200 or 409': (r) => r.status === 200 || r.status === 409,
    });
    
    // 2. Status Polling (MATCHED 감지)
    const maxPollingTime = TIMEOUT * 1000;
    const pollingDeadline = Date.now() + maxPollingTime;
    
    while (Date.now() < pollingDeadline && !matched) {
        const statusResponse = http.get(
            `${BASE_URL}/queue/status/${userId}`,
            {
                headers: { 'Content-Type': 'application/json' },
                timeout: `${TIMEOUT}s`,
            }
        );
        
        // Status 체크 (메트릭용)
        check(statusResponse, {
            'status check is 200': (r) => r.status === 200,
        });
        
        // 연결 오류 시 재시도
        if (statusResponse.status === 0) {
            sleep(POLLING_INTERVAL);
            continue;
        }
        
        // 200 응답인 경우에만 처리
        if (statusResponse.status === 200 && statusResponse.body) {
            try {
                const statusBody = JSON.parse(statusResponse.body);
                if (statusBody.status === 'MATCHED') {
                    matched = true;
                    matchedWith = statusBody.matchedWith;
                    break;
                }
            } catch (e) {
                // JSON 파싱 실패 시 계속 진행
                sleep(POLLING_INTERVAL);
                continue;
            }
        }
        
        sleep(POLLING_INTERVAL);
    }
    
    const matchLatencyMs = Date.now() - joinStartTime;
    matchLatency.add(matchLatencyMs);
    
    if (matched) {
        matchSuccessRate.add(1);
        
        // 3. ACK
        const ackResponse = http.post(
            `${BASE_URL}/queue/ack`,
            JSON.stringify({ userId: userId }),
            {
                headers: { 'Content-Type': 'application/json' },
                timeout: `${TIMEOUT}s`,
            }
        );
        
        check(ackResponse, {
            'ack status is 200': (r) => r.status === 200,
        });
        
        console.log(`User ${userId} matched with ${matchedWith} in ${matchLatencyMs}ms`);
    } else {
        matchTimeoutRate.add(1);
        matchSuccessRate.add(0);
        console.error(`User ${userId} timeout after ${matchLatencyMs}ms`);
        
        // 타임아웃 시 상태 확인 및 정리
        try {
            const statusCheckResponse = http.get(
                `${BASE_URL}/queue/status/${userId}`,
                {
                    headers: { 'Content-Type': 'application/json' },
                    timeout: '5s',
                }
            );
            
            if (statusCheckResponse.status === 200 && statusCheckResponse.body) {
                try {
                    const statusBody = JSON.parse(statusCheckResponse.body);
                    const currentStatus = statusBody.status;
                    
                    if (currentStatus === 'WAITING') {
                        // WAITING 상태면 Leave Queue로 정리
                        const leaveResponse = http.post(
                            `${BASE_URL}/queue/leave`,
                            JSON.stringify({ userId: userId }),
                            {
                                headers: { 'Content-Type': 'application/json' },
                                timeout: '5s',
                            }
                        );
                        
                        if (leaveResponse.status === 200) {
                            console.log(`User ${userId} left queue after timeout (was WAITING)`);
                        }
                    } else if (currentStatus === 'MATCHED') {
                        // MATCHED 상태면 ACK 호출 (매칭은 됐지만 polling 타임아웃)
                        const ackResponse = http.post(
                            `${BASE_URL}/queue/ack`,
                            JSON.stringify({ userId: userId }),
                            {
                                headers: { 'Content-Type': 'application/json' },
                                timeout: '5s',
                            }
                        );
                        
                        if (ackResponse.status === 200) {
                            console.log(`User ${userId} acknowledged match after timeout (was MATCHED)`);
                            // 매칭 성공으로 기록
                            matchSuccessRate.add(1);
                            matchTimeoutRate.add(-1); // 타임아웃 카운트 취소
                        }
                    }
                    // IDLE 상태면 이미 정리됨, 아무것도 안 함
                } catch (e) {
                    // JSON 파싱 실패 시 무시
                }
            }
        } catch (e) {
            // 상태 확인 실패해도 다음 iteration에서 처리 가능
            // 무시하고 계속 진행
        }
    }
    
    sleep(1);
}
