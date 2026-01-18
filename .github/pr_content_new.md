## 개요
예외 처리 개선, ID 생성기 인터페이스 도입 및 주요 기능 개선

## 변경사항

### 예외 처리 개선
- SelfActionNotAllowedException 추가 (자기 자신 차단/친구 요청/신고 방지)
- UnauthorizedException 추가 (인증되지 않은 사용자 접근 처리)
- GlobalExceptionHandler에 예외 핸들러 추가

### ID 생성기 인터페이스 도입
- IdGenerator 포트 인터페이스 추가
- SnowflakeIdGenerator 개선 및 테스트 보완
- 모든 서비스에서 IdGenerator 의존성 주입으로 변경

### 보안 인증 개선
- CustomUserPrincipal 추가 (사용자 인증 정보 관리)
- WithMockCustomUser 어노테이션 추가 (테스트용)
- JwtAuthenticationFilter 개선
- JwtTokenProvider 기능 강화
- SecurityConfig 개선
- WebSocketAuthInterceptor 개선

### 푸시 알림 기능 개선
- FcmPushNotificationSender 개선
- SendPushNotificationUseCase 인터페이스 확장
- SendPushNotificationService 개선
- 푸시 알림 테스트 추가

### 검증 로직 개선
- UserValidator 검증 로직 강화
- MessageValidator 개선
- Emoji enum 개선
- 검증 로직 테스트 보완

### 서비스 로직 개선
- 여러 서비스에서 IdGenerator 사용으로 변경
- SelfActionNotAllowedException 적용 (차단, 친구 요청, 신고)
- UserRepository 인터페이스 확장
- 서비스 로직 개선 및 리팩토링

### 테스트 코드 보완
- 여러 서비스 테스트 개선
- GetMessageReactionsServiceTest 추가
- SendMessageServiceTest 대폭 개선
- RateLimitIntegrationTest 개선

### 설정 파일 업데이트
- OpenApiConfig 개선
- RateLimitInterceptor 설정 개선
- application.yml 설정 파일 업데이트 (모든 환경)
- project-rules.md 업데이트

## 통계
- 변경 파일: 62개
- 추가: 2,467줄 / 삭제: 532줄
- 커밋: 9개 (논리적 단위로 분리)

## 관련 이슈
Closes #[이슈번호]

## 체크리스트
- [x] 코드 리뷰 준비 완료
- [x] 테스트 통과
- [x] 문서 업데이트 완료

## 테스트 방법
1. 예외 처리: 자기 자신 차단/친구 요청/신고 시도 시 예외 발생 확인
2. ID 생성: IdGenerator를 통한 ID 생성 확인
3. 보안: JWT 토큰 생성/검증 확인
4. 푸시 알림: FCM을 통한 푸시 알림 전송 확인
5. 검증: UserValidator, MessageValidator 동작 확인

## 리뷰 포인트
- 예외 처리 로직이 적절한지
- ID 생성기 인터페이스 도입이 올바른지
- 보안 개선사항이 안전한지
- 푸시 알림 기능이 정상 동작하는지
