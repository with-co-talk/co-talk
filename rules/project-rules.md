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
- 변수명, 메서드명: camelCase
- 클래스명: PascalCase
- 상수: UPPER_SNAKE_CASE
- 패키지명: lowercase

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

## Response Language
- 한국어로 응답
