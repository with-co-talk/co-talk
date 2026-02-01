## 📋 개요
아키텍처 전반 리팩토링 및 Presence 기능 추가

## 🎯 변경사항

### 도메인 레이어 개선
- ChatRoomMember 엔티티 수정
- ChatMessageBroker, ChatRoomMemberRepository, MessageRepository, UserEventBroker 포트 인터페이스 변경

### Repository 레이어 개선
- ChatRoomMemberRepositoryAdapter 수정
- MessageRepositoryAdapter 수정
- 관련 JPA Repository 인터페이스 변경

### Service 레이어 개선
- CreateChatRoomService 수정
- CreateGroupChatRoomService 수정
- GetChatRoomsService 수정
- MarkAsReadService 수정
- SendMessageService 수정

### Controller 레이어 개선
- 모든 REST Controller 수정 (Auth, Block, ChatMessage, ChatRoom, File, Friend, MessageSearch, NotificationSetting, Report, User)
- WebSocket Controller 및 DTO 변경

### Infrastructure 레이어 개선
- Messaging 관련 클래스 수정 (InMemoryChatMessageBroker, RedisChatMessageBroker, RedisChatMessageSubscriber, RedisUserEventBroker, RedisUserEventSubscriber)
- SecurityContextHelper 수정
- WebSocketEventListener 수정

### Presence 기능 추가 ✨
- PresenceInactiveRequest DTO 추가
- PresencePingRequest DTO 추가
- ChatRoomPresenceTracker 포트 추가
- Presence Infrastructure 구현 추가 (InMemoryChatRoomPresenceTracker, RedisChatRoomPresenceTracker)

### 데이터베이스 마이그레이션
- V4 마이그레이션 파일 추가 (last_read_message_id 컬럼 추가)
- LocalDataInitializer 수정

## 🔗 관련 이슈
이슈는 수동으로 생성해주세요.

## ✅ 체크리스트
- [x] 코드 리뷰 준비 완료
- [x] 테스트 통과
- [x] 문서 업데이트 완료

## 📝 테스트 방법
1. 도메인 레이어: 엔티티 및 포트 인터페이스 변경사항 확인
2. Repository: 데이터 접근 로직 정상 동작 확인
3. Service: 비즈니스 로직 정상 동작 확인
4. Controller: REST API 및 WebSocket 정상 동작 확인
5. Presence: 채팅방 사용자 활동 상태 추적 기능 확인
6. 마이그레이션: 데이터베이스 스키마 변경 확인

## 🔍 리뷰 포인트
- 헥사고날 아키텍처 원칙 준수 여부
- 포트와 어댑터 분리 적절성
- Presence 기능 구현의 확장성
- 데이터베이스 마이그레이션 안전성

## 통계
- 변경 파일: 68개
- 추가: 5,873줄 / 삭제: 331줄
- 커밋: 12개 (논리적 단위로 분리)
