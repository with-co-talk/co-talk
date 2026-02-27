# WebSocket 브로드캐스트가 실패해도 메시지는 살려야 한다

> Co-Talk 개발기 7편 — [PR #62](https://github.com/with-co-talk/co-talk/pull/62) 실패 격리 설계

---

## 문제의 발단

채팅 앱에서 메시지를 보내면 내부적으로 이런 일이 순서대로 일어난다.

```
1. DB에 메시지 저장
2. Redis Pub/Sub으로 브로드캐스트 (WebSocket 전달)
3. 채팅 목록 업데이트 이벤트 발행
4. 오프라인 사용자에게 FCM 푸시 알림
```

언뜻 보면 자연스럽다. 문제는 이걸 **하나의 트랜잭션**으로 묶었을 때 생긴다.

Redis 커넥션이 잠깐 끊기거나, Pub/Sub 채널에 타임아웃이 나면?
트랜잭션이 롤백된다. DB에 저장됐던 메시지도 같이 사라진다.

사용자 입장에서는 "전송" 버튼을 눌렀고, 잠깐 로딩이 돌더니 아무 일도 없었던 것처럼 보인다.
**메시지가 증발한 거다.**

---

## 왜 이게 위험한가

단순히 "재전송하면 되지 않냐"는 생각이 들 수 있다. 하지만 실제로는 더 나쁜 시나리오가 있다.

```
시나리오: Redis 브로드캐스트 타임아웃 (500ms 초과)

송신자: 전송 성공 메시지 봄 (UI 낙관적 업데이트)
수신자: 아무것도 받지 못함
DB: 롤백 완료 → 메시지 없음
```

송신자는 메시지를 보냈다고 생각한다.
수신자는 아무것도 받지 못했다.
DB에는 기록이 없다.

이걸 "일시적인 네트워크 문제"라고 치부할 수 없다. **데이터 유실이다.**

더 나아가서, 만약 이 채팅이 업무 채팅이라면? 중요한 결정이 담긴 메시지가 사라진다.
사용자는 상대방이 읽었는지 안 읽었는지도 모른 채 일을 진행하게 된다.

---

## 실패 흐름 다이어그램

<!-- IMAGE: 실패 복구 흐름 다이어그램 — 아래 ASCII 다이어그램을 Mermaid로 렌더링하거나 draw.io로 그린 "수정 전/수정 후" 비교 이미지 -->

### 수정 전 — 하나의 흐름, 하나의 실패 지점

```
클라이언트 → WebSocket → @Transactional 시작
                              │
                         DB 저장 ✓
                              │
                         Redis 브로드캐스트 ✗ (타임아웃)
                              │
                         예외 전파
                              │
                         @Transactional 롤백
                              │
                         DB 저장도 취소 ← 여기가 문제
```

### 수정 후 — 저장과 전달을 분리

```
클라이언트 → WebSocket
                │
         [트랜잭션 범위]
         DB 저장 ✓
         lastReadMessageId 업데이트 ✓
         트랜잭션 커밋 ← 여기서 끝
                │
         [트랜잭션 밖]
         try { 브로드캐스트 } catch { 로그만 }  ← 실패해도 OK
         try { 채팅목록 업데이트 } catch { 로그만 }  ← 실패해도 OK
         FCM 푸시 알림  ← 트랜잭션 밖에서 별도 실행
```

---

## 코드로 보기

<!-- IMAGE: 메시지 전달 실패 시 애플리케이션 로그 — `[WS] Failed to broadcast message: messageId=..., roomId=...` 형태의 ERROR 로그 캡처 (Grafana Loki 또는 docker logs 화면) -->

### ChatWebSocketController — 실패 격리 적용

```java
@MessageMapping("/chat/message")
public void sendMessage(@Payload ChatMessageRequest request, StompHeaderAccessor headerAccessor) {
    Long authenticatedUserId = extractUserId(headerAccessor);

    // 유효성 검증 (생략)

    // 메시지 저장 + 컨텍스트 조회 (sender, members 한 번만 조회)
    SendMessageUseCase.SendResult result = sendMessageUseCase.sendMessageWithContext(
            request.roomId(), authenticatedUserId, request.content());

    // 브로드캐스트 — 실패해도 메시지는 저장됨
    try {
        broadcastChatMessageUseCase.broadcastMessage(
                result.message(), result.senderNickname(), result.senderAvatarUrl(), result.members());
    } catch (Exception e) {
        log.error("[WS] Failed to broadcast message: messageId={}, roomId={}",
                result.message().getId(), request.roomId(), e);
    }

    // 채팅 목록 업데이트 — 실패해도 메시지는 저장됨
    try {
        publishChatListUpdateUseCase.publishChatListUpdate(
                result.message(), result.members(), result.senderNickname());
    } catch (Exception e) {
        log.error("[WS] Failed to publish chat list update: messageId={}, roomId={}",
                result.message().getId(), request.roomId(), e);
    }
}
```

`sendMessageUseCase.sendMessageWithContext()`가 성공하면 메시지는 DB에 커밋된다.
그 이후의 브로드캐스트와 채팅목록 업데이트는 각각 독립된 try-catch로 감싼다.
둘 다 실패해도 메시지는 살아있다.

이모지 반응도 같은 패턴을 적용했다.

```java
@MessageMapping("/chat/reaction/add")
public void addReaction(@Payload AddReactionRequest request, StompHeaderAccessor headerAccessor) {
    // 반응 저장
    ReactionResult result = addMessageReactionUseCase.addReactionWithContext(
            request.messageId(), authenticatedUserId, request.emoji());

    // 브로드캐스트 실패해도 반응은 저장됨 - 다음 조회 시 표시
    try {
        broadcastReactionEventUseCase.broadcastReactionEvent(
                result.reaction(), result.chatRoomId(), "ADDED");
    } catch (Exception e) {
        log.error("[WS] Failed to broadcast reaction add: messageId={}, roomId={}, emoji={}",
                request.messageId(), result.chatRoomId(), request.emoji(), e);
    }
}
```

---

### SendMessageService — TransactionTemplate으로 명시적 경계

`@Transactional` 어노테이션 대신 `TransactionTemplate`을 직접 사용한 이유가 있다.

```java
@Service
@RequiredArgsConstructor
public class SendMessageService implements SendMessageUseCase {

    private final TransactionTemplate transactionTemplate;
    // ...

    private SendResult doSendMessage(Long chatRoomId, Long senderId,
                                     Message message, String notificationContent) {
        // DB 작업만 트랜잭션으로 래핑
        SendResult result = transactionTemplate.execute(status -> {
            List<ChatRoomMember> members = chatRoomMemberRepository.findByChatRoomId(chatRoomId);
            User sender = userRepository.findById(senderId).orElse(null);

            // 멤버십 검증
            boolean isMember = members.stream()
                    .anyMatch(m -> m.getUserId().equals(senderId));
            if (!isMember) {
                throw new ChatRoomAccessDeniedException(chatRoomId, senderId);
            }

            // 메시지 저장
            Message savedMessage = messageRepository.save(message);

            // 발신자 lastReadMessageId 업데이트
            chatRoomMemberRepository.updateLastReadMessageIdIfNewer(
                    chatRoomId, senderId, timeProvider.now(), savedMessage.getId());

            return new SendResult(savedMessage, senderNickname, senderAvatarUrl, members);
        });

        // 트랜잭션 밖: 푸시 알림 (Redis 호출 — DB 커넥션 미점유)
        sendPushNotificationsToOtherMembers(
                chatRoomId, senderId, notificationContent,
                result.senderNickname(), result.senderAvatarUrl(), result.members());

        return result;
    }
}
```

`@Transactional`을 메서드에 붙이면 메서드 전체가 트랜잭션이다.
FCM 푸시 알림이나 Redis 호출도 트랜잭션 안에서 실행되고, 그 시간만큼 DB 커넥션이 붙잡힌다.

`TransactionTemplate`을 쓰면 람다 블록 안만 트랜잭션이다.
DB 커넥션을 최소한의 시간만 점유하고, Redis/FCM 호출은 커밋 이후에 독립적으로 실행된다.

---

## 이중 안전장치

정리하면 이렇다.

| 계층 | 역할 | 실패 시 동작 |
|------|------|------------|
| `TransactionTemplate` | DB 저장 트랜잭션 경계 | 저장 실패 시만 롤백 |
| Controller try-catch | 브로드캐스트 격리 | 실패 로그 + 계속 진행 |
| Controller try-catch | 채팅목록 업데이트 격리 | 실패 로그 + 계속 진행 |

`SendMessageService`에서 `TransactionTemplate`으로 DB 저장 범위를 명확히 끊고,
`ChatWebSocketController`에서 각 브로드캐스트 단계를 try-catch로 격리한다.

서비스 계층과 컨트롤러 계층 양쪽에서 방어한다.

---

## 브로드캐스트 실패 시 복구 경로

브로드캐스트가 실패하면 수신자는 실시간으로 메시지를 받지 못한다.
하지만 데이터는 DB에 있다.

수신자가 채팅방을 나갔다가 다시 들어오면, REST API로 최신 메시지를 조회한다.
`/api/v1/chatrooms/{id}/messages` 엔드포인트가 DB에서 메시지를 가져온다.

```
브로드캐스트 실패 시나리오:

송신자 전송 → DB 저장 ✓ → 브로드캐스트 ✗
                                   │
                              수신자: 실시간 수신 ✗
                                   │
                              수신자: 채팅방 재진입
                                   │
                              REST API 조회 → DB에서 메시지 반환 ✓
```

실시간 전달은 실패했지만 데이터 정합성은 유지된다.
"아까 메시지 보냈는데 왜 없냐"는 상황은 없다.

---

## 코드가 지저분해지는 트레이드오프

솔직히 인정한다. try-catch가 여러 개 들어가면 코드가 길어진다.

```java
// 이게 더 깔끔해 보인다
broadcastChatMessageUseCase.broadcastMessage(...);
publishChatListUpdateUseCase.publishChatListUpdate(...);

// 이게 현실이다
try {
    broadcastChatMessageUseCase.broadcastMessage(...);
} catch (Exception e) {
    log.error("[WS] Failed to broadcast message: messageId={}, roomId={}", ...);
}

try {
    publishChatListUpdateUseCase.publishChatListUpdate(...);
} catch (Exception e) {
    log.error("[WS] Failed to publish chat list update: messageId={}, roomId={}", ...);
}
```

첫 번째 방식은 간결하지만, 브로드캐스트 실패 시 예외가 전파된다.
WebSocket 핸들러에서 예외가 발생하면 Spring STOMP는 해당 세션을 끊거나 에러 프레임을 보낸다.
최악의 경우 DB 커밋 이후에 예외가 터지면서 일관성 없는 상태로 남는다.

두 번째 방식은 장황하지만, 각 단계의 실패를 독립적으로 처리한다.
로그에 `messageId`와 `roomId`를 남기기 때문에 나중에 어떤 메시지가 브로드캐스트에 실패했는지 추적 가능하다.

코드의 간결함보다 데이터 안전이 먼저다.

---

## 배운 것

**1. "메시지 저장"과 "메시지 전달"은 분리되어야 한다.**

저장은 핵심이고 전달은 최선(best-effort)이다.
실시간 전달이 실패해도 저장은 반드시 성공해야 한다.

**2. 실시간 기능의 실패가 핵심 데이터를 날려서는 안 된다.**

Redis, WebSocket, FCM은 모두 외부 시스템이다.
네트워크 문제, 잠깐의 다운타임, 타임아웃은 언제든 발생할 수 있다.
이것들이 DB 저장 트랜잭션에 영향을 줘서는 안 된다.

**3. `TransactionTemplate`은 범위를 명시적으로 만들어준다.**

`@Transactional`은 편리하지만 범위가 암묵적이다.
어디서 커밋이 일어나는지, 어디까지 롤백되는지 코드만 보고는 바로 파악하기 어렵다.
`TransactionTemplate`은 람다로 트랜잭션 경계를 눈에 보이게 표시한다.

**4. 로그에 식별자를 남겨라.**

```java
log.error("[WS] Failed to broadcast message: messageId={}, roomId={}",
          result.message().getId(), request.roomId(), e);
```

브로드캐스트가 실패했을 때 어떤 메시지인지 알아야 나중에 대응할 수 있다.
단순히 "브로드캐스트 실패"가 아니라 `messageId`와 `roomId`를 남겨야 한다.

---

## 마치며

채팅 앱에서 메시지 유실은 신뢰도 문제다.
한 번 메시지가 사라지는 경험을 하면 사용자는 그 앱을 다시 믿기 어렵다.

Redis 브로드캐스트 실패가 메시지를 날리던 버그를 수정하면서,
"저장"과 "전달"을 분리하는 것이 단순한 성능 최적화가 아니라 **설계 원칙**임을 다시 확인했다.

코드가 조금 길어지더라도, 실패가 격리되는 구조가 맞다.

---

*다음 편: [사이드 프로젝트에 모니터링 스택을 붙인 이유](blog-09-monitoring-stack.md)*
