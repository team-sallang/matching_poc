## 1. 개요 (Background & Objective)

현재의 단순 매칭 로직을 폐기하고, **Azar(하이퍼커넥트)의 데이터 중심 매칭 아키텍처**를 경량화하여 적용한다.
기존의 단순 조회 방식을 버리고 **'즉시 매칭(Intercept)'**과 **'스케줄링 루프(Loop)'**가 결합된 하이브리드 방식을 도입한다.
특히 **PostgreSQL의 동시성 제어 기능**과 **GIN Index**를 활용하여 Redis 없이도 고성능과 데이터 무결성을 보장하는 시스템을 구축한다.

### 핵심 목표 (Key Goals)

1. **Latency**: 대기자가 있을 경우 0.1초 내 즉시 매칭 (Controller Intercept).
2. **Concurrency**: `SELECT ...  FOR UPDATE SKIP LOCKED`를 적용하여 중복 매칭(Double Booking) 원천 차단.
3. **Optimization**: PostgreSQL **GIN Index**를 사용하여 배열(Array) 기반 취미 매칭 속도 극대화
4. **UX**: Polling을 제거하고 **Supabase Realtime**을 통해 매칭 성사 즉시 시그널 전송.

---

## 2. 데이터베이스 스키마 재설계 (Schema Redesign)

기존 ERD를 기반으로 매칭 속도를 위한 **역정규화(Denormalization)** 및 **인덱싱**을 수행한다

### 2.1 테이블 정의

- [ ] **`hobbies` (Master)**: A~F 카테고리 체계를 갖춘 정적 데이터.
- [ ] **`users` (Modified)**
  - `total_score` (INT, Default 0): 활동 점수.
  - `tier` (ENUM): 거름/시들/새싹/꽃잎/열매.
  - `hobby_ids_cache` (INT[]): 매칭 조인 비용 절감을 위한 취미 ID 배열 캐시.
- [ ] **`match_queue` (New Engine)**
  - `queue_id` (PK), `user_id` (FK)
  - `hobby_ids` (INT[]): **GIN Index 적용 필수.**
  - `status` (ENUM: 'waiting', 'matched')
  - `location`, `birth_year`, `gender`, `tier` (역정규화 컬럼)
  - `created_at` (Timestamp): 단계별 완화 로직의 기준 시간.
- [ ] **`rooms` (Signaling)**
  - `room_id` (UUID), `user1_id`, `user2_id`.
  - _Supabase Realtime이 이 테이블의 INSERT 이벤트를 구독함._

### 2.2 인덱스 적용 (Performance)

PostgreSQL의 배열 교집합 연산 속도를 위해 반드시 적용해야 함 .

```sql
CREATE INDEX idx_mq_hobbies ON match_queue USING GIN (hobby_ids);
CREATE INDEX idx_mq_status_time ON match_queue (status, created_at);
```

---

## 3. 매칭 엔진 로직 (Matching Engine Implementation)

### 3.1 동시성 제어 전략 (Concurrency Strategy)

모든 매칭 쿼리(Controller/Scheduler 공통)는 반드시 비관적 락(Pessimistic Lock)과 Skip Locked를 사용해야 함.
Query Rule:

```sql
SELECT * FROM match_queue ...  FOR UPDATE SKIP LOCKED
```

### 3.2 로직 A: 즉시 매칭 (Synchronous Intercept) - Controller

API 요청(POST /match) 시점에 대기열 진입 전 탐색을 먼저 수행한다.

1. 유저 상태 확인: users. tier 확인.
2. 후보 탐색 (Query):
   - _일반 유저_: status='waiting' AND tier > -21 AND hobby_ids && : myHobbies (1단계 조건) 만족하는 유저 1명 조회
   - _거름 유저_: status='waiting' AND tier <= -21인 유저(동료 거름) 1명 조회
3. Transaction 처리:
   - 성공 시: rooms 테이블 INSERT → 상대방 status='matched' 업데이트 → room_id 반환
   - 실패 시: 내 정보를 match_queue에 INSERT → waiting 반환 → Supabase Realtime 대기 모드 전환

### 3.3 로직 B: 스케줄링 매칭 (Async Loop) - Scheduler

- 1초 주기로 실행되며, 대기 시간(Current - CreatedAt)에 따라 조건을 완화한다.

| 단계 | 대기 시간 | 완화 조건 (AND 연산)                    | 거름 회원 매칭 여부 |
| ---- | --------- | --------------------------------------- | ------------------- |
| 1    | 0~5초     | 취미 교집합 + 지역 + 나이(±5) + 이성    | 불가                |
| 2    | 5~10초    | [취미 무관] + 지역 + 나이 + 이성        | 불가                |
| 3    | 10~20초   | [취미 무관] + [지역 무관] + 나이 + 이성 | 불가                |
| 4    | 20~30초   | [취미/지역 무관] + [나이 무관] + 이성   | 불가                |
| 5    | 30초~     | [모두 무관] + 동성 허용                 | 허용 (구제)         |

---

## 4. 점수 및 등급 시스템 (Scoring System)

매칭 로직과 분리하여 비동기 이벤트로 점수를 계산한다.

### 4.1 등급 산정 (Tier)

- 거름 (-21↓): 일반 매칭 격리 (Phase 5에서만 해제)
- 시들 (-20 ~ -11) / 새싹 (-10 ~ 10) / 꽃잎 (11 ~ 19) / 열매 (20↑)

### 4.2 점수 업데이트 트리거

- [ ] **가산점:** 출석(+1), 대화 5분↑(+2), 하트 받음(+1), 신규 유저 케어(+3), 결제(+1~+20)
- [ ] **감점:** 신고(-3), 제재(-5 ~ -20), 탈주(나-2/상대-1)

---

## 5. 구현 체크리스트 (Implementation Checklist)

**Backend (Spring Boot)**

- [ ] match_queue 엔티티 및 Repository 생성 (Native Query 필수)
- [ ] MatchService.findMatch() 구현: FOR UPDATE SKIP LOCKED 적용 확인
- [ ] MatchScheduler 구현: 1초 주기, Phase 1~5 로직 적용
- [ ] 점수 산정 로직(ScoreService) 구현

**Frontend & Infra**

- [ ] Supabase Realtime 활성화: rooms 테이블의 INSERT 이벤트 구독 설정
- [ ] API 연동:
  - POST /api/v1/match: 매칭 요청
  - socket.on('INSERT', 'rooms'): 매칭 성사 시 화상방 이동

**Verification (검증)**

- [ ] 동시성 테스트: 유저 A, B가 동시에 유저 C와 매칭 시도 시, 반드시 한 명만 성공하고 한 명은 대기해야 함
- [ ] 인덱스 성능: 대기열 1만 명 기준, 1단계 매칭 쿼리가 0.01초 내에 수행되는지 EXPLAIN ANALYZE로 확인

---

## 6. 참고 사항 (Reference)

- 취미 데이터는 소스의 A~F 카테고리 구조를 DB 시딩(Seeding) 할 것
- 거름 회원(-21점 이하)은 일반 회원과 즉시 매칭되지 않도록 WHERE 절에서 엄격히 분리할 것
