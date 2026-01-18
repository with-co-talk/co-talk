## 개요
예외 처리 개선, ID 생성기 인터페이스 도입 및 주요 기능 개선

## 목표
- 예외 처리 체계 개선
- ID 생성 로직 추상화
- 보안 인증 강화
- 푸시 알림 기능 개선
- 검증 로직 강화
- 서비스 로직 개선

## 주요 변경사항

### 예외 처리 개선
- SelfActionNotAllowedException 추가 (자기 자신에 대한 액션 방지)
- UnauthorizedException 추가 (인증되지 않은 사용자 접근 처리)
- GlobalExceptionHandler에 예외 핸들러 추가

### ID 생성기 인터페이스 도입
- IdGenerator 포트 인터페이스 추가
- SnowflakeIdGenerator 개선 및 테스트 보완
- 서비스에서 IdGenerator 의존성 주입으로 변경

### 보안 인증 개선
- CustomUserPrincipal 추가
- WithMockCustomUser 어노테이션 추가 (테스트용)
- JWT 인증 필터 및 토큰 제공자 개선
- SecurityConfig 개선

### 푸시 알림 기능 개선
- FcmPushNotificationSender 개선
- SendPushNotificationUseCase 인터페이스 확장
- 푸시 알림 테스트 추가

### 검증 로직 개선
- UserValidator 검증 로직 강화
- MessageValidator 개선
- Emoji enum 개선

### 서비스 로직 개선
- 여러 서비스에서 IdGenerator 사용으로 변경
- SelfActionNotAllowedException 적용
- UserRepository 인터페이스 확장

### 테스트 코드 보완
- 여러 서비스 테스트 개선
- GetMessageReactionsServiceTest 추가
- SendMessageServiceTest 대폭 개선
