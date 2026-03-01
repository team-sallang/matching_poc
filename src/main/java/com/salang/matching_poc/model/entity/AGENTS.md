<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-03-01 | Updated: 2026-03-01 -->

# model/entity

## Purpose
JPA `@Entity` 클래스. PostgreSQL 테이블과 1:1로 매핑되며 도메인 객체의 영속성 계층을 구성한다.

## Key Files

| File | Description |
|------|-------------|
| `User.java` | 사용자 엔티티 — UUID PK, nickname, gender, birthDate, region, totalScore, tier; `BaseTimeEntity` 상속 |
| `MatchQueue.java` | 매칭 대기열 엔티티 — userId, status(WAITING/MATCHED), hobbyIds(배열), tier, region, birthYear, gender; 복합 인덱스(status, created_at) |
| `Room.java` | 매칭 완료 채팅방 — UUID roomId, user1, user2 FK; `BaseTimeEntity` 상속 |
| `Hobby.java` | 취미 엔티티 — Integer PK, 취미명 |
| `UserHobby.java` | 사용자-취미 연결 엔티티 (N:M 분해 테이블) |

## Subdirectories

| Directory | Purpose |
|-----------|---------|
| `base/` | 공통 감사 필드 베이스 클래스 (see `base/AGENTS.md`) |

## For AI Agents

### Working In This Directory
- 엔티티 변경(필드 추가·삭제·타입 변경) 시 `supabase/migrations/`에 새 마이그레이션 SQL 추가 필수.
- `MatchQueue.hobbyIds`는 `@JdbcTypeCode(SqlTypes.ARRAY)`로 PostgreSQL `integer[]` 배열 매핑 — H2 호환 안 됨.
- `User.id`는 `@GeneratedValue(generator = "uuid2")` — Supabase/PostgreSQL의 UUID 생성과 연동.
- `MatchQueue`는 `BaseTimeEntity`를 상속하지 않고 `@PrePersist`로 `createdAt` 직접 관리 (스케줄러에서 대기 시간 계산에 사용).
- Lombok `@Builder`는 빌더 생성자에만 적용 — `@NoArgsConstructor(AccessLevel.PROTECTED)`로 JPA 기본 생성자 보호.

### Common Patterns
- 엔티티 불변성: setter 미노출, 상태 변경은 `setStatus()`처럼 최소한의 setter만 허용.
- `@Enumerated(EnumType.STRING)` — DB에 문자열로 저장, enum 이름 변경 시 마이그레이션 필요.

## Dependencies

### Internal
- `model/entity/base/BaseTimeEntity.java` — createdAt, updatedAt 공통 필드
- `model/enums/` — Gender, Region, Tier, MatchStatus 열거형
- `supabase/migrations/` — 실제 스키마와 동기화

<!-- MANUAL: -->
