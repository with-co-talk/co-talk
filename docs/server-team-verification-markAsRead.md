# 서버팀 확인 사항: markAsRead 후 WebSocket 메시지 전송

> **역할**: 읽기 기능 검증 시점의 Q&A·확인 사항. 동작 스펙·가이드는 [READ_FEATURE.md](./READ_FEATURE.md) 참고.

## 📋 확인 요청 사항

### 1. markAsRead 호출 후 서버가 업데이트된 메시지를 WebSocket으로 보내주는지

**현재 서버 동작 (코드 분석 결과):**
- ❌ **읽음 처리 후 업데이트된 메시지 객체를 `/topic/chat/room/{roomId}`로 전송하지 않음**
- ✅ 읽음 처리 후 `RoomReadEvent` (이벤트 타입: "READ")만 `/topic/chat/room/{roomId}`로 전송
- ✅ `/topic/user/{userId}/chat-list`로 `ChatListUpdateEvent` 전송 (unreadCount 포함)

**코드 위치:**
- `MarkAsReadService.publishRoomReadEvent()`: `ChatRoomEventMessage` (이벤트)만 전송
- `MarkAsReadService.publishChatListUpdate()`: 채팅 목록 업데이트 이벤트만 전송

**질문:**
- 읽음 처리 후 업데이트된 `unreadCount`가 포함된 메시지 객체를 `/topic/chat/room/{roomId}`로 전송해야 하는가?
- 아니면 현재처럼 `RoomReadEvent`만 전송하는 것이 의도된 동작인가?

---

### 2. 읽음 처리 후 변경된 unreadCount가 포함된 메시지를 `/topic/chat/room/{roomId}`로 전송하는지

**현재 서버 동작:**
- ❌ **읽음 처리 후 메시지 객체를 전송하지 않음**
- ✅ `RoomReadEvent`만 전송 (메시지 객체가 아님)
- ✅ `/topic/user/{userId}/chat-list`로는 `ChatListUpdateEvent`에 unreadCount 포함하여 전송

**질문:**
- 읽음 처리 후 각 메시지의 `unreadCount`가 업데이트된 메시지 객체를 `/topic/chat/room/{roomId}`로 브로드캐스트해야 하는가?
- 예: 메시지 A의 unreadCount가 3 → 2로 변경되면, 업데이트된 메시지 A를 다시 전송해야 하는가?

---

### 3. 메시지 전송 시 항상 최신 unreadCount를 포함하는지

**현재 서버 동작 (새 메시지 전송 시):**
- ✅ `ChatWebSocketController.publishToRedis()`에서 새 메시지 전송 시 unreadCount 계산
- ✅ 계산 방식: `totalMembers - 1` (발신자 제외)
- ⚠️ **presence를 고려하지 않음** (코드 주석 참조)

**코드 위치:**
```java
// ChatWebSocketController.publishToRedis()
int totalMembers = members.size();
int unreadCount = Math.max(0, totalMembers - 1); // 발신자 제외
```

**질문:**
- 새 메시지 전송 시 unreadCount 계산이 정확한가?
- presence를 고려해야 하는가? (현재는 고려하지 않음)

---

### 4. 새 메시지 전송 시 수신자별 최신 unreadCount를 포함하는지

**현재 서버 동작:**
- ✅ `/topic/chat/room/{roomId}`로는 **동일한 unreadCount** 전송 (발신자 제외한 전체 인원 수)
- ✅ `/topic/user/{userId}/chat-list`로는 **수신자별로 다른 unreadCount** 계산하여 전송

**코드 위치:**
- `ChatWebSocketController.publishToRedis()`: 방 토픽으로는 동일한 unreadCount
- `ChatWebSocketController.publishChatListUpdate()`: 사용자별로 다른 unreadCount 계산

**질문:**
- `/topic/chat/room/{roomId}`로 전송하는 메시지의 unreadCount는 방 전체 기준인가?
- 아니면 수신자별로 다른 unreadCount를 포함한 메시지를 각각 전송해야 하는가?

---

### 5. 읽음 처리 후 어떤 이벤트를 보내는지

**현재 서버 동작:**

1. **`/topic/chat/room/{roomId}`로 전송:**
   - `ChatRoomEventMessage` (RoomReadEvent)
   - 이벤트 타입: "READ"
   - 포함 정보: `chatRoomId`, `userId`, `lastReadMessageId`, `lastReadAt`

2. **`/topic/user/{userId}/read-receipt`로 전송:**
   - `ReadReceiptEvent`
   - 모든 채팅방 멤버에게 전송 (읽은 사용자 본인 포함)

3. **`/topic/user/{userId}/chat-list`로 전송:**
   - `ChatListUpdateEvent`
   - 이벤트 타입: "READ"
   - 포함 정보: `chatRoomId`, `lastMessage`, `unreadCount` (수신자별로 계산)

**코드 위치:**
- `MarkAsReadService.publishRoomReadEvent()`
- `MarkAsReadService.publishReadReceiptToMembers()`
- `MarkAsReadService.publishChatListUpdate()`

**질문:**
- 현재 전송하는 이벤트들이 클라이언트 요구사항을 만족하는가?
- 추가로 전송해야 하는 이벤트가 있는가?

---

### 6. readEvents만 보내는지, 아니면 업데이트된 메시지도 함께 보내는지

**현재 서버 동작:**
- ✅ **readEvents만 전송** (RoomReadEvent, ReadReceiptEvent, ChatListUpdateEvent)
- ❌ **업데이트된 메시지 객체는 전송하지 않음**

**질문:**
- 읽음 처리 후 업데이트된 unreadCount가 포함된 메시지 객체를 `/topic/chat/room/{roomId}`로 전송해야 하는가?
- 예: 메시지 A의 unreadCount가 변경되면, 업데이트된 메시지 A를 다시 브로드캐스트해야 하는가?

---

## 🔍 현재 클라이언트 동작

### 클라이언트 기대 동작:
1. 서버가 보내주는 메시지의 `unreadCount`를 그대로 사용
2. 기존 메시지가 있으면 서버가 보내준 값으로 업데이트
3. `readEvents`는 사용하지 않음 (현재)

### 클라이언트 요구사항:
- **읽음 처리 후 서버가 업데이트된 메시지를 보내주면 클라이언트는 그 값을 그대로 사용**
- **서버가 보내주지 않으면 서버 측 수정이 필요**

---

## 📊 현재 서버 구현 요약

### 새 메시지 전송 시 (`ChatWebSocketController`):
```
/topic/chat/room/{roomId}
  └─ WebSocketChatMessage (unreadCount 포함, 방 전체 기준)

/topic/user/{userId}/chat-list
  └─ ChatListUpdateEvent (unreadCount 포함, 수신자별 계산)
```

### 읽음 처리 후 (`MarkAsReadService`):
```
/topic/chat/room/{roomId}
  └─ ChatRoomEventMessage (RoomReadEvent) ⚠️ 메시지 객체가 아님

/topic/user/{userId}/read-receipt
  └─ ReadReceiptEvent

/topic/user/{userId}/chat-list
  └─ ChatListUpdateEvent (unreadCount 포함, 수신자별 계산)
```

---

## ❓ 확인이 필요한 핵심 질문

1. **읽음 처리 후 업데이트된 메시지 객체를 `/topic/chat/room/{roomId}`로 전송해야 하는가?**
   - 현재는 `RoomReadEvent`만 전송
   - 클라이언트는 업데이트된 메시지 객체를 기대

2. **읽음 처리 후 각 메시지의 unreadCount가 변경되면, 해당 메시지들을 다시 브로드캐스트해야 하는가?**
   - 예: 메시지 A, B, C가 있고, 읽음 처리로 A, B의 unreadCount가 변경되면
   - A, B를 업데이트된 unreadCount와 함께 다시 전송해야 하는가?

3. **새 메시지 전송 시 `/topic/chat/room/{roomId}`로 전송하는 unreadCount는 방 전체 기준인가, 수신자별인가?**
   - 현재는 방 전체 기준 (발신자 제외)
   - 수신자별로 다른 unreadCount를 포함한 메시지를 각각 전송해야 하는가?

---

## 📝 참고 코드 위치

- `MarkAsReadService.java`: 읽음 처리 로직
- `ChatWebSocketController.java`: 메시지 전송 로직
- `RedisChatMessageSubscriber.java`: Redis → WebSocket 브로드캐스트
- `RedisChatMessageBroker.java`: Redis Pub/Sub 발행

---

## ✅ 확인 후 필요한 작업

서버팀 확인 결과에 따라:

1. **읽음 처리 후 메시지 재전송이 필요한 경우:**
   - `MarkAsReadService`에 메시지 재전송 로직 추가
   - 각 메시지의 unreadCount 재계산 및 브로드캐스트

2. **현재 동작이 의도된 경우:**
   - 클라이언트가 `readEvents`를 처리하도록 수정
   - 또는 클라이언트에서 unreadCount를 직접 계산

---

**작성일:** 2026-01-26  
**작성자:** AI Assistant  
**목적:** 서버팀과의 동작 확인 및 클라이언트-서버 간 인터페이스 정합성 확보
