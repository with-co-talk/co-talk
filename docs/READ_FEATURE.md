# 읽기 기능 (Read Feature) 가이드

## 📋 개요

Co-Talk의 읽기 기능은 카카오톡/라인 스타일의 읽음 표시 시스템입니다. 사용자가 채팅방을 열어 메시지를 읽으면, 서버는 이를 기록하고 다른 사용자들에게 읽음 상태를 실시간으로 전달합니다.

## 🔄 동작 흐름

### 1. 읽음 처리 요청

**REST API 엔드포인트:**
```
POST /api/v1/chat/rooms/{roomId}/read
```

**요청:**
- 인증: JWT 토큰 필요 (Authorization 헤더)
- Path Parameter: `roomId` (채팅방 ID)
- Body: 없음 (사용자 ID는 JWT에서 추출)

**응답:**
```json
{
  "message": "읽음 처리되었습니다."
}
```

**참고:**
- 읽음 처리는 **REST API만 사용**합니다 (하이브리드 방식)
- 요청은 REST API로 전송하고, 업데이트는 WebSocket으로 수신합니다
- WebSocket 엔드포인트는 제공하지 않습니다

**동작:**
1. 서버가 현재 채팅방의 마지막 메시지 ID를 조회
2. 사용자의 `lastReadMessageId`를 마지막 메시지 ID로 업데이트
3. 원자적 업데이트로 동시성 제어 (Lost Update 방지)
4. 여러 이벤트를 WebSocket으로 브로드캐스트

### 2. 서버가 브로드캐스트하는 이벤트

읽음 처리 후 서버는 다음 이벤트들을 WebSocket으로 전송합니다:

#### 2.1. 업데이트된 메시지 (`/topic/chat/room/{roomId}`)

**목적:** 각 메시지의 `unreadCount`를 업데이트하여 클라이언트에 전송

**전송 내용:**
- 최근 50개 메시지
- 각 메시지의 업데이트된 `unreadCount` 포함
- 메시지 형식: `ChatBroadcastMessage`

**예시:**
```json
{
  "id": 123,
  "senderId": 1,
  "chatRoomId": 10,
  "content": "안녕하세요",
  "type": "TEXT",
  "createdAt": 1706256000000,
  "unreadCount": 2  // 업데이트된 읽지 않은 멤버 수
}
```

**클라이언트 처리:**
- 기존 메시지가 있으면 서버가 보내준 값으로 업데이트
- `unreadCount`를 서버 값으로 교체

#### 2.2. 채팅방 READ 이벤트 (`/topic/chat/room/{roomId}`)

**목적:** 채팅방 화면에서 읽음 상태를 실시간으로 반영

**전송 내용:**
```json
{
  "schemaVersion": 1,
  "eventId": "read-receipt:10:1:123",
  "eventType": "READ",
  "chatRoomId": 10,
  "userId": 1,
  "lastReadMessageId": 123,
  "lastReadAt": "2024-01-26T10:30:00"
}
```

**클라이언트 처리:**
- 읽음 이벤트를 받아 UI에 반영 (선택사항)
- 주로 메시지 업데이트로 처리하는 것이 권장됨

#### 2.3. 읽음 영수증 이벤트 (`/topic/user/{userId}/read-receipt`)

**목적:** 모든 채팅방 멤버에게 읽음 상태 알림

**전송 대상:** 채팅방의 모든 멤버 (읽은 사용자 본인 포함)

**전송 내용:**
```json
{
  "schemaVersion": 1,
  "eventId": "read-receipt:10:1:123",
  "chatRoomId": 10,
  "userId": 1,
  "lastReadMessageId": 123,
  "lastReadAt": "2024-01-26T10:30:00"
}
```

#### 2.4. 채팅 목록 업데이트 (`/topic/user/{userId}/chat-list`)

**목적:** 채팅 목록의 `unreadCount` 업데이트

**전송 내용:**
```json
{
  "schemaVersion": 1,
  "eventId": "chat-list:READ:10:1:123",
  "eventType": "READ",
  "roomId": 10,
  "lastMessage": "안녕하세요",
  "lastMessageType": "TEXT",
  "lastMessageAt": "2024-01-26T10:30:00",
  "senderId": 2,
  "senderNickname": "홍길동",
  "unreadCount": 0  // 수신자별로 계산된 값
}
```

**특징:**
- 각 멤버별로 다른 `unreadCount` 계산
- 읽은 사용자는 `unreadCount = 0`
- 다른 사용자는 자신의 `lastReadMessageId` 기준으로 계산

## 🎯 주요 특징

### 1. 원자적 업데이트

**문제:** 여러 스레드가 동시에 `markAsRead`를 호출하면 Lost Update 문제 발생 가능

**해결:**
```sql
UPDATE chat_room_member
SET last_read_message_id = ?,
    last_read_at = ?
WHERE chat_room_id = ?
  AND user_id = ?
  AND (last_read_message_id IS NULL OR last_read_message_id < ?)
```

- 기존 메시지 ID보다 큰 경우에만 업데이트
- 데이터베이스 레벨에서 원자적 처리

### 2. 메시지 ID 기반 계산

**카톡/라인 스타일:**
- `unreadCount`는 "내가 아직 읽지 않은 메시지 수"
- `lastReadMessageId` 기준으로 결정적으로 계산
- 시간 기반이 아닌 메시지 ID 기반

**계산 방식:**
```java
// 읽지 않은 메시지 수 = lastReadMessageId 이후의 메시지 수
long unreadCount = messageRepository.countUnreadMessagesByLastReadMessageId(
    chatRoomId,
    userId,
    lastReadMessageId
);
```

### 3. 실시간 동기화

**하이브리드 방식:**
- **요청:** REST API (`POST /api/v1/chat/rooms/{roomId}/read`)
- **응답:** WebSocket 이벤트 (업데이트된 메시지, 채팅 목록 등)

**장점:**
- REST API로 안정적인 요청 처리
- WebSocket으로 실시간 업데이트 전달
- 서버가 단일 소스 (Single Source of Truth)

## 📱 클라이언트 구현 가이드

### 1. 읽음 처리 호출 시점

**권장 시점:**
1. 채팅방 화면이 포그라운드로 전환될 때
2. 새 메시지를 받았을 때 (채팅방이 열려있는 경우)
3. 사용자가 명시적으로 읽음 처리 버튼을 누를 때

**주의사항:**
- 너무 빈번한 호출 방지 (디바운싱/스로틀링 고려)
- 네트워크 오류 시 재시도 로직 필요

### 2. WebSocket 이벤트 처리

**메시지 업데이트:**
```dart
// 서버가 보내준 메시지의 unreadCount를 그대로 사용
if (existingMessage.id == receivedMessage.id) {
  // 기존 메시지 업데이트
  updateMessage(receivedMessage);
}
```

**채팅 목록 업데이트:**
```dart
// 서버가 보내준 unreadCount로 채팅 목록 업데이트
chatList.updateUnreadCount(roomId, event.unreadCount);
```

### 3. 상태 관리

**권장 방식:**
- 서버가 보내준 값을 최종 소스로 사용
- 낙관적 업데이트는 선택사항 (서버 값이 우선)
- 로컬 상태와 서버 상태 불일치 시 서버 값으로 덮어쓰기

## 🔍 문제 해결

### 문제 1: 읽음 처리가 반영되지 않음

**원인:**
- WebSocket 연결이 끊어짐
- 이벤트 구독이 안 됨
- 서버 이벤트를 받지 못함

**해결:**
1. WebSocket 연결 상태 확인
2. 채팅방 토픽 구독 확인 (`/topic/chat/room/{roomId}`)
3. 사용자 채널 구독 확인 (`/topic/user/{userId}/chat-list`)
4. 서버 로그 확인

### 문제 2: unreadCount가 정확하지 않음

**원인:**
- 클라이언트가 서버 값을 무시하고 로컬 계산
- 이벤트를 받지 못함
- 동시성 문제

**해결:**
- 서버가 보내준 값을 항상 우선 사용
- 이벤트 수신 로직 확인
- 서버 로그로 실제 계산 값 확인

### 문제 3: 읽음 처리가 너무 느림

**원인:**
- 네트워크 지연
- 서버 처리 지연
- 이벤트 브로드캐스트 지연

**해결:**
- 낙관적 업데이트 고려 (서버 값으로 나중에 덮어쓰기)
- 서버 성능 모니터링
- Redis Pub/Sub 지연 확인

## 📊 성능 고려사항

### 1. 메시지 재전송 최적화

**현재 구현:**
- 읽음 처리 후 최근 50개 메시지 재전송
- 각 메시지의 `unreadCount` 재계산

**최적화 방안:**
- 변경된 메시지만 전송 (필요 시)
- 배치 처리 고려
- 클라이언트 캐싱 활용

### 2. 동시성 처리

**현재 구현:**
- 원자적 UPDATE 쿼리
- 데이터베이스 레벨 동시성 제어

**추가 고려사항:**
- 분산 환경에서의 동시성 (여러 서버 인스턴스)
- Redis 분산 락 (필요 시)

## 🔗 관련 문서

- [기능 점검 요약](./FEATURE_CHECK_SUMMARY.md) — 읽기 기능 검증 결과
- [서버팀 확인 사항 (markAsRead Q&A)](./server-team-verification-markAsRead.md)
- [문서 목차](./README.md)

## 📝 변경 이력

- 2026-01-26: 초기 문서 작성
