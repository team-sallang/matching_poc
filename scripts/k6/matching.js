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
// 현실적인 사용자 행동 시뮬레이션을 위한 대기 시간
const MATCH_SUCCESS_WAIT_MIN = parseInt(__ENV.MATCH_SUCCESS_WAIT_MIN || '10'); // 매칭 성공 후 최소 대기 시간 (10초)
const MATCH_SUCCESS_WAIT_MAX = parseInt(__ENV.MATCH_SUCCESS_WAIT_MAX || '60'); // 매칭 성공 후 최대 대기 시간 (초, 1분)
const TIMEOUT_RETRY_WAIT_MIN = parseInt(__ENV.TIMEOUT_RETRY_WAIT_MIN || '5'); // 타임아웃 후 재시도 전 최소 대기 시간 (초)
const TIMEOUT_RETRY_WAIT_MAX = parseInt(__ENV.TIMEOUT_RETRY_WAIT_MAX || '10'); // 타임아웃 후 재시도 전 최대 대기 시간 (초)

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

// Duration 파싱 헬퍼 함수 (예: "30s" -> 30, "5m" -> 300)
function parseDuration(durationStr) {
    const match = durationStr.match(/^(\d+)([smh])$/);
    if (!match) return 30; // 기본값 30초
    
    const value = parseInt(match[1]);
    const unit = match[2];
    
    switch (unit) {
        case 's': return value;
        case 'm': return value * 60;
        case 'h': return value * 3600;
        default: return value;
    }
}

export function setup() {
    const rampUpMs = parseDuration(RAMP_UP) * 1000;
    const durationMs = parseDuration(DURATION) * 1000;
    const rampDownMs = parseDuration(RAMP_DOWN) * 1000;
    // RAMP_DOWN 시작 시점: RAMP_UP + DURATION
    // 테스트 종료 시점: RAMP_UP + DURATION + RAMP_DOWN
    // RAMP_DOWN 단계에서는 정리만 수행하므로, RAMP_DOWN 시작 시점에서 정리 모드 진입
    return { 
        users: testUsers,
        testStartTime: Date.now(),
        rampDownStartTime: Date.now() + rampUpMs + durationMs,
        testEndTime: Date.now() + rampUpMs + durationMs + rampDownMs
    };
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
        // match_success_rate: 매칭 성공률 (MATCHED 상태가 된 비율)
        // 타임아웃은 정상 처리로 간주되므로, 실제 매칭 성공률만 측정
        // 목표: 80% 이상 (타임아웃 포함 시 실제 성공률은 더 높을 수 있음)
        'match_success_rate': ['rate>0.8'],
    },
};

// 사용자 상태 확인 헬퍼 함수
function checkUserStatus(userId, timeout = '5s') {
    try {
        const statusResponse = http.get(
            `${BASE_URL}/queue/status/${userId}`,
            {
                headers: { 'Content-Type': 'application/json' },
                timeout: timeout,
            }
        );
        
        if (statusResponse.status === 200 && statusResponse.body) {
            try {
                const statusBody = JSON.parse(statusResponse.body);
                return statusBody.status;
            } catch (e) {
                return null;
            }
        }
    } catch (e) {
        // 상태 확인 실패
    }
    return null;
}

// 사용자 정리 헬퍼 함수
function cleanupUser(userId, timeout = '0.5s') {
    // RAMP_DOWN 단계에서는 타임아웃을 매우 짧게 설정하여 서버 부하 방지
    // status 확인 없이 바로 leave/ack 시도 (서버가 에러 처리)
    // 여러 VU가 같은 사용자를 cleanup할 수 있으므로, 에러는 무시
    // 먼저 leave 시도 (WAITING 상태일 가능성이 높음)
    try {
        http.post(
            `${BASE_URL}/queue/leave`,
            JSON.stringify({ userId: userId }),
            {
                headers: { 'Content-Type': 'application/json' },
                timeout: timeout,
            }
        );
    } catch (e) {
        // leave 실패 시 ack 시도 (MATCHED 상태일 수 있음)
        try {
            http.post(
                `${BASE_URL}/queue/ack`,
                JSON.stringify({ userId: userId }),
                {
                    headers: { 'Content-Type': 'application/json' },
                    timeout: timeout,
                }
            );
        } catch (e2) {
            // 둘 다 실패해도 무시 (이미 cleanup되었거나 서버 부하)
        }
    }
}

export default function (data) {
    const users = data.users;
    if (!users || users.length === 0) {
        console.error('No test users available');
        return;
    }
    
    let iterationCounter = 0;
    let currentUserId = null;
    
    try {
        while (true) {
            // 1. 사용자 선택 (수동 카운터 사용)
            const userIndex = ((__VU - 1) + iterationCounter) % users.length;
            const user = users[userIndex];
            currentUserId = user.userId;
            const gender = user.gender;
            
            // 2. RAMP_DOWN 체크
            // RAMP_DOWN 단계에서는 새로운 매칭 시도를 중단하고 즉시 종료
            // cleanup은 finally 블록에서 수행 (서버 부하 방지)
            if (Date.now() >= data.rampDownStartTime) {
                break;
            }
            
            const joinStartTime = Date.now();
            let matched = false;
            let matchedWith = null;
            
            // 4. Join Queue
            const joinResponse = http.post(
                `${BASE_URL}/queue/join`,
                JSON.stringify({ userId: currentUserId, gender: gender }),
                {
                    headers: { 'Content-Type': 'application/json' },
                    timeout: `${TIMEOUT}s`,
                }
            );
            
            // Join 실패 처리
            if (joinResponse.status === 0) {
                // matchLatency 기록 제거 (Join 실패는 매칭 시도 자체가 실패)
                matchTimeoutRate.add(1);
                matchSuccessRate.add(0);
                sleep(1);
                iterationCounter++;
                continue;
            }
            
            // 409 ALREADY_IN_QUEUE는 정상 (이전 반복에서 큐에 남아있을 수 있음)
            // 200이 아니고 409도 아니면 실패
            if (joinResponse.status !== 200 && joinResponse.status !== 409) {
                // matchLatency 기록 제거 (Join 실패는 매칭 시도 자체가 실패)
                matchTimeoutRate.add(1);
                matchSuccessRate.add(0);
                sleep(1);
                iterationCounter++;
                continue;
            }
            
            // Join 성공 체크 (메트릭용)
            check(joinResponse, {
                'join status is 200 or 409': (r) => r.status === 200 || r.status === 409,
            });
            
            // 5. Status Polling (MATCHED 감지)
            const maxPollingTime = TIMEOUT * 1000;
            const pollingDeadline = Date.now() + maxPollingTime;
            
            while (Date.now() < pollingDeadline && !matched) {
                const statusResponse = http.get(
                    `${BASE_URL}/queue/status/${currentUserId}`,
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
                            // 매칭 성공 시점에 즉시 match_latency 기록
                            const matchLatencyMs = Date.now() - joinStartTime;
                            if (matchLatencyMs >= 0) {  // 음수 방지
                                matchLatency.add(matchLatencyMs);
                            }
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
            
            // 매칭 성공 시점에 이미 기록했으므로 여기서는 제거
            // 타임아웃인 경우는 아래 else 블록에서 처리
            
            // 6. 성공/타임아웃 처리
            if (matched) {
                matchSuccessRate.add(1);
                
                // ACK 호출
                const ackResponse = http.post(
                    `${BASE_URL}/queue/ack`,
                    JSON.stringify({ userId: currentUserId }),
                    {
                        headers: { 'Content-Type': 'application/json' },
                        timeout: `${TIMEOUT}s`,
                    }
                );
                
                check(ackResponse, {
                    'ack status is 200': (r) => r.status === 200,
                });
                
                // 현실적인 사용자 행동: 매칭 성공 후 대화/게임 시간 시뮬레이션 (30초~5분 랜덤)
                const successWaitTime = Math.random() * (MATCH_SUCCESS_WAIT_MAX - MATCH_SUCCESS_WAIT_MIN) + MATCH_SUCCESS_WAIT_MIN;
                sleep(successWaitTime);
            } else {
                // 타임아웃 시 상태 확인 먼저 수행 (메트릭 기록 전)
                let actualStatus = checkUserStatus(currentUserId);
                
                // 상태 확인 결과에 따라 올바른 메트릭 기록
                if (actualStatus === 'MATCHED') {
                    // 실제로는 매칭 성공 (polling 타임아웃이었지만 매칭은 완료됨)
                    matchSuccessRate.add(1);
                    matchTimeoutRate.add(0);
                    
                    // 타임아웃 후 확인한 경우는 실제 매칭 시간을 알 수 없으므로
                    // match_latency 기록하지 않음 (이미 정상 매칭 시점에 기록됨)
                    
                    // ACK 호출
                    const ackResponse = http.post(
                        `${BASE_URL}/queue/ack`,
                        JSON.stringify({ userId: currentUserId }),
                        {
                            headers: { 'Content-Type': 'application/json' },
                            timeout: '5s',
                        }
                    );
                    
                    if (ackResponse.status === 200) {
                        console.log(`User ${currentUserId} acknowledged match after timeout (was MATCHED)`);
                    }
                    
                    // 현실적인 사용자 행동: 매칭 성공 후 대화/게임 시간 시뮬레이션 (30초~5분 랜덤)
                    const successWaitTime = Math.random() * (MATCH_SUCCESS_WAIT_MAX - MATCH_SUCCESS_WAIT_MIN) + MATCH_SUCCESS_WAIT_MIN;
                    sleep(successWaitTime);
                } else {
                    // 실제 타임아웃 (WAITING, IDLE, 또는 상태 확인 실패)
                    // 타임아웃도 정상 처리로 간주 (시스템이 정상 동작했고, 단지 매칭 상대가 없었을 뿐)
                    matchTimeoutRate.add(1);
                    matchSuccessRate.add(0);
                    
                    // 현실적인 사용자 행동: 타임아웃 시 큐에서 떠남
                    if (actualStatus === 'WAITING') {
                        try {
                            http.post(
                                `${BASE_URL}/queue/leave`,
                                JSON.stringify({ userId: currentUserId }),
                                {
                                    headers: { 'Content-Type': 'application/json' },
                                    timeout: '5s',
                                }
                            );
                        } catch (e) {
                            // Leave 실패해도 무시하고 계속 진행
                        }
                    }
                    
                    // 현실적인 사용자 행동: 타임아웃 후 잠시 대기 후 재시도 (5~10초 랜덤)
                    const retryWaitTime = Math.random() * (TIMEOUT_RETRY_WAIT_MAX - TIMEOUT_RETRY_WAIT_MIN) + TIMEOUT_RETRY_WAIT_MIN;
                    sleep(retryWaitTime);
                }
            }
            
            iterationCounter++;
        }
    } finally {
        // 최종 정리 (RAMP_DOWN 단계에서 서버 부하 방지를 위해 짧은 타임아웃 사용)
        if (currentUserId) {
            cleanupUser(currentUserId, '2s');
        }
    }
}
