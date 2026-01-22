# API 명세서 (v1.1 - PoC Stage)

## 1. 기본 정보

- **Base URL**: `/api/v1`
- **인증 (Authentication)**: 현재 PoC(기술 검증) 단계에서는 별도의 인증 절차를 생략합니다. API 요청 시, **Request Body에 `user_id`를 직접 포함**하여 요청하는 사용자를 식별합니다.

---

## 2. 매칭 API

### 2.1. `POST /match` - 매칭 요청

특정 사용자(`user_id`)가 실시간 매칭을 요청합니다. 시스템은 즉시 매칭(Intercept)을 시도하며, 성공 시 즉시 `room_id`를 반환하고, 실패 시 사용자를 매칭 대기열에 등록하고 `WAITING` 상태를 반환합니다.

- **Request Body**:
    ```json
    {
      "user_id": "a1b2c3d4-e5f6-7890-1234-000000000001"
    }
    ```

- **Responses**:
    - **`200 OK` (즉시 매칭 성공)**
        ```json
        {
          "status": "MATCHED",
          "data": {
            "room_id": "a1b2c3d4-e5f6-7890-1234-567890abcdef",
            "matched_at": "2026-01-20T12:00:00Z"
          }
        }
        ```
    - **`202 Accepted` (대기열 등록)**
        ```json
        {
          "status": "WAITING",
          "data": {
            "message": "매칭 대기열에 등록되었습니다. Supabase Realtime을 통해 매칭 결과를 기다려주세요.",
            "queued_at": "2026-01-20T12:00:00Z"
          }
        }
        ```
    - **`404 Not Found` (사용자 없음)**
        ```json
        {
          "status": "ERROR",
          "error": {
            "code": "USER_NOT_FOUND",
            "message": "해당 사용자를 찾을 수 없습니다."
          }
        }
        ```
    - **`409 Conflict` (이미 대기열에 있거나 매칭된 상태)**
        ```json
        {
          "status": "ERROR",
          "error": {
            "code": "ALREADY_IN_QUEUE",
            "message": "이미 매칭 대기열에 등록되어 있습니다."
          }
        }
        ```

### 2.2. `DELETE /match` - 매칭 취소

특정 사용자(`user_id`)가 매칭 대기열에서 자신의 매칭 요청을 취소합니다.

- **Request Body**:
    ```json
    {
      "user_id": "a1b2c3d4-e5f6-7890-1234-000000000001"
    }
    ```

- **Responses**:
    - **`204 No Content` (성공적으로 취소됨)**
    - **`404 Not Found` (대기열에 사용자가 없음)**
        ```json
        {
          "status": "ERROR",
          "error": {
            "code": "NOT_IN_QUEUE",
            "message": "매칭 대기열에 등록되어 있지 않습니다."
          }
        }
        ```

---

## 3. 실시간 이벤트 (Supabase Realtime)

매칭이 성사되면 클라이언트는 Supabase Realtime 서비스를 통해 `rooms` 테이블의 `INSERT` 이벤트를 수신해야 합니다.

- **구독 대상**: `rooms` 테이블
- **필터 조건**: `user1_id` 또는 `user2_id`가 자신의 `user_id`와 일치하는 `INSERT` 이벤트

- **수신 데이터 형식 (Payload)**:
    ```json
    {
      "type": "INSERT",
      "table": "rooms",
      "schema": "public",
      "record": {
        "room_id": "a1b2c3d4-e5f6-7890-1234-567890abcdef",
        "user1_id": "a1b2c3d4-e5f6-7890-1234-000000000001",
        "user2_id": "a1b2c3d4-e5f6-7890-1234-000000000002",
        "created_at": "2026-01-20T12:00:05Z"
      },
      "old_record": null
    }
    ```
