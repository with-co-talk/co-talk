# Co-Talk Project Rules

## Project Overview
Co-Talk은 Java/Spring Boot 기반의 백엔드 프로젝트입니다.

## Architecture
- **Hexagonal Architecture (Ports and Adapters)**
  - `adapter/inbound`: REST 컨트롤러, 외부 요청 처리
  - `adapter/outbound`: DB, 외부 API 연동
  - `application`: 유스케이스, 서비스 로직
  - `domain`: 도메인 엔티티, 비즈니스 로직

## Code Conventions

### Java
- Java 17+ 사용
- **Effective Java 준수**: Joshua Bloch의 Effective Java (3rd Edition) 가이드라인 전반을 따르기
- 변수명, 메서드명: camelCase
- 클래스명: PascalCase
- 상수: UPPER_SNAKE_CASE
- 패키지명: lowercase
- **JavaDoc 필수 작성**: 모든 public 클래스, 메서드, 필드에 JavaDoc 작성
  - 클래스: 클래스의 역할과 책임 설명
  - 메서드: `@param`, `@return`, `@throws` 태그 포함
  - 간단한 getter/setter는 생략 가능
- **풀 패키지 경로 사용 금지**: 모든 클래스는 `import` 문을 사용해야 함
  - ❌ `java.lang.reflect.Field field = ...`
  - ✅ `import java.lang.reflect.Field;` 후 `Field field = ...`
  - ❌ `com.cotalk.domain.entity.BaseEntity.class`
  - ✅ `import com.cotalk.domain.entity.BaseEntity;` 후 `BaseEntity.class`

### Spring Boot
- REST API는 `/api/v1` prefix 사용
- DTO는 record 타입 권장
- 의존성 주입은 생성자 주입 사용

### Testing
- 단위 테스트: JUnit 5 + Mockito
- 통합 테스트: @SpringBootTest
- 테스트 메서드명: `should_예상결과_when_조건` 형식

## Git Conventions
- 커밋 메시지: `type: 설명` 형식
  - feat: 새로운 기능
  - fix: 버그 수정
  - refactor: 리팩토링
  - docs: 문서 수정
  - test: 테스트 추가/수정
- 브랜치: `feature/기능명`, `fix/버그명`

## Development Methodology
- **TDD (Test-Driven Development) 필수**
  - RED: 실패하는 테스트 먼저 작성
  - GREEN: 테스트를 통과하는 최소한의 코드 구현
  - REFACTOR: 코드 개선
- 모든 비즈니스 로직(Application 서비스)은 테스트 먼저 작성
- 컨트롤러는 테스트 먼저 작성 후 구현
- 테스트 없이 프로덕션 코드 작성 금지

## Response Language
- 한국어로 응답
