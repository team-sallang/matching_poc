# ERD (Entity-Relationship Diagram) - v3

## 테이블 정의

### 1. `users`

사용자 기본 정보.

| 컬럼명           | 타입            | 제약조건                    | 설명                                  |
|---------------|---------------|-------------------------|-------------------------------------|
| `id`          | `UUID`        | `PK`                    | 사용자 고유 ID                           |
| `nickname`    | `VARCHAR(50)` | `UNIQUE`, `NOT NULL`    | 닉네임                                 |
| `gender`      | `ENUM`        | `NOT NULL`              | 성별 (`MALE`, `FEMALE`)               |
| `birth_date`  | `DATE`        | `NOT NULL`              | 출생년도                                |
| `region`      | `ENUM`        | `NOT NULL`              | 지역                                  |
| `total_score` | `INT`         | `NOT NULL`, `Default 0` | 활동 점수                               |
| `tier`        | `VARCHAR(20)` | `NOT NULL`              | 사용자 등급 (TIER_FRUIT, TIER_LEAF, ...) |
| `created_at`  | `TIMESTAMP`   | `NOT NULL`              | 생성 일시                               |
| `updated_at`  | `TIMESTAMP`   | `NOT NULL`              | 수정 일시                               |
| `deleted_at`  | `TIMESTAMP`   | `NULL`                  | 삭제 일시                               |

### 1-2. `regions`

    SEOUL("서울"),      // 서울특별시
    GYEONGGI("경기"),   // 경기도
    INCHEON("인천"),    // 인천광역시
    GANGWON("강원"),    // 강원특별자치도
    CHUNGNAM("충남"),   // 충청남도
    DAEJEON("대전"),    // 대전광역시
    CHUNGBUK("충북"),   // 충청북도
    SEJONG("세종"),     // 세종특별자치시
    BUSAN("부산"),      // 부산광역시
    ULSAN("울산"),      // 울산광역시
    DAEGU("대구"),      // 대구광역시
    GYEONGBUK("경북"),  // 경상북도
    GYEONGNAM("경남"),  // 경상남도
    JEONNAM("전남"),    // 전라남도
    GWANGJU("광주"),    // 광주광역시
    JEONBUK("전북"),    // 전북특별자치도
    JEJU("제주");       // 제주특별자치도

### 1-3. `tiers`

    FERTILIZER("거름"), // -21점 이하 거름회원
    WILTING("시들"),    // -20 ~ -11점 시들회원
    SPROUT("새싹"),     // -10 ~ +10점 새싹회원
    PETAL("꽃잎"),      // 11 ~ 20점 꽃잎회원
    FRUIT("열매");      // 21점 이상 열매회원

---

### 2. `hobbies`

세부 취미 항목 테이블.

| 컬럼명           | 타입            | 제약조건                   | 설명                  |
|---------------|---------------|------------------------|---------------------|
| `id`          | `INT`         | `PK`, `Auto-increment` | 취미 고유 ID            |
| `category`    | `ENUM`        | `NOT NULL`             | 상위 카테고리 코드          |
| `subcategory` | `ENUM`        | `NULL`                 | 하위 카테고리 코드          |
| `name`        | `VARCHAR(50)` | `NOT NULL`             | 취미 이름 (e.g., "하이킹") |
| `created_at`  | `TIMESTAMP`   | `NOT NULL`             | 생성 일시               |
| `updated_at`  | `TIMESTAMP`   | `NOT NULL`             | 수정 일시               |
| `deleted_at`  | `TIMESTAMP`   | `NULL`                 | 삭제 일시               |

---

### 4. `user_hobbies`

사용자와 취미의 N:M 관계를 정의하는 매핑(조인) 테이블. (Surrogate Key 적용)

| 컬럼명          | 타입          | 제약조건                   | 설명                      |
|--------------|-------------|------------------------|-------------------------|
| `user_hobby_id` | `BIGINT`    | `PK`, `Auto-increment` | **(대리 키)** 매핑 테이블 고유 ID |
| `user_id`    | `UUID`      | `FK (users.id)`        | 사용자 ID                  |
| `hobby_id`   | `INT`       | `FK (hobbies.id)`      | 취미 ID                   |
| `created_at` | `TIMESTAMP` | `NOT NULL`             | 생성 일시                   |
| `updated_at` | `TIMESTAMP` | `NOT NULL`             | 수정 일시                   |
| `deleted_at` | `TIMESTAMP` | `NULL`                 | 삭제 일시                   |

**Constraints & Indexes:**

- `UNIQUE` 제약조건: `(users.id, hobbies.id)`
    - 한 명의 유저가 동일한 취미를 여러 번 등록하는 것을 방지합니다.

---

### 5. `match_queue`

매칭 대기열 엔진 테이블. `refactoring.md`의 성능 목표를 위해 의도적으로 역정규화된 구조를 일부 유지합니다.

| 컬럼명          | 타입            | 제약조건                      | 설명                             |
|--------------|---------------|---------------------------|--------------------------------|
| `queue_id`   | `BIGINT`      | `PK`, `Auto-increment`    | 대기열 고유 ID                      |
| `user_id`    | `UUID`        | `FK (users.id)`, `UNIQUE` | 대기중인 사용자 ID                    |
| `status`     | `VARCHAR(20)` | `Default 'waiting'`       | 상태 (`waiting`, `matched`)      |
| `hobby_ids`  | `INT[]`       | `GIN Index`               | **(역정규화)** 매칭 시점의 사용자 취미 ID 배열 |
| `tier`       | `VARCHAR(20)` |                           | **(역정규화)** 매칭 시점의 사용자 등급       |
| `location`   | `VARCHAR(50)` |                           | **(역정규화)** 매칭 시점의 지역           |
| `birth_year` | `INT`         |                           | **(역정규화)** 매칭 시점의 출생년도         |
| `gender`     | `VARCHAR(10)` |                           | **(역정규화)** 매칭 시점의 성별           |
| `created_at` | `TIMESTAMP`   | `NOT NULL`                | 대기 시작 시간 (매칭 조건 완화 기준)         |

**Rationale for Denormalization:**

- `match_queue`에 `tier`, `location`, `hobby_ids` 등을 캐싱하는 이유는 매칭 조회 시 `users` 테이블과의 Join을 최소화하여 0.1초 내의 응답 속도 목표를 달성하기
  위함입니다.
- `hobby_ids`는 PostgreSQL의 `GIN` 인덱스와 배열 교차 연산(`&&`)을 사용한 고속 필터링에 필수적입니다.
- 데이터는 매칭 요청 시 `user_hobbies`와 `users` 테이블에서 가져와 채웁니다.

---

### 6. `rooms`

매칭이 성사된 사용자 방.

| 컬럼명          | 타입          | 제약조건            | 설명      |
|--------------|-------------|-----------------|---------|
| `room_id`    | `UUID`      | `PK`            | 방 고유 ID |
| `user1_id`   | `UUID`      | `FK (users.id)` | 참여자 1   |
| `user2_id`   | `UUID`      | `FK (users.id)` | 참여자 2   |
| `created_at` | `TIMESTAMP` | `NOT NULL`      | 방 생성 시간 |

---

## 관계 (Relationships)

- **`users` ↔ `hobbies`**: N:M
    - `user_hobbies` 조인 테이블을 통해 관계를 맺습니다.
- **`users` → `match_queue`**: 1:1
    - 매칭을 기다리는 사용자는 `match_queue`에 하나의 레코드를 가집니다.
- **`users` ↔ `rooms`**: 1:N
    - `rooms` 테이블의 `user1_id`, `user2_id`가 `users.id`를 참조합니다.
