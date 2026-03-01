## 프로젝트 정보
- 언어: Java 17+
- 프레임워크: Spring Boot 3.x
- 빌드: `./gradlew build`
- 컴파일: `./gradlew compileJava`
- 테스트: `./gradlew test`
- 린트: `./gradlew checkstyleMain` (또는 `./gradlew spotlessCheck`)

## 리팩토링 규칙
- 한 번에 하나의 파일만 수정
- public API(URL, 요청/응답 형태)는 변경 금지
- 수정 후 반드시 테스트 실행
- 테스트 실패 시 테스트가 아닌 코드를 수정
- Entity 변경 시 반드시 `supabase/migrations/`에 마이그레이션 SQL 추가
- PostgreSQL 네이티브 쿼리 변경 시 실제 DB 환경에서 검증 필수 (H2 미지원)

## TDD 원칙
- 모든 변경은 실패하는 테스트를 먼저 작성한 뒤 코드를 수정
- 테스트 없이 프로덕션 코드를 수정하지 않는다
- 테스트 이름은 한국어로 동작 설명 (`@DisplayName` 한국어 활용)
- Red → Green → Refactor 사이클

## 검토 모드 (중요)
- 코드 수정 후 바로 다음 단계로 넘어가지 마라
- 수정할 때마다 보여줘:
  1. 무엇을 왜 변경했는지 한국어로 설명
  2. 변경 전후 핵심 코드 비교
  3. 이 변경으로 배울 수 있는 개념
- 내가 "확인" 또는 "ㅇㅇ"이라고 할 때까지 대기

## 노션 학습 기록
- 새로운 개념을 설명했을 때 다음과 같이 물어봐:
  "📝 이 개념을 노션에 정리할까요? (제목: OOO / 분야: OOO)"
- 내가 "ㅇㅇ" 또는 "노션" → 기록
- 내가 "패스" 또는 "ㄴㄴ" → 건너뜀

## 설계 논의 모드
- 코드 수정 시 설계 선택지가 2개 이상이면 바로 구현하지 말고 선택지를 먼저 보여줘
- 각 선택지마다:
  1. 어떻게 구현하는지 (코드 스케치)
  2. 장점
  3. 단점 (트레이드오프)
  4. 실무에서 보통 어떤 걸 선택하는지
- 내가 선택한 뒤에 구현

## Java/Spring 컨벤션
- Entity는 일반 class (기본 생성자 + Getter 필수, JPA 요구사항)
- Entity Lombok 조합: `@Getter` + `@NoArgsConstructor(access = AccessLevel.PROTECTED)` + `@Builder` + `@AllArgsConstructor(access = AccessLevel.PRIVATE)`
- Entity에 `@Data`, `@EqualsAndHashCode`, `@ToString` 금지 (순환 참조 + lazy loading 트리거 위험)
- Entity equals/hashCode: ID 기반 직접 구현 (id가 null이면 false, hashCode는 `getClass().hashCode()` 고정값)
- DTO는 `record` 사용 (불변 보장, equals/hashCode/toString 자동)
- 필드는 `final` 우선, 가변 필드 최소화
- 생성자 주입 사용 (`@Autowired` 필드 주입 금지)
- `Optional`은 반환 타입에만 사용 (파라미터, 필드에 사용 금지)
- Stream API 적극 활용하되, 중첩 stream은 메서드 분리
- `var`는 로컬 변수에서만 사용 (타입이 명확할 때)
- 상수는 `private static final`로 정의
- 예외는 커스텀 예외 클래스 사용 (`RuntimeException` 직접 throw 금지)
- `@Transactional`: Service 클래스 레벨에 `@Transactional(readOnly = true)` 기본, 쓰기 메서드에만 `@Transactional` 오버라이드
- Repository에 `@Transactional` 별도 추가 불필요 (Spring Data JPA 내부적으로 적용됨), 트랜잭션 경계는 Service에서 관리
