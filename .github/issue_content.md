## 📋 개요
BaseEntity 적용을 통한 코드 중복 제거 및 주요 기능 개선

## 🎯 목표
- 엔티티 코드 중복 제거 (BaseEntity 적용)
- 메시지 반응 기능 개선
- Rate Limit 기능 강화
- 보안 및 JWT 개선
- WebSocket 코드 정리
- 테스트 코드 보완

## 💡 주요 변경사항

### 1. BaseEntity 리팩토링
- BaseEntity 클래스 추가 (createdAt, updatedAt 자동 관리)
- JPA Auditing 설정 추가
- 모든 엔티티에 BaseEntity 상속 적용
- @PrePersist/@PreUpdate 제거로 코드 중복 제거

### 2. 메시지 반응 기능 개선
- 그룹화된 반응 응답(GroupedReactionResponse) 추가
- GetMessageReactionsService 개선
- Emoji enum 및 이모지 유효성 검증 강화
- WebSocket DTO 추가

### 3. Rate Limit 기능 개선
- RateLimitInterceptor 로직 개선
- RateLimitProperties 설정 개선
- 테스트 설정 파일 추가

### 4. 보안 및 JWT 개선
- JwtTokenProvider 기능 강화
- SecurityConfig 개선
- JwtAuthenticationFilter 개선

### 5. WebSocket 코드 정리
- ChatWebSocketController 코드 간소화
- WebSocketConfig 개선
- 메시징 브로커 코드 정리

### 6. 테스트 코드 보완
- 새로운 테스트 추가 (15개)
- 기존 테스트 코드 정리
- 테스트 픽스처 정리

## ✅ 체크리스트
- [x] BaseEntity 적용 완료
- [x] 메시지 반응 기능 개선 완료
- [x] Rate Limit 기능 개선 완료
- [x] 보안 및 JWT 개선 완료
- [x] WebSocket 코드 정리 완료
- [x] 테스트 코드 보완 완료
- [x] 문서화 완료

## 📝 참고사항
- 총 7개의 논리적 커밋으로 분리
- 리팩토링 가이드 문서 추가 (docs/REFACTORING_GUIDE.md)
