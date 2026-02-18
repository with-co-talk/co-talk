# DTO 리팩토링 분석

> **역할**: WebSocket/REST DTO 분리·중복 분석. 전체 리팩토링 계획은 [REFACTORING_GUIDE.md](./REFACTORING_GUIDE.md) 참고.

## 현재 상태

### 1. DTO 분리 현황

#### ✅ 잘 분리된 부분
- **REST API DTO**: `adapter/inbound/rest/dto/` 패키지에 도메인별로 잘 분리됨
  - `auth/`, `message/`, `chatroom/`, `friend/`, `user/` 등
  - 각 DTO는 별도 파일로 관리되고 있음

#### ❌ 분리되지 않은 부분
- **WebSocket 컨트롤러 내부 DTO**: `ChatWebSocketController` 클래스 내부에 5개의 record가 정의됨
  - `ChatMessageRequest`
  - `FileMessageRequest`
  - `AddReactionRequest`
  - `RemoveReactionRequest`
  - `ReactionBroadcastMessage`

- **인프라스트럭처 내부 DTO**: `InMemoryChatMessageBroker` 클래스 내부에 record가 정의됨
  - `WebSocketMessage`

## 문제점

### 1. 중복된 DTO 구조

#### WebSocket vs REST - 반응(Reaction) DTO
```java
// WebSocket 내부
public record AddReactionRequest(Long messageId, Long userId, String emoji) {}

// REST DTO
public record AddReactionRequest(
    @NotNull Long userId,
    @NotBlank String emoji
) {}
```

**차이점**:
- WebSocket 버전은 `messageId`를 포함 (URL 경로가 없어서)
- REST 버전은 `messageId`가 URL 경로 변수로 분리됨
- REST 버전은 validation 어노테이션 포함

#### WebSocket vs REST - 파일 메시지 DTO
```java
// WebSocket 내부
public record FileMessageRequest(
    Long senderId, Long roomId, String fileUrl, ...
) {}

// REST DTO
public record SendFileMessageRequest(
    @NotNull Long senderId,
    @NotNull Long chatRoomId,  // 필드명이 다름!
    @NotBlank String fileUrl, ...
) {}
```

**차이점**:
- 필드명 불일치: `roomId` vs `chatRoomId`
- REST 버전은 validation 어노테이션 포함

### 2. DTO가 분리되지 않은 이유 (추정)

#### WebSocket DTO가 내부에 있는 이유
1. **프로토콜 차이**: WebSocket은 `@MessageMapping`을 사용하고, REST는 `@RequestBody`를 사용
2. **필드 구조 차이**: WebSocket은 URL 경로 변수가 없어서 `messageId`를 DTO에 포함
3. **Validation 차이**: REST는 `@Valid`와 validation 어노테이션을 사용하지만, WebSocket은 다를 수 있음
4. **빠른 개발**: 초기 개발 시 편의를 위해 내부에 정의했을 가능성

#### InMemoryChatMessageBroker의 WebSocketMessage가 내부에 있는 이유
1. **인프라스트럭처 레벨**: 메시지 브로커 구현체 내부에서만 사용
2. **WebSocket 전용**: REST API와는 무관한 WebSocket 전용 메시지 포맷
3. **캡슐화**: 외부에서 직접 사용할 필요가 없어서 내부에 정의

## 리팩토링 제안

### 옵션 1: WebSocket DTO를 별도 패키지로 분리 (권장)

```
adapter/inbound/websocket/dto/
  ├── ChatMessageRequest.java
  ├── FileMessageRequest.java
  ├── AddReactionRequest.java
  ├── RemoveReactionRequest.java
  └── ReactionBroadcastMessage.java
```

**장점**:
- REST DTO와 명확히 구분
- WebSocket 전용 DTO임을 명시
- 재사용성 향상 (다른 WebSocket 핸들러에서도 사용 가능)

**단점**:
- 파일 수 증가
- 패키지 구조가 복잡해짐

### 옵션 2: 공통 DTO 재사용 (제한적)

**가능한 경우**:
- `AddReactionRequest`, `RemoveReactionRequest`는 구조가 거의 동일하지만 `messageId` 포함 여부가 다름
- REST는 `@PathVariable`로 `messageId`를 받고, WebSocket은 DTO에 포함

**해결책**:
- WebSocket 전용 DTO를 별도로 유지하되, 공통 필드는 별도 record로 추출
- 또는 WebSocket DTO가 REST DTO를 확장하는 구조

### 옵션 3: InMemoryChatMessageBroker의 WebSocketMessage 분리

```
adapter/inbound/websocket/dto/
  └── WebSocketMessage.java
```

또는

```
infrastructure/messaging/dto/
  └── WebSocketMessage.java
```

## 권장 리팩토링 계획

### 1단계: WebSocket DTO 분리 (우선순위: 높음)
- `ChatWebSocketController`의 내부 record들을 `adapter/inbound/websocket/dto/`로 이동
- 컨트롤러는 분리된 DTO를 import하여 사용

### 2단계: InMemoryChatMessageBroker DTO 분리 (우선순위: 중간)
- `WebSocketMessage`를 `infrastructure/messaging/dto/`로 이동
- 또는 `adapter/inbound/websocket/dto/`로 이동 (WebSocket 관련이므로)

### 3단계: 공통 필드 추출 검토 (우선순위: 낮음)
- WebSocket과 REST DTO 간 공통 필드가 많다면 공통 base record 고려
- 하지만 프로토콜 차이로 인해 제한적일 수 있음

## 결론

**DTO가 분리되지 않은 이유**:
1. **프로토콜 차이**: WebSocket과 REST는 요청 구조가 다름 (URL 경로 변수 vs DTO 필드)
2. **Validation 차이**: REST는 `@Valid` 사용, WebSocket은 다를 수 있음
3. **개발 편의성**: 초기 개발 시 빠른 구현을 위해 내부에 정의
4. **사용 범위**: WebSocket 전용 DTO는 외부에서 사용할 필요가 없어 보였을 수 있음

**리팩토링 필요성**:
- ✅ **코드 가독성**: 컨트롤러 클래스가 너무 길어짐 (279줄)
- ✅ **재사용성**: 다른 WebSocket 핸들러에서도 사용 가능
- ✅ **일관성**: REST DTO와 동일한 패턴 유지
- ⚠️ **중복 제거**: WebSocket과 REST DTO는 프로토콜 차이로 완전 통합은 어려움
