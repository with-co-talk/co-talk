## 개요
BaseEntity 적용을 통한 코드 중복 제거 및 주요 기능 개선

## 목표
- 엔티티 코드 중복 제거 (BaseEntity 적용)
- 메시지 반응 기능 개선
- Rate Limit 기능 강화
- 보안 및 JWT 개선
- WebSocket 코드 정리
- 테스트 코드 보완

## 주요 변경사항

### BaseEntity 리팩토링
- BaseEntity 클래스 추가 및 JPA Auditing 설정
- 모든 엔티티에 BaseEntity 상속 적용 (13개)
- @PrePersist/@PreUpdate 제거로 코드 중복 제거

### 메시지 반응 기능 개선
- 그룹화된 반응 응답 추가
- GetMessageReactionsService 개선
- Emoji enum 및 이모지 유효성 검증 강화

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
