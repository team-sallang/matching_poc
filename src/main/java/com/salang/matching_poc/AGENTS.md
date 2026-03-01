<!-- Parent: ../../../../../../AGENTS.md -->
<!-- Generated: 2026-03-01 | Updated: 2026-03-01 -->

# com.salang.matching_poc (Main Package)

## Purpose
매칭 서비스 애플리케이션의 루트 패키지. Spring Boot 진입점과 모든 기능 서브패키지를 포함한다. 사용자 간 소셜 매칭(단계적 조건 매칭)을 처리하며, 매칭 큐 관리, 채팅방 생성, 실시간 모니터링을 지원한다.

## Key Files

| File | Description |
|------|-------------|
| `MatchingPocApplication.java` | Spring Boot 진입점 — `@SpringBootApplication`, 스케줄링 활성화(`@EnableScheduling`) |

## Subdirectories

| Directory | Purpose |
|-----------|---------|
| `constants/` | 매칭 알고리즘 전역 상수 (see `constants/AGENTS.md`) |
| `controller/` | REST API 컨트롤러 및 DTO (see `controller/AGENTS.md`) |
| `exception/` | 커스텀 예외 클래스 및 전역 예외 핸들러 (see `exception/AGENTS.md`) |
| `model/` | JPA 엔티티, 열거형 (see `model/AGENTS.md`) |
| `repository/` | Spring Data JPA 리포지토리 — 커스텀 쿼리 포함 (see `repository/AGENTS.md`) |
| `service/` | 비즈니스 로직 서비스 레이어 (see `service/AGENTS.md`) |
| `worker/` | 스케줄러 — 주기적 매칭 처리 (see `worker/AGENTS.md`) |

## For AI Agents

### Architecture Overview
```
HTTP 요청
    ↓
MatchController (controller/)
    ↓
MatchService (service/)
    ├── 즉시 매칭: MatchQueueMatchFinder → Room 생성 → MATCHED 처리
    └── 대기열 등록: MatchQueue 저장 → WAITING 상태
         ↑
MatchScheduler (worker/) — 1초 주기 스케줄
    ├── Phase 1~5 단계별 조건 완화 매칭 시도
    └── confirmMatch() → 원자적 상태 업데이트 + Room 생성
```

### Working In This Directory
- 새 기능 패키지 추가 시 이 AGENTS.md의 Subdirectories 표 업데이트.
- Lombok 어노테이션 사용: `@Getter`, `@Builder`, `@RequiredArgsConstructor`, `@Slf4j`.
- 모든 서비스 메서드에 적절한 `@Transactional` 적용.
- Micrometer로 비즈니스 메트릭 카운터/게이지 등록.

### Testing Requirements
- `./gradlew test` 로 전체 테스트 실행.
- 컨트롤러: `@WebMvcTest` + MockMvc.
- 서비스/리포지토리: H2 기반 슬라이스 테스트.
- PostgreSQL 네이티브 쿼리: 실제 DB 환경에서 별도 검증.

### Common Patterns
- 동시성 안전: `for update skip locked` 네이티브 쿼리로 선점 잠금.
- 원자적 상태 전환: `updateStatusIf()` JPQL 벌크 업데이트.
- `MatchAlreadyProcessedException`으로 경쟁 조건 감지 및 롤백.

## Dependencies

### Internal
- `supabase/migrations/` — DB 스키마
- `src/main/resources/application.yaml` — 설정

### External
- Spring Boot 3.4 (Web, JPA, Actuator)
- PostgreSQL 드라이버
- Micrometer + OpenTelemetry
- Lombok
- springdoc-openapi (Swagger)

<!-- MANUAL: -->
