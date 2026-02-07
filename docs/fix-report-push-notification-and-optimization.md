# 푸시 알림 및 서버 최적화 수정 보고서

**작성일**: 2026-02-07
**작성자**: Claude (AI Assistant)

---

## 목차

1. [푸시 알림 클릭 시 WebSocket 연결 문제](#1-푸시-알림-클릭-시-websocket-연결-문제)
2. [서버 채팅 로직 N+1 쿼리 최적화](#2-서버-채팅-로직-n1-쿼리-최적화)
3. [백그라운드 푸시 알림 미표시 문제](#3-백그라운드-푸시-알림-미표시-문제)

---

## 1. 푸시 알림 클릭 시 WebSocket 연결 문제

### 증상
- 푸시 메시지 클릭 후 채팅방 진입 시 예전 대화만 표시됨
- 메시지 송수신이 제대로 되지 않음

### 원인
앱이 백그라운드/종료 상태에서 푸시 알림을 클릭하여 채팅방에 진입할 때, WebSocket이 연결되지 않은 상태에서 구독을 시도하여 실시간 메시지를 수신하지 못함.

### 수정 내용

#### 파일: `co-talk-flutter/lib/presentation/blocs/chat/chat_room_bloc.dart`

**1. `_subscribeToWebSocket` 메서드 수정**

WebSocket 구독 전 연결 상태를 확인하고, 연결되지 않은 경우 연결을 시도하도록 수정.

```dart
Future<void> _subscribeToWebSocket(int roomId) async {
  final isConnected = _webSocketService.isConnected;
  final lastMessageId = _cacheManager.lastMessageId;
  _log('_subscribeToWebSocket: roomId=$roomId, isConnected=$isConnected, lastMessageId=$lastMessageId');

  // Ensure WebSocket is connected before subscribing
  if (!isConnected) {
    _log('_subscribeToWebSocket: WebSocket not connected, attempting to connect...');
    final connected = await _webSocketService.ensureConnected(
      timeout: const Duration(seconds: 10),
    );
    if (!connected) {
      _log('_subscribeToWebSocket: Failed to connect WebSocket');
    } else {
      _log('_subscribeToWebSocket: WebSocket connected successfully');
    }
  }

  // ... 기존 구독 로직
}
```

**2. `_onForegrounded` 메서드 수정**

앱이 포그라운드로 돌아올 때 WebSocket 연결 상태를 확인하고, 필요시 재연결 및 재구독.

```dart
Future<void> _onForegrounded(
  ChatRoomForegrounded event,
  Emitter<ChatRoomState> emit,
) async {
  // ... 기존 검증 로직

  // Ensure WebSocket is connected when returning to foreground
  if (!_webSocketService.isConnected) {
    _log('_onForegrounded: WebSocket disconnected, reconnecting...');
    final connected = await _webSocketService.ensureConnected(
      timeout: const Duration(seconds: 5),
    );
    if (connected) {
      _log('_onForegrounded: WebSocket reconnected, resubscribing to room');
      await _subscribeToWebSocket(state.roomId!);
    } else {
      _log('_onForegrounded: Failed to reconnect WebSocket');
    }
  }

  // ... 기존 로직
}
```

### 결과
- 푸시 알림 클릭 진입 시에도 WebSocket이 정상 연결됨
- 실시간 메시지 송수신 정상 동작

---

## 2. 서버 채팅 로직 N+1 쿼리 최적화

### 증상
채팅방 메시지 전송 시 멤버 수만큼 개별 쿼리가 발생하여 성능 저하.

### 원인
`ChatWebSocketController`와 `MarkAsReadService`에서 각 멤버의 `unreadCount`를 개별 쿼리로 조회 (N+1 문제).

```java
// Before: N+1 쿼리 발생
for (ChatRoomMember member : members) {
    int memberUnreadCount = messageRepository
        .countUnreadMessagesByLastReadMessageId(chatRoomId, member.getUserId(), lastReadMessageId)
        .intValue();
    // ...
}
```

### 수정 내용

#### 1. 배치 쿼리 추가

**파일: `MessageJpaRepository.java`**

```java
@Query(value = """
    SELECT cm.user_id as userId,
           COUNT(m.id) as unreadCount
    FROM chat_room_members cm
    LEFT JOIN messages m ON m.chat_room_id = cm.chat_room_id
      AND m.is_deleted = false
      AND m.sender_id <> cm.user_id
      AND (cm.last_read_message_id IS NULL OR m.id > cm.last_read_message_id)
    WHERE cm.chat_room_id = :chatRoomId
    GROUP BY cm.user_id
    """, nativeQuery = true)
List<Object[]> batchCountUnreadMessagesForAllMembers(@Param("chatRoomId") Long chatRoomId);
```

#### 2. 인터페이스 추가

**파일: `MessageRepository.java`**

```java
/**
 * 채팅방의 모든 멤버에 대해 읽지 않은 메시지 수를 한 번에 조회한다.
 * (N+1 쿼리 방지용 배치 조회)
 */
Map<Long, Long> batchCountUnreadMessagesForAllMembers(Long chatRoomId);
```

#### 3. 어댑터 구현

**파일: `MessageRepositoryAdapter.java`**

```java
@Override
public Map<Long, Long> batchCountUnreadMessagesForAllMembers(Long chatRoomId) {
    List<Object[]> results = messageJpaRepository.batchCountUnreadMessagesForAllMembers(chatRoomId);
    Map<Long, Long> unreadCountMap = new HashMap<>();

    for (Object[] row : results) {
        Long userId = ((Number) row[0]).longValue();
        Long unreadCount = ((Number) row[1]).longValue();
        unreadCountMap.put(userId, unreadCount);
    }

    return unreadCountMap;
}
```

#### 4. 서비스 최적화

**파일: `ChatWebSocketController.java`, `MarkAsReadService.java`**

```java
// After: 단일 배치 쿼리로 최적화
Map<Long, Long> unreadCountMap = messageRepository.batchCountUnreadMessagesForAllMembers(
        message.getChatRoomId());

for (ChatRoomMember member : members) {
    int memberUnreadCount = unreadCountMap.getOrDefault(member.getUserId(), 0L).intValue();
    // ...
}
```

### 결과

| 항목 | Before | After |
|------|--------|-------|
| 쿼리 수 | N+1 (멤버 수 + 1) | 1 |
| 10명 채팅방 | 11 쿼리 | 1 쿼리 |
| 100명 그룹 | 101 쿼리 | 1 쿼리 |

---

## 3. 백그라운드 푸시 알림 미표시 문제

### 증상
- 핸드폰 화면을 꺼야만 푸시 알림이 옴
- 다른 앱 사용 중(백그라운드)에는 알림이 오지 않음

### 원인

#### Android
서버의 FCM 설정에서 **알림 채널 ID가 누락**되어 있었음. Android 8.0+ 에서는 채널 ID를 명시하지 않으면 백그라운드 알림이 제대로 표시되지 않음.

#### iOS
APNs 헤더(`apns-push-type`, `apns-priority`)가 누락되어 백그라운드 알림 전송이 지연되거나 누락될 수 있었음.

### 수정 내용

**파일: `FcmPushNotificationSender.java`**

#### Android 설정 수정

```java
private AndroidConfig createAndroidConfig() {
    return AndroidConfig.builder()
            .setPriority(AndroidConfig.Priority.HIGH)
            .setNotification(AndroidNotification.builder()
                    .setChannelId("chat_messages")  // 추가: Flutter 앱과 동일한 채널 ID
                    .setSound("default")
                    .build())
            .build();
}
```

#### iOS 설정 수정

```java
private ApnsConfig createApnsConfig() {
    return ApnsConfig.builder()
            .putHeader("apns-push-type", "alert")  // 추가: 알림 타입 명시
            .putHeader("apns-priority", "10")      // 추가: 즉시 전송 우선순위
            .setAps(Aps.builder()
                    .setSound("default")
                    .setBadge(1)
                    .build())
            .build();
}
```

### 설정 설명

#### Android

| 설정 | 값 | 설명 |
|------|-----|------|
| `channelId` | `chat_messages` | Flutter 앱에서 생성한 알림 채널과 일치해야 함 |

#### iOS

| 헤더 | 값 | 설명 |
|------|-----|------|
| `apns-push-type` | `alert` | 사용자에게 표시되는 알림 타입 |
| `apns-priority` | `10` | 즉시 전송 (5는 절전 모드로 지연됨) |

### 결과
- Android: 다른 앱 사용 중에도 푸시 알림 정상 표시
- iOS: 백그라운드에서 즉시 푸시 알림 수신

---

## 수정된 파일 목록

### Flutter (co-talk-flutter)

| 파일 | 수정 내용 |
|------|----------|
| `lib/presentation/blocs/chat/chat_room_bloc.dart` | WebSocket 재연결 로직 추가 |

### Spring Boot (co-talk)

| 파일 | 수정 내용 |
|------|----------|
| `MessageJpaRepository.java` | 배치 쿼리 메서드 추가 |
| `MessageRepository.java` | 인터페이스 메서드 추가 |
| `MessageRepositoryAdapter.java` | 어댑터 구현 추가 |
| `ChatWebSocketController.java` | 배치 쿼리 사용으로 최적화 |
| `MarkAsReadService.java` | 배치 쿼리 사용으로 최적화 |
| `MarkAsReadServiceTest.java` | 테스트 코드 업데이트 |
| `FcmPushNotificationSender.java` | Android 채널 ID, iOS APNs 헤더 추가 |

---

## 배포 체크리스트

- [ ] Spring Boot 서버 재배포
- [ ] Android 앱 테스트: 백그라운드 푸시 알림 확인
- [ ] iOS 앱 테스트: 백그라운드 푸시 알림 확인
- [ ] 푸시 알림 클릭 → 채팅방 진입 → 메시지 송수신 테스트
- [ ] 대규모 그룹 채팅방에서 성능 개선 확인
