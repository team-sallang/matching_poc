import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// 환경변수 설정 (기본값)
// VU: 동시 사용자 수 (기본: 1000)
// DURATION: 테스트 지속 시간 (기본: 5m)
// RAMP_UP: ramp-up 시간 (기본: 30s)
// BASE_URL: Spring Boot 앱 주소 (기본: http://localhost:8080)
// POLLING_INTERVAL: Status polling 간격 (기본: 0.1초)
// TIMEOUT: 타임아웃 (기본: 30초)

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const VU = parseInt(__ENV.VU || '1000');
const DURATION = __ENV.DURATION || '5m';
const RAMP_UP = __ENV.RAMP_UP || '30s';
const POLLING_INTERVAL = parseFloat(__ENV.POLLING_INTERVAL || '0.1');
const TIMEOUT = parseInt(__ENV.TIMEOUT || '30');

// 커스텀 메트릭
const matchSuccessRate = new Rate('match_success_rate');
const matchTimeoutRate = new Rate('match_timeout_rate');
const matchLatency = new Trend('match_latency');

// 테스트 사용자 데이터 로드
let testUsers = [];

function loadOrCreateTestUsers() {
    const fs = require('k6/experimental/fs');
    const path = '/scripts/test-users.json';
    
    try {
        // 파일이 있으면 로드
        const fileContent = fs.readFileSync(path);
        if (fileContent) {
            testUsers = JSON.parse(fileContent);
            console.log(`Loaded ${testUsers.length} test users from file`);
            return;
        }
    } catch (e) {
        // 파일이 없으면 생성
        console.log('Test users file not found, creating new users...');
    }
    
    // 1000명의 사용자 생성 (성별 50:50)
    testUsers = [];
    for (let i = 0; i < 1000; i++) {
        testUsers.push({
            userId: generateUUID(),
            gender: i % 2 === 0 ? 'male' : 'female'
        });
    }
    
    // 파일로 저장
    try {
        fs.writeFileSync(path, JSON.stringify(testUsers, null, 2));
        console.log(`Created and saved ${testUsers.length} test users to file`);
    } catch (e) {
        console.log('Warning: Could not save test users to file:', e);
    }
}

function generateUUID() {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
        const r = Math.random() * 16 | 0;
        const v = c === 'x' ? r : (r & 0x3 | 0x8);
        return v.toString(16);
    });
}

export function setup() {
    loadOrCreateTestUsers();
    return { users: testUsers };
}

export const options = {
    stages: [
        { duration: RAMP_UP, target: VU }, // ramp-up
        { duration: DURATION, target: VU }, // 테스트 지속
    ],
    thresholds: {
        'http_req_duration': ['p(95)<500'], // 95% 요청이 500ms 이하
        'http_req_failed': ['rate<0.01'], // 에러율 1% 미만
        'match_success_rate': ['rate>0.8'], // 매칭 성공률 80% 이상
    },
};

export default function (data) {
    const users = data.users;
    if (!users || users.length === 0) {
        console.error('No test users available');
        return;
    }
    
    // 랜덤 사용자 선택
    const user = users[Math.floor(Math.random() * users.length)];
    const userId = user.userId;
    const gender = user.gender;
    
    // 1. Join Queue
    const joinStartTime = Date.now();
    const joinResponse = http.post(
        `${BASE_URL}/queue/join`,
        JSON.stringify({
            userId: userId,
            gender: gender
        }),
        {
            headers: { 'Content-Type': 'application/json' },
            timeout: `${TIMEOUT}s`,
        }
    );
    
    const joinSuccess = check(joinResponse, {
        'join status is 200': (r) => r.status === 200,
        'join response has status': (r) => {
            const body = JSON.parse(r.body);
            return body.status === 'WAITING';
        },
    });
    
    if (!joinSuccess) {
        console.error(`Join failed for user ${userId}: ${joinResponse.status} - ${joinResponse.body}`);
        return;
    }
    
    // 2. Status Polling (MATCHED 감지)
    let matched = false;
    let matchedWith = null;
    const maxPollingTime = TIMEOUT * 1000; // milliseconds
    const pollingDeadline = Date.now() + maxPollingTime;
    
    while (Date.now() < pollingDeadline && !matched) {
        const statusResponse = http.get(
            `${BASE_URL}/queue/status/${userId}`,
            {
                headers: { 'Content-Type': 'application/json' },
                timeout: `${TIMEOUT}s`,
            }
        );
        
        const statusCheck = check(statusResponse, {
            'status check is 200': (r) => r.status === 200,
        });
        
        if (statusCheck) {
            const statusBody = JSON.parse(statusResponse.body);
            if (statusBody.status === 'MATCHED') {
                matched = true;
                matchedWith = statusBody.matchedWith;
                break;
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
            JSON.stringify({
                userId: userId
            }),
            {
                headers: { 'Content-Type': 'application/json' },
                timeout: `${TIMEOUT}s`,
            }
        );
        
        check(ackResponse, {
            'ack status is 200': (r) => r.status === 200,
            'ack response has status IDLE': (r) => {
                const body = JSON.parse(r.body);
                return body.status === 'IDLE';
            },
        });
        
        console.log(`User ${userId} matched with ${matchedWith} in ${matchLatencyMs}ms`);
    } else {
        matchTimeoutRate.add(1);
        matchSuccessRate.add(0);
        console.error(`User ${userId} timeout after ${matchLatencyMs}ms`);
    }
    
    sleep(1); // 다음 VU 실행 전 대기
}

export function teardown(data) {
    console.log('Test completed');
}

