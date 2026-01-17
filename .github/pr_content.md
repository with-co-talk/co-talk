## 개요
BaseEntity 적용을 통한 코드 중복 제거 및 주요 기능 개선

## 변경사항

### BaseEntity 리팩토링
- BaseEntity 클래스 추가 및 JPA Auditing 설정
- 모든 엔티티에 BaseEntity 상속 적용 (13개)
- @PrePersist/@PreUpdate 제거로 코드 중복 제거

### 메시지 반응 기능 개선
- 그룹화된 반응 응답 추가
- GetMessageReactionsService 개선
- Emoji enum 및 이모지 유효성 검증 강화
- WebSocket DTO 추가 (7개)

### Rate Limit 기능 개선
- RateLimitInterceptor 로직 개선
- RateLimitProperties 설정 개선

### 보안 및 JWT 개선
- JwtTokenProvider 기능 강화
- SecurityConfig 및 JwtAuthenticationFilter 개선

### WebSocket 코드 정리
- ChatWebSocketController 코드 간소화
- 메시징 브로커 코드 정리

### 테스트 코드 보완
- 새로운 테스트 추가 (15개)
- 기존 테스트 코드 정리

### 인프라 설정 업데이트
- build.gradle.kts 의존성 업데이트
- docker-compose.dev.yml 설정 업데이트

## 통계
- 변경 파일: 95개
- 추가: 5,803줄 / 삭제: 691줄
- 커밋: 7개 (논리적 단위로 분리)

## 관련 이슈
Closes #[이슈번호]

## 체크리스트
- [x] 코드 리뷰 준비 완료
- [x] 테스트 통과
- [x] 문서 업데이트 완료

## 테스트 방법
1. BaseEntity: 엔티티의 createdAt, updatedAt 자동 관리 확인
2. 메시지 반응: 그룹화된 반응 응답 확인
3. Rate Limit: 설정된 제한값으로 동작 확인
4. 보안: JWT 토큰 생성/검증 확인
5. WebSocket: 메시지 전송/수신 확인

## 리뷰 포인트
- BaseEntity 상속 구조 적용 여부
- 메시지 반응 그룹화 로직 정확성
- Rate Limit 설정 적절성
- 보안 개선사항 안전성
