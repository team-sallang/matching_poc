#!/usr/bin/env node

/**
 * 테스트 사용자 파일 생성 스크립트
 * k6가 파일 쓰기를 지원하지 않으므로 별도 스크립트로 생성
 */

const fs = require('fs');
const path = require('path');

const TEST_USERS_FILE = path.join(__dirname, 'test-users.json');
const USER_COUNT = parseInt(process.env.VU || '1000');

function generateUUID() {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
        const r = Math.random() * 16 | 0;
        const v = c === 'x' ? r : (r & 0x3 | 0x8);
        return v.toString(16);
    });
}

function generateUsers(count) {
    const users = [];
    for (let i = 0; i < count; i++) {
        users.push({
            userId: generateUUID(),
            gender: i % 2 === 0 ? 'male' : 'female'
        });
    }
    return users;
}

// 메인 로직
function main() {
    // 파일이 이미 있으면 건너뛰기 (옵션)
    if (process.argv.includes('--force') || !fs.existsSync(TEST_USERS_FILE)) {
        console.log(`Generating ${USER_COUNT} test users...`);
        const users = generateUsers(USER_COUNT);
        
        fs.writeFileSync(TEST_USERS_FILE, JSON.stringify(users, null, 2), 'utf8');
        console.log(`✓ Created ${users.length} test users at ${TEST_USERS_FILE}`);
        console.log(`  - Male users: ${users.filter(u => u.gender === 'male').length}`);
        console.log(`  - Female users: ${users.filter(u => u.gender === 'female').length}`);
    } else {
        console.log(`Test users file already exists at ${TEST_USERS_FILE}`);
        console.log(`  Use --force to regenerate`);
    }
}

main();

