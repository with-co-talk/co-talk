# 기능 검증 보고서 (2026-02-07)

> **문서 목차**: [docs/README.md](./README.md)

## 1. 검증 요약

| 항목 | 결과 | 비고 |
|------|------|------|
| 테스트 실행 | ✅ 통과 | `./gradlew test --rerun-tasks` 성공 |
| TDD/테스트 규칙 | ⚠️ 일부 미준수 | DisplayName 형식, Given-When-Then 일부 |
| 헥사고날 아키텍처 | ❌ 위반 사항 있음 | 도메인 JPA/Spring, Use Case의 인프라 직접 의존 |
| 기능 구현 품질 | ✅ 전반 양호 | 포트 기반 Use Case, 예외/검증 처리 |
| 테스트 없는 서비스 | ⚠️ 3개 | HideFriend, UnhideFriend, GetHiddenFriends |

---

## 2. 테스트 실행 결과

- **전체 테스트**: `./gradlew test --rerun-tasks` → **BUILD SUCCESSFUL**
- **리액티브 테스트**: 테스트 코드에서 `Mono/Flux.block()` 사용 없음 (금지 준수)
- **Given-When-Then**: 다수 테스트에 `// given`, `// when`, `// then` 주석 사용

---

## 3. TDD / 테스트 규칙 검토

### 3.1 잘 지킨 점

- 테스트 클래스/메서드에 **@DisplayName** 사용 (대부분의 테스트 클래스에 존재)
- **Given-When-Then** 구조를 가진 테스트 다수
- 단위 테스트는 **Mock + ExtendWith(MockitoExtension.class)** 로 격리
- **AssertJ** 사용
- 테스트 명명: `should_ReturnX_when_Y` 등 시나리오가 드러나는 이름 사용

### 3.2 미준수 사항

1. **@DisplayName 형식**
   - 규칙: `"🔴 RED: {테스트 설명}"` 형식
   - 현황: `"🔴"` 접두어를 사용한 테스트가 **0건** (대부분 `"SendMessageService"`, `"메시지 전송 성공 시"` 등)
   - 권장: 규칙을 유지하려면 클래스/메서드 중 하나에 `🔴 RED:` 형식 적용, 또는 규칙 문서를 현재 관례에 맞게 수정

2. **리액티브 테스트 (StepVerifier)**
   - 규칙: 리액티브 스트림은 `StepVerifier`로 검증, `block()` 금지
   - 현황: Application 서비스 계층이 **Mono/Flux를 반환하지 않음** (동기 API) → StepVerifier 사용처 없음
   - 참고: 프로젝트는 `spring-boot-starter-web` 사용 (WebFlux 아님). 규칙 문서의 "Spring WebFlux (리액티브)"와 실제 스택이 다름.

3. **테스트가 없는 Use Case**
   - `HideFriendService`
   - `UnhideFriendService`
   - `GetHiddenFriendsService`  
   → 통합 테스트로 간접 커버 여부는 별도 확인 필요. TDD 관점에서는 단위 테스트 추가 권장.

---

## 4. 헥사고날 아키텍처 검토

### 4.1 위반 사항 (수정 완료·예정)

1. **도메인 계층 JPA 분리 (User 완료, 나머지 유지)**
   - **User**: 순수 도메인 `User`(extends `DomainBaseEntity`) + `adapter/outbound/persistence/entity/UserJpaEntity` + `UserMapper`로 완전 분리. JPQL/쿼리는 `UserJpaEntity` 사용.
   - **나머지 엔티티**(Message, ChatRoom, Friend 등): `domain/entity`에 `@Entity`·BaseEntity(JPA) 유지. 동일 패턴으로 분리 시 JpaEntity + Mapper + Adapter 수정 필요.

2. **도메인 계층 Spring @Component** → **✅ 수정 완료**
   - `domain/validator`에서 `@Component` 제거, `infrastructure/config/DomainValidatorConfig`에서 `@Bean`으로 등록.

3. **Application(Use Case)이 인프라에 직접 의존** → **✅ 수정 완료**
   - `AuthTokenPort`, `PasswordEncoderPort` 도입 후 Use Case는 해당 포트만 사용. `JwtAuthTokenAdapter`, `SpringPasswordEncoderAdapter`가 인프라에서 구현체 주입.

### 4.2 잘 지킨 점

- Use Case는 대부분 **도메인 포트(Repository, UseCase)** 만 사용 (위 3개 제외)
- Controller → Use Case → Port 구조 유지
- 도메인 예외(`domain.exception`)로 비즈니스 오류 표현

---

## 5. 기능/구현 품질 요약

- **메시지 전송**: 멤버 검증, XSS 방지(HtmlSanitizer), 메시지 검증, 저장 후 읽음/푸시 처리 등 흐름이 명확함
- **인증**: 로그인 시 온라인 상태 갱신, JWT 발급 등 요구사항 반영
- **예외**: 도메인 예외 클래스가 잘 분리되어 있음
- **테스트**: 대부분의 서비스에 대응하는 단위 테스트 존재, Mock으로 격리

---

## 6. 권장 조치 (우선순위)

| 우선순위 | 항목 | 조치 | 상태 |
|----------|------|------|------|
| 높음 | Use Case의 인프라 의존 | JWT/비밀번호 검증을 포트로 추출, Use Case는 포트만 사용 | ✅ 완료 (AuthTokenPort, PasswordEncoderPort 도입) |
| 높음 | 도메인 Spring 제거 | 도메인 검증기에서 @Component 제거, 인프라에서 @Bean 등록 | ✅ 완료 (DomainValidatorConfig) |
| 높음 | 도메인 JPA 분리 | 엔티티를 순수 도메인으로 분리하고 persistence 어댑터에서 JPA 매핑 | ✅ User 완료 (UserJpaEntity, UserMapper, DomainBaseEntity). 나머지 엔티티는 기존 BaseEntity(JPA) 유지 |
| 중간 | DisplayName 형식 | 규칙과 실제 중 하나로 통일 | 미적용 |
| 중간 | 테스트 없는 서비스 | HideFriendService, UnhideFriendService, GetHiddenFriendsService 단위 테스트 추가 | 미적용 |
| 낮음 | WebFlux vs Web | 규칙 문서의 "Spring WebFlux" 표현을 현재 스택에 맞게 수정 | 미적용 |

---

## 7. 결론

- **테스트**: 전반적으로 잘 작성되어 있으며, 실행 결과 모두 통과. TDD 관례(DisplayName 형식, 리액티브 규칙)는 문서와 실제가 일부 어긋나 있음.
- **아키텍처**: 포트/어댑터 구조는 유지되나, **도메인에 JPA/Spring 사용**과 **Use Case의 JWT/PasswordEncoder 직접 의존**은 헥사고날 규칙 위반으로 정리됨.
- **기능**: 검토한 범위 내에서 로직과 예외 처리, 검증은 일관되게 구현되어 있음.

위 권장 조치를 순차적으로 적용하면 규칙 준수도와 유지보수성이 더 좋아질 것입니다.
