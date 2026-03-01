# 매칭 서비스 구조 문서

> 최종 업데이트: 2026-03-02

---

## 1. 시스템 개요

소셜 매칭 POC. 두 사용자를 빠르게 연결하는 것을 목표로 한다.

| 관심사 | 전략 |
|--------|------|
| 초저지연 매칭 | 요청 시 대기열 인터셉트(즉시 매칭) 우선 시도 |
| 동시성 안전 | `FOR UPDATE SKIP LOCKED` + `updateStatusIf` 원자적 벌크 업데이트 |
| 조건 완화 | 대기 시간에 따라 Phase 1→5 단계적 조건 완화 |
| 모니터링 | Micrometer 커스텀 메트릭 + OpenTelemetry 분산 트레이싱 |

### 핵심 컴포넌트

| 컴포넌트 | 역할 |
|----------|------|
| `MatchController` | REST API 진입점 (POST/DELETE/GET) |
| `MatchService` | 매칭 비즈니스 로직 (인터셉트, 대기열, 확정, 취소) |
| `MatchScheduler` | 1초 주기 대기열 스캔 및 Phase 기반 매칭 실행 |
| `MatchQueueMatchFinder` | Phase 1~5 쿼리 인터페이스 (PostgreSQL / H2 구현 분리) |
| `MatchQueueRepository` | `FOR UPDATE SKIP LOCKED` 네이티브 쿼리, `updateStatusIf` |

---

## 2. 전체 아키텍처

```mermaid
flowchart TD
    C[Client] -->|POST /api/v1/match| MC[MatchController]
    MC --> MS[MatchService]

    MS -->|대기열에 파트너 존재?| IC{인터셉트 가능?}
    IC -->|YES - 즉시 매칭| CR[Room 생성\npartner.status = MATCHED]
    IC -->|NO - 대기열 등록| MQ[(match_queue\nstatus=WAITING)]

    CR -->|200 OK + room_id| C
    MQ -->|202 Accepted| C

    SCHED[MatchScheduler\n1초 fixedDelay] -->|findByStatus WAITING| MQ
    SCHED --> PHASE[Phase 1~5 결정\n대기 시간 기반]
    PHASE -->|FOR UPDATE SKIP LOCKED| MQ
    PHASE -->|파트너 발견| CM[MatchService.confirmMatch]
    CM -->|updateStatusIf WAITING→MATCHED| MQ
    CM -->|Room 생성| ROOMS[(rooms)]

    C2[Client] -->|GET /api/v1/match/status| MC
    MC --> GS[getMatchStatus]
    GS --> ROOMS
    GS --> MQ

    style CR fill:#d4edda,stroke:#28a745
    style MQ fill:#fff3cd,stroke:#ffc107
    style ROOMS fill:#d4edda,stroke:#28a745
    style SCHED fill:#cce5ff,stroke:#004085
```

---

## 3. 매칭 요청 흐름

```mermaid
sequenceDiagram
    participant C as Client
    participant MC as MatchController
    participant MS as MatchService
    participant DB as DB (match_queue / rooms)

    %% 즉시 매칭 경로
    C->>MC: POST /api/v1/match {userId}
    MC->>MS: requestMatch(request)
    MS->>DB: findByUserId(userId) — 중복 체크
    MS->>DB: findPhase1Match(...) — 인터셉트 파트너 탐색

    alt 파트너 존재 (즉시 매칭)
        MS->>DB: roomRepository.save(Room)
        MS->>DB: partner.status = MATCHED
        MS-->>MC: MatchResponse.matched(roomId)
        MC-->>C: 200 OK {status: "MATCHED", roomId}
    else 파트너 없음 (대기열 등록)
        MS->>DB: matchQueueRepository.save(MatchQueue)
        MS-->>MC: MatchResponse.waiting(...)
        MC-->>C: 202 Accepted {status: "WAITING"}
    end

    %% 상태 조회
    C->>MC: GET /api/v1/match/status?user_id={userId}
    MC->>MS: getMatchStatus(userId)
    MS->>DB: roomRepository.findFirstByUser1IdOrUser2Id
    alt Room 존재
        MS-->>C: 200 OK {status: "MATCHED", roomId}
    else
        MS->>DB: matchQueueRepository.findByUserId
        MS-->>C: 200 OK {status: "WAITING"}
    end

    %% 취소
    C->>MC: DELETE /api/v1/match {userId}
    MC->>MS: cancelMatch(request)
    MS->>DB: matchQueueRepository.delete(queue)
    MS-->>C: 204 No Content
```

---

## 4. 스케줄러 매칭 확정 흐름

```mermaid
sequenceDiagram
    participant SCH as MatchScheduler
    participant REPO as MatchQueueRepository
    participant MS as MatchService
    participant DB as DB (rooms)

    loop fixedDelay 1000ms (단일 스레드)
        SCH->>REPO: findByStatus(WAITING)
        SCH->>SCH: Collections.shuffle(waitingUsers)

        loop 대기열 각 사용자
            SCH->>SCH: resolvePhase(requester)\n대기 시간 → Phase 1~5

            SCH->>REPO: findPhaseNMatch(...)\nFOR UPDATE SKIP LOCKED

            alt 파트너 발견
                SCH->>MS: confirmMatch(user1Id, user2Id)
                MS->>REPO: updateStatusIf([u1,u2], WAITING→MATCHED)
                alt updated == 2 (성공)
                    MS->>DB: roomRepository.save(Room)
                    MS-->>SCH: 완료
                else updated != 2 (충돌)
                    MS-->>SCH: throw MatchAlreadyProcessedException
                    SCH->>SCH: warn 로그 후 다음 사용자로 계속
                end
            else 파트너 없음
                SCH->>SCH: 다음 사용자로 계속
            end
        end
    end
```

---

## 5. Phase 1~5 조건 완화

### 대기 시간 → Phase 결정

```mermaid
flowchart LR
    S([대기 시작]) --> P1{대기 < 5초}
    P1 -->|YES| PH1[Phase 1\n가장 엄격]
    P1 -->|NO| P2{대기 < 10초}
    P2 -->|YES| PH2[Phase 2]
    P2 -->|NO| P3{대기 < 20초}
    P3 -->|YES| PH3[Phase 3]
    P3 -->|NO| P4{대기 < 30초}
    P4 -->|YES| PH4[Phase 4]
    P4 -->|NO| PH5[Phase 5\n조건 없음]

    style PH1 fill:#f8d7da,stroke:#842029
    style PH2 fill:#fff3cd,stroke:#856404
    style PH3 fill:#d1ecf1,stroke:#0c5460
    style PH4 fill:#cce5ff,stroke:#004085
    style PH5 fill:#d4edda,stroke:#155724
```

### 각 Phase 활성 조건

| Phase | 대기 시간 | 성별 반대 | 지역 일치 | 나이 ±5 | 취미 공통 | 등급 제외(FERTILIZER) |
|-------|----------|-----------|-----------|---------|-----------|----------------------|
| 1 | 0~5s | ✅ | ✅ | ✅ | ✅ | ✅ |
| 2 | 5~10s | ✅ | ✅ | ✅ | ❌ | ✅ |
| 3 | 10~20s | ✅ | ❌ | ✅ | ❌ | ✅ |
| 4 | 20~30s | ✅ | ❌ | ❌ | ❌ | ✅ |
| 5 | 30s+ | ❌ | ❌ | ❌ | ❌ | ❌ |

> **Phase 5**: `user_id != 본인`만 필터링. 모든 조건 완화.

---

## 6. 동시성 제어 메커니즘

```mermaid
sequenceDiagram
    participant T1 as Thread 1 (스케줄러)
    participant T2 as Thread 2 (스케줄러 or 요청)
    participant DB as PostgreSQL

    Note over T1,T2: 같은 파트너를 동시에 선택 시도

    par Thread 1
        T1->>DB: findPhase1Match(...)\nFOR UPDATE SKIP LOCKED
        DB-->>T1: partner_A (행 락 획득)
    and Thread 2
        T2->>DB: findPhase1Match(...)\nFOR UPDATE SKIP LOCKED
        DB-->>T2: partner_B (락된 partner_A는 SKIP)
    end

    Note over T1,T2: 서로 다른 파트너 선택 → 안전

    T1->>DB: updateStatusIf([user1, partnerA], WAITING→MATCHED)
    DB-->>T1: updated = 2 ✅
    T1->>DB: roomRepository.save(Room)

    T2->>DB: updateStatusIf([user2, partnerB], WAITING→MATCHED)
    DB-->>T2: updated = 2 ✅
    T2->>DB: roomRepository.save(Room)

    Note over T1,T2: 동시 매칭 → 각각 다른 Room 생성

    %% 충돌 케이스
    Note over T1,T2: 동일 파트너 선택 시 충돌 감지

    T1->>DB: updateStatusIf([user1, partnerX], WAITING→MATCHED)
    DB-->>T1: updated = 2 ✅ (먼저 성공)

    T2->>DB: updateStatusIf([user1, partnerX], WAITING→MATCHED)
    DB-->>T2: updated = 0 또는 1 ❌
    T2-->>T2: throw MatchAlreadyProcessedException\n→ 트랜잭션 롤백, 경고 로그
```

### 핵심 안전장치

| 레이어 | 메커니즘 | 효과 |
|--------|---------|------|
| DB 쿼리 | `FOR UPDATE SKIP LOCKED` | 다른 트랜잭션이 락 중인 행은 건너뜀 → 스레드별 독립 파트너 선택 |
| 서비스 | `updateStatusIf` 벌크 업데이트 | `updated != 2`이면 `MatchAlreadyProcessedException` → 트랜잭션 롤백 |
| 스케줄러 | `catch MatchAlreadyProcessedException` | 경합 감지 후 warn 로그, 다음 사용자로 계속 진행 |

---

## 7. 컴포넌트 의존관계

```mermaid
classDiagram
    class MatchController {
        +requestMatch(MatchRequest) MatchResponse
        +cancelMatch(MatchRequest) void
        +getMatchStatus(UUID) MatchResponse
    }

    class MatchService {
        +requestMatch(MatchRequest) MatchResponse
        +confirmMatch(UUID, UUID) void
        +cancelMatch(MatchRequest) void
        +getMatchStatus(UUID) MatchResponse
        -findInterceptPartner(User) Optional~MatchQueue~
        -doInterceptAndReturn(User, MatchQueue) MatchResponse
        -doEnqueueAndReturn(User) MatchResponse
    }

    class MatchScheduler {
        +runMatchingLoop() void
        -findAndProcessMatch(MatchQueue) void
        -resolvePhase(MatchQueue) int
        -findPartnerByPhase(MatchQueue, int) Optional~MatchQueue~
    }

    class MatchQueueMatchFinder {
        <<interface>>
        +findPhase1Match(...) Optional~MatchQueue~
        +findPhase2Match(...) Optional~MatchQueue~
        +findPhase3Match(...) Optional~MatchQueue~
        +findPhase4Match(...) Optional~MatchQueue~
        +findPhase5Match(...) Optional~MatchQueue~
    }

    class MatchQueueMatchFinderPostgres {
        +findPhase1Match(...) Optional~MatchQueue~
        +findPhase2Match(...) Optional~MatchQueue~
        +findPhase3Match(...) Optional~MatchQueue~
        +findPhase4Match(...) Optional~MatchQueue~
        +findPhase5Match(...) Optional~MatchQueue~
    }

    class MatchQueueRepository {
        +findByStatus(MatchStatus) List~MatchQueue~
        +findByUserId(UUID) Optional~MatchQueue~
        +findPhase1Match(...) Optional~MatchQueue~
        +findPhase2Match(...) Optional~MatchQueue~
        +findPhase3Match(...) Optional~MatchQueue~
        +findPhase4Match(...) Optional~MatchQueue~
        +findPhase5Match(...) Optional~MatchQueue~
        +updateStatusIf(List, MatchStatus, MatchStatus) int
    }

    class UserRepository {
        +findById(UUID) Optional~User~
    }

    class RoomRepository {
        +save(Room) Room
        +findFirstByUser1IdOrUser2Id(UUID, UUID) Optional~Room~
    }

    MatchController --> MatchService
    MatchScheduler --> MatchService
    MatchScheduler --> MatchQueueMatchFinder
    MatchScheduler --> MatchQueueRepository
    MatchService --> MatchQueueMatchFinder
    MatchService --> MatchQueueRepository
    MatchService --> UserRepository
    MatchService --> RoomRepository
    MatchQueueMatchFinderPostgres ..|> MatchQueueMatchFinder
    MatchQueueMatchFinderPostgres --> MatchQueueRepository
```

---

## 8. 데이터 모델

```mermaid
erDiagram
    users {
        UUID id PK
        VARCHAR nickname UK
        VARCHAR gender
        DATE birth_date
        VARCHAR region
        INT total_score
        VARCHAR tier
        TIMESTAMP created_at
        TIMESTAMP updated_at
    }

    hobbies {
        INT id PK
        VARCHAR name
    }

    user_hobbies {
        BIGINT id PK
        UUID user_id FK
        INT hobby_id FK
    }

    match_queue {
        BIGINT queue_id PK
        UUID user_id UK
        VARCHAR status
        INT[] hobby_ids
        VARCHAR tier
        VARCHAR location
        INT birth_year
        VARCHAR gender
        TIMESTAMP created_at
    }

    rooms {
        UUID room_id PK
        UUID user1_id FK
        UUID user2_id FK
        TIMESTAMP created_at
        TIMESTAMP updated_at
    }

    users ||--o{ user_hobbies : "가진다"
    hobbies ||--o{ user_hobbies : "속한다"
    users ||--o| match_queue : "대기열 등록"
    users ||--o{ rooms : "user1로 참여"
    users ||--o{ rooms : "user2로 참여"
```

> **match_queue.hobby_ids**: PostgreSQL `integer[]` 배열. GIN 인덱스(`&&` 연산자)로 취미 교집합 필터링.
> **match_queue.status**: `WAITING` → `MATCHED` 단방향 전이. `updateStatusIf`가 원자적으로 처리.

---

## 9. 모니터링 스택

```mermaid
flowchart LR
    APP[Spring Boot\nmatching_poc]

    APP -->|/actuator/prometheus\nHTTP Scrape| PROM[Prometheus]
    PROM -->|PromQL| GRAF[Grafana\nDashboard]

    APP -->|OTLP HTTP\nlocalhost:14318| TEMPO[Tempo\n분산 트레이싱]
    TEMPO -->|TraceQL| GRAF

    APP -->|JSON 로그\n./logs/application.log| PROM_TAIL[Promtail]
    PROM_TAIL -->|LogQL| LOKI[Loki]
    LOKI -->|LogQL| GRAF

    style APP fill:#f0f0f0,stroke:#666
    style GRAF fill:#f46800,color:#fff,stroke:#c44d00
    style PROM fill:#e6522c,color:#fff,stroke:#c44d00
    style TEMPO fill:#7B45CB,color:#fff,stroke:#5a2da0
    style LOKI fill:#f0a500,color:#fff,stroke:#c07d00
```

### 커스텀 메트릭

| 메트릭 이름 | 타입 | 설명 |
|------------|------|------|
| `match.request.immediate` | Counter | 즉시 매칭(인터셉트) 성공 횟수 |
| `match.request.enqueued` | Counter | 대기열 등록 횟수 |
| `match.confirm.success` | Counter | 스케줄러 매칭 확정 성공 횟수 |
| `match.confirm.conflict` | Counter | `updateStatusIf` 충돌 횟수 (`MatchAlreadyProcessedException`) |
| `match.queue.waiting.count` | Gauge | 현재 WAITING 상태 대기열 인원 수 |
| `match.scheduler.loop` | Observation | 스케줄러 루프 1회 실행 스팬 (Tempo) |
| `match.attempt` | Observation | 개별 매칭 시도 스팬. `phase`, `result` 태그 포함 |

> `match.attempt`의 `result` 태그: `matched` / `none` / `conflict` / `error`
