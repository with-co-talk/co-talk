# Co-Talk 개발기 4편: DTO에서 userId를 제거한 이유 — 보안 리팩토링 ([이슈 #46](https://github.com/with-co-talk/co-talk/issues/46))

> **시리즈 목차**
> - 1편: 헥사고날 아키텍처로 채팅 앱 설계하기
> - 2편: JWT + Spring Security 인증 흐름
> - 3편: WebSocket STOMP와 Redis Pub/Sub으로 실시간 채팅 구현
> - **4편: DTO에서 userId를 제거한 이유 — 보안 리팩토링 (현재)**

---

## 발단: 코드 리뷰 중 발견한 구조적 취약점

초기 개발 단계에서 메시지 전송 API는 이런 식이었다.

```java
// Before — 취약한 구조
public record SendMessageRequest(
        Long senderId,   // 클라이언트가 직접 보내는 값
        Long chatRoomId,
        String content
) {}
```

```java
@PostMapping
public ResponseEntity<SendMessageResponse> sendMessage(
        @RequestBody SendMessageRequest request) {
    // request.senderId()를 그대로 신뢰해서 사용
    Message message = sendMessageUseCase.sendMessage(
            request.senderId(), request.chatRoomId(), request.content());
    return ResponseEntity.status(HttpStatus.CREATED).body(SendMessageResponse.from(message));
}
```

언뜻 보면 문제없어 보인다. JWT 인증을 통과해야 이 API에 접근할 수 있으니까. 하지만 여기에 치명적인 결함이 있다.

**JWT가 "이 사람이 누구인지"를 증명하는데, senderId는 클라이언트가 임의로 설정한다.**

인증(Authentication)과 실제 동작에 사용되는 신원(Identity)이 분리돼 있는 것이다.

---

<!-- IMAGE: curl로 IDOR 공격 시도 예시 터미널 스크린샷 — 위 curl 명령어(정상 요청과 senderId를 변조한 위장 요청)를 실제 터미널에서 실행한 화면. 두 요청이 나란히 보이도록 캡처 -->

## IDOR: 위장 공격이 가능한 구조

이 패턴은 **IDOR(Insecure Direct Object Reference)** 취약점에 해당한다. OWASP Top 10에도 포함된, API 보안에서 가장 흔하게 발견되는 취약점 중 하나다.

공격 시나리오는 간단하다:

1. 공격자 A가 정상적으로 로그인해서 유효한 JWT 토큰을 발급받는다.
2. A가 메시지 전송 요청 시 `senderId`를 B의 ID로 바꿔서 보낸다.
3. 서버는 JWT 검증만 통과하면 그 요청을 신뢰한다.
4. B의 이름으로 메시지가 저장되고, B의 이름으로 모든 참여자에게 브로드캐스트된다.

```bash
# 정상 요청
curl -X POST /api/v1/chat/messages \
  -H "Authorization: Bearer <A의 JWT>" \
  -d '{"senderId": 1001, "chatRoomId": 42, "content": "안녕"}'

# 위장 공격
curl -X POST /api/v1/chat/messages \
  -H "Authorization: Bearer <A의 JWT>" \
  -d '{"senderId": 9999, "chatRoomId": 42, "content": "나는 B다"}'
  #              ^^^^ A의 토큰으로 B인 척
```

인증은 성공, 권한 검증은 없음, 위장 성공. 이게 IDOR다.

채팅 서비스에서 이 취약점이 악용된다면 메시지 위조, 타인 사칭, 사회공학 공격의 토대가 된다.

---

## 해결책: 서버가 신원을 결정한다

핵심 원칙은 하나다.

> **"클라이언트에서 보내는 사용자 ID를 절대 신뢰하지 마라."**

사용자 신원은 서버가 발급한 JWT에서만 추출해야 한다. Spring Security에서는 이를 `@AuthenticationPrincipal`로 깔끔하게 처리할 수 있다.

<!-- IMAGE: @AuthenticationPrincipal 적용 후 401 응답 터미널 스크린샷 — 인증 토큰 없이 또는 senderId를 변조한 curl 요청에 대해 서버가 401 Unauthorized를 반환하는 화면 캡처 -->

### CustomUserPrincipal

먼저 JWT 인증 필터가 토큰을 검증한 뒤 `SecurityContext`에 저장하는 `CustomUserPrincipal`이다:

```java
/**
 * JWT 인증을 위한 커스텀 UserDetails 구현체.
 * SecurityContext에 저장되어 인증된 사용자 정보를 제공한다.
 */
public class CustomUserPrincipal implements UserDetails {

    private final Long userId;
    private final String role;
    private final Collection<? extends GrantedAuthority> authorities;

    public CustomUserPrincipal(Long userId, String role,
                               Collection<? extends GrantedAuthority> authorities) {
        this.userId = userId;
        this.role = role;
        this.authorities = authorities;
    }

    public Long getUserId() {
        return userId;
    }

    // ... UserDetails 구현
}
```

`userId`는 JWT 파싱 단계에서 서버가 직접 채워 넣는다. 클라이언트가 건드릴 수 없다.

### DTO에서 senderId 제거

```java
// After — 안전한 구조
public record SendMessageRequest(
        Long chatRoomId,
        String content
        // senderId 없음. 서버가 JWT에서 추출
) {}
```

DTO 자체가 사용자 ID 필드를 갖지 않으니, 클라이언트가 아무리 요청 본문을 조작해도 다른 사람으로 위장할 수단이 없다.

### 컨트롤러에서 principal 주입

```java
@PostMapping
public ResponseEntity<SendMessageResponse> sendMessage(
        @AuthenticationPrincipal CustomUserPrincipal principal,  // JWT에서 추출된 신뢰 가능한 정보
        @Valid @RequestBody SendMessageRequest request) {

    Message message = sendMessageUseCase.sendTextMessageAndBroadcast(
            request.chatRoomId(),
            principal.getUserId(),   // 클라이언트 요청값이 아닌 JWT 기반 값
            request.content());

    return ResponseEntity.status(HttpStatus.CREATED)
            .body(SendMessageResponse.from(message));
}
```

`principal.getUserId()`는 JWT 서명을 통해 서버가 보장하는 값이다. 클라이언트 요청 본문과는 완전히 독립적이다.

실제 `ChatMessageController`의 모든 엔드포인트가 이 패턴을 따른다:

```java
// 메시지 수정: 본인 메시지만 수정 가능
@PutMapping("/{messageId}")
public ResponseEntity<UpdateMessageResponse> updateMessage(
        @AuthenticationPrincipal CustomUserPrincipal principal,
        @PathVariable Long messageId,
        @Valid @RequestBody UpdateMessageRequest request) {
    // request에는 content만 있음. userId는 principal에서 가져옴
    Message message = updateMessageUseCase.updateMessage(
            messageId, principal.getUserId(), request.content());
    return ResponseEntity.ok(UpdateMessageResponse.from(message));
}

// 메시지 삭제: 본인 메시지만 삭제 가능
@DeleteMapping("/{messageId}")
public ResponseEntity<MessageResponse> deleteMessage(
        @AuthenticationPrincipal CustomUserPrincipal principal,
        @PathVariable Long messageId) {
    deleteMessageUseCase.deleteMessage(messageId, principal.getUserId());
    return ResponseEntity.ok(MessageResponse.of("메시지가 삭제되었습니다."));
}

// 답장: senderId를 DTO에서 받지 않음
@PostMapping("/{messageId}/reply")
public ResponseEntity<SendMessageResponse> replyToMessage(
        @AuthenticationPrincipal CustomUserPrincipal principal,
        @PathVariable Long messageId,
        @Valid @RequestBody ReplyMessageRequest request) {
    // ReplyMessageRequest는 content만 담고 있음
    Message message = messageReplyForwardUseCase.replyToMessage(
            principal.getUserId(), messageId, request.content());
    return ResponseEntity.status(HttpStatus.CREATED)
            .body(SendMessageResponse.from(message));
}
```

수정, 삭제, 답장 모두 동일한 원칙이다. DTO는 비즈니스 데이터만, 신원은 서버가 결정.

---

## WebSocket에서의 적용

REST API는 `@AuthenticationPrincipal`로 깔끔하게 해결됐지만, WebSocket은 요청 구조가 다르다. STOMP 프레임에는 HTTP 요청처럼 `@AuthenticationPrincipal`을 직접 붙일 수 없다.

해결 방법은 `StompHeaderAccessor`를 통해 WebSocket 세션에서 인증 정보를 꺼내는 것이다.

```java
// WebSocket DTO — userId 없음
public record ChatMessageRequest(
        @NotNull(message = "채팅방 ID는 필수입니다.")
        Long roomId,

        @NotBlank(message = "메시지 내용은 필수입니다.")
        @Size(max = MessageConstants.MAX_MESSAGE_LENGTH)
        String content
) {}
```

```java
@MessageMapping("/chat/message")
public void sendMessage(@Payload ChatMessageRequest request,
                        StompHeaderAccessor headerAccessor) {
    // STOMP 헤더에서 인증된 사용자 ID 추출
    Long authenticatedUserId = extractUserId(headerAccessor);

    // 채팅방 멤버십 검증 (추가 방어선)
    validateChatRoomMembership(request.roomId(), authenticatedUserId);

    sendMessageUseCase.sendMessageWithContext(
            request.roomId(), authenticatedUserId, request.content());
}

private Long extractUserId(StompHeaderAccessor headerAccessor) {
    if (headerAccessor == null || headerAccessor.getUser() == null) {
        throw new IllegalArgumentException("User authentication information is missing");
    }
    // getUser().getName()은 STOMP CONNECT 시 JWT 검증을 통해 서버가 설정한 값
    String userIdStr = headerAccessor.getUser().getName();
    try {
        return Long.parseLong(userIdStr);
    } catch (NumberFormatException e) {
        throw new IllegalArgumentException("Invalid user ID format: " + userIdStr, e);
    }
}
```

흐름을 정리하면:

1. 클라이언트가 STOMP CONNECT 프레임에 JWT를 담아 연결 시도
2. 서버의 WebSocket 인증 인터셉터가 JWT를 검증
3. 검증 성공 시 `headerAccessor.setUser()`로 Principal 설정 (userId를 name으로)
4. 이후 모든 메시지 핸들러에서 `headerAccessor.getUser().getName()`으로 신뢰 가능한 userId 접근

WebSocket도 REST와 동일한 원칙: **클라이언트가 보낸 userId는 무시, 서버가 설정한 Principal만 사용.**

---

## 리팩토링 규모와 전략

이 작업은 결코 작은 변경이 아니었다. **45개 파일, 432줄 추가, 438줄 삭제.** 거의 모든 컨트롤러와 연관 DTO를 건드린 전면 수정이었다.

이런 대규모 변경에서 중요한 건 순서다. 잘못된 순서로 진행하면 중간에 빌드가 깨진 채로 며칠을 보낼 수 있다.

진행한 순서:

1. **테스트 코드 먼저 수정**: 컨트롤러 테스트의 DTO 생성 부분에서 userId 파라미터 제거. 테스트가 새 인터페이스를 기준으로 작성되면 이후 구현 수정 시 빠르게 검증 가능
2. **DTO 변경**: `senderId`, `userId` 필드 제거. record 타입이라 변경 범위가 명확
3. **컨트롤러 수정**: `@RequestBody`에서 받던 userId를 `@AuthenticationPrincipal`로 전환
4. **서비스 레이어 확인**: 서비스 메서드 시그니처가 userId를 파라미터로 받는 구조는 그대로 유지. 서비스는 컨트롤러가 "어디서" userId를 가져왔는지 모르고 알 필요도 없다

DTO와 컨트롤러 경계에서만 변경이 일어났고, 서비스와 도메인 레이어는 그대로였다. 헥사고날 아키텍처의 경계가 변경 전파를 막아준 덕분이다.

---

## 아이러니: 잔재는 테스트 코드에 남아 있었다

이 리팩토링 이후에도 한동안 테스트 코드에는 senderId를 직접 지정하는 패턴이 남아 있었다. 프로덕션 코드는 안전해졌지만, 테스트 유틸리티나 픽스처에서 `senderId`를 하드코딩하는 코드가 군데군데 살아남은 것이다.

나중에 시리즈 12편에서 다룰 테스트 관련 이슈도 이 잔재에서 비롯됐다. "보안 리팩토링을 제대로 했으면 그 문제도 없었을 텐데"라고 할 수 있지만, 돌이켜보면 **프로덕션 코드와 테스트 코드를 함께 정합성 있게 유지하는 것**이 얼마나 중요한지를 보여주는 사례였다.

---

## 정리

| 항목 | Before | After |
|---|---|---|
| senderId 출처 | 클라이언트 요청 본문 | JWT (서버 검증) |
| 위장 공격 가능 여부 | 가능 | 불가 |
| 컨트롤러 파라미터 | `@RequestBody`에 userId 포함 | `@AuthenticationPrincipal` 별도 주입 |
| WebSocket userId 출처 | 페이로드 DTO | `StompHeaderAccessor.getUser()` |

이 리팩토링을 통해 배운 교훈 두 가지:

**1. 인증(Authentication)과 신원(Identity)을 혼동하지 마라.**
JWT로 "이 사람이 로그인한 사용자다"는 증명했어도, 그 사람이 "누구로 행동할 것인지"는 서버가 결정해야 한다.

**2. 대규모 리팩토링도 올바른 순서면 안전하다.**
테스트 먼저 → DTO → 컨트롤러 → 확인. 45개 파일을 건드려도 각 단계에서 컴파일과 테스트가 통과하면 무너지지 않는다.

---

참고:
- [OWASP - IDOR (Broken Object Level Authorization)](https://owasp.org/API-Security/editions/2023/en/0xa1-broken-object-level-authorization/)
- [Spring Security - @AuthenticationPrincipal](https://docs.spring.io/spring-security/reference/servlet/integrations/mvc.html#mvc-authentication-principal)
- [STOMP over WebSocket with Spring](https://docs.spring.io/spring-framework/reference/web/websocket/stomp/authentication.html)
