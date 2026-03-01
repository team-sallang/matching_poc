<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-03-01 | Updated: 2026-03-01 -->

# model/entity/base

## Purpose
JPA 엔티티 공통 감사(Audit) 필드 베이스 클래스. `createdAt`, `updatedAt` 자동 관리를 제공한다.

## Key Files

| File | Description |
|------|-------------|
| `BaseTimeEntity.java` | `@MappedSuperclass` — `createdAt`(삽입 시), `updatedAt`(수정 시) 자동 설정; `@EntityListeners(AuditingEntityListener.class)` |

## For AI Agents

### Working In This Directory
- `User`, `Room` 엔티티가 상속. `MatchQueue`는 대기 시간 계산을 위해 직접 `@PrePersist`로 관리.
- Spring Data JPA Auditing 활성화 필요 (`@EnableJpaAuditing` — `MatchingPocApplication`에 설정).

<!-- MANUAL: -->
