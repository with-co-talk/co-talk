# MSA를 준비하는 첫 리팩토링 - Notification 경계부터 끊기

> "MSA의 시작은 Kafka가 아니라 의존 방향이다."

모놀리스를 MSA로 나누겠다고 마음먹으면 보통 가장 먼저 드는 생각은 이런 것들이다.

- Kafka를 붙여야 하나?
- API Gateway는 뭘 써야 하지?
- 서비스 디스커버리가 필요할까?
- DB는 언제 분리하지?

그런데 실제 프로젝트를 뜯어보면, 진짜 먼저 해야 하는 일은 기술 선택이 아니라 경계 정리다.

이번 글에서는 Co-Talk 백엔드에서 MSA 전환을 염두에 두고 진행한 첫 번째 리팩토링, `notification` 경계 분리 이야기를 정리해보려 한다.

핵심은 단순하다.

`다른 도메인이 notification 모듈의 인바운드 유스케이스를 직접 호출하지 않게 만들기`

이걸 작은 단계로 해두면, 나중에 notification을 별도 서비스로 떼어낼 때 훨씬 수월해진다.

---

## 왜 Notification부터 손봤나

현재 Co-Talk는 헥사고날 아키텍처를 기반으로 구성되어 있다.

- `domain`
- `application`
- `adapter`
- `infrastructure`

구조만 보면 이미 잘 나뉘어 있다. 하지만 "패키지가 나뉘어 있다"와 "서비스 경계가 분리돼 있다"는 전혀 다른 이야기다.

실제 코드를 보면 `message`, `friend` 같은 다른 application 서비스가 notification 기능을 직접 호출하고 있었다.

예를 들면:

- 메시지 전송 후 푸시 알림 전송
- 친구 요청 생성 후 친구 요청 알림 전송

문제는 이 호출이 `notification` 도메인의 인바운드 유스케이스를 직접 의존하고 있었다는 점이다.

즉 의미상으로는 이런 구조였다.

```text
message service -> notification use case
friend service -> notification use case
```

모놀리스 안에서는 큰 문제가 없어 보인다. 하지만 MSA를 생각하기 시작하면 이 구조는 경계를 흐린다.

인바운드 유스케이스는 원래 그 모듈의 "들어오는 입구"인데, 다른 내부 모듈이 그 입구를 직접 잡고 있으면 사실상 내부 결합이 생기기 때문이다.

---

## 바꾸기 전 구조

기존에는 이런 식의 의존이 있었다.

```java
private final SendPushNotificationUseCase sendPushNotificationUseCase;
```

그리고 메시지나 친구 요청 로직 안에서 이렇게 호출했다.

```java
sendPushNotificationUseCase.sendNewMessageNotificationBulk(...);
sendPushNotificationUseCase.sendFriendRequestNotification(...);
```

이 방식은 간단하지만, `message`와 `friend`가 `notification`의 내부 유스케이스 계약을 알아야 한다는 점이 문제다.

MSA 관점에서 보면 이렇게 읽힌다.

- `message`는 알림이 어떻게 처리되는지 안다
- `friend`도 알림 모듈의 진입 인터페이스를 안다
- notification을 떼어내려면 여러 application 서비스가 같이 수정된다

결국 "알림 기능을 쓰는 것"과 "알림 모듈의 진입 방식에 의존하는 것"이 섞여 있었던 셈이다.

---

## 이번 리팩토링의 목표

이번 작업의 목표는 아주 작게 잡았다.

1. `notification` 외부 모듈은 notification 인바운드 포트에 의존하지 않는다
2. 대신 outbound port를 통해 알림 전송을 위임한다
3. 기존 비즈니스 동작은 그대로 유지한다
4. 아키텍처 테스트로 이 규칙을 고정한다

즉 "기능 추가"가 아니라 "경계 고정"이 목적이었다.

---

## TDD로 먼저 규칙을 만들다

이 프로젝트는 TDD가 원칙이다. 그래서 구현보다 먼저 아키텍처 테스트를 추가했다.

추가한 규칙은 이런 의미다.

- `application.service.notification` 밖의 서비스는
- `domain.port.inbound.notification` 에 의존하면 안 된다

테스트 이름도 의도를 그대로 드러내게 작성했다.

```java
@DisplayName("Notification 외 Application 서비스는 Notification 인바운드 포트에 의존하지 않는다")
```

이 테스트를 먼저 넣고 돌리면 당연히 실패한다. 왜냐하면 실제로 `SendMessageService`, `SendFriendRequestService` 가 그 포트를 직접 보고 있었기 때문이다.

이 실패가 이번 리팩토링의 출발점이었다.

---

## 새로 만든 포트: NotificationCommandPort

다른 모듈이 notification에 일을 요청할 때는 인바운드 유스케이스가 아니라 별도의 아웃바운드 포트를 보게 했다.

새로 추가한 포트는 다음과 같은 역할을 가진다.

```java
public interface NotificationCommandPort {
    void sendNewMessageNotification(...);
    void sendNewMessageNotificationBulk(...);
    void sendFriendRequestNotification(...);
}
```

이 이름에서 중요한 건 `Command` 라는 표현이다.

이 포트는 "알림을 어떻게 처리하는지"를 노출하지 않는다. 그저 "알림 전송을 요청한다"는 의미만 제공한다.

즉 `message`와 `friend` 입장에서는:

- notification의 내부 유스케이스를 알 필요가 없고
- 알림이 실제로 FCM인지, 이벤트 기반인지, 다른 서비스 호출인지 몰라도 된다

이 차이가 작아 보여도 경계 설계에서는 매우 중요하다.

---

## 구현은 기존 서비스를 재사용

새 포트를 만들었다고 해서 알림 구현을 새로 만든 건 아니다.

기존의 `SendPushNotificationService`가 그대로 다음 두 역할을 함께 맡도록 했다.

- `SendPushNotificationUseCase`
- `NotificationCommandPort`

즉 notification 모듈 내부에서는 여전히 기존 유스케이스를 사용할 수 있고, 외부 모듈에서는 새 outbound port로만 접근하게 했다.

이렇게 하면 좋은 점이 있다.

- 기존 동작을 거의 건드리지 않는다
- 구현 중복이 없다
- 작은 리팩토링으로 의존 방향만 바로잡을 수 있다
- 나중에 notification을 분리 서비스로 뺄 때 adapter 교체가 쉬워진다

---

## message / friend 서비스는 어떻게 바뀌었나

이제 `SendMessageService` 와 `SendFriendRequestService` 는 더 이상 notification 인바운드 포트를 모른다.

변경 후 의존은 이렇게 바뀌었다.

```java
private final NotificationCommandPort notificationCommandPort;
```

그리고 호출도 이렇게 바뀐다.

```java
notificationCommandPort.sendNewMessageNotificationBulk(...);
notificationCommandPort.sendFriendRequestNotification(...);
```

중요한 건 비즈니스 동작은 그대로라는 점이다.

- 메시지를 보내면 여전히 푸시 알림을 보낸다
- 친구 요청을 보내면 여전히 친구 요청 알림을 보낸다
- 실패 정책도 그대로 유지된다

즉 이번 작업은 "무엇을 하느냐"를 바꾼 게 아니라, "어떤 경계를 통해 하느냐"를 바꾼 것이다.

---

## 알림 실패 정책은 어떻게 봤나

리뷰하면서 자연스럽게 나온 질문이 있다.

"알림이 실패하면 실패 정책이 따로 있나?"

현재 정책은 `best effort`다.

즉:

- 친구 요청 생성이 핵심 비즈니스 성공 조건
- 푸시 알림은 부가 기능
- 알림 실패가 친구 요청 자체를 롤백시키지는 않음

이 정책은 변경 전에도 같았고, 이번 리팩토링에서도 유지했다.

코드에서도 알림 전송은 예외를 잡고 `warn` 로그를 남기는 구조다.

```java
try {
    notificationCommandPort.sendFriendRequestNotification(...);
} catch (Exception e) {
    log.warn(...);
}
```

이 판단은 현실적이다. 알림 실패 때문에 친구 요청 자체가 실패하면 사용자 경험이 더 이상해질 수 있다.

다만 한계도 분명하다.

현재는:

- 재시도 없음
- 보상 처리 없음
- 메트릭/실패율 추적 부족

즉 지금은 모놀리스 단계에서의 합리적인 `best effort` 정책이고, 나중에 MSA나 이벤트 기반 구조로 발전하면 이 부분은 `outbox + retry` 같은 방식으로 보강하는 것이 맞다.

---

## 이번 작업에서 중요한 진짜 포인트

표면적으로는 "알림 포트 하나 만들고 의존성 바꾼 것"처럼 보일 수 있다.

하지만 구조적으로는 이런 의미가 있다.

### 1. 모듈의 진입점과 외부 요청 포트를 분리했다

인바운드 유스케이스는 그 모듈의 입구다. 외부 모듈이 그 입구를 직접 잡지 않도록 막으면서 경계가 선명해졌다.

### 2. MSA 이전의 모듈러 모놀리스 규칙을 세웠다

서비스 분리를 하기 전에 먼저 모놀리스 안에서 경계를 테스트로 고정했다.

### 3. 리팩토링 기준을 ArchUnit으로 잠갔다

"이제부터는 이렇게 한다"를 문서가 아니라 테스트로 남겼다.

### 4. 작은 변경으로 미래의 분리 비용을 줄였다

이제 notification을 나중에 외부 서비스로 뺄 때 수정 범위가 줄어든다.

---

## 검증

이번 변경은 아래 테스트로 검증했다.

```bash
./gradlew test \
  --tests com.cotalk.architecture.HexagonalArchitectureTest \
  --tests com.cotalk.application.service.message.SendMessageServiceTest \
  --tests com.cotalk.application.service.friend.SendFriendRequestServiceTest
```

검증 포인트는 세 가지였다.

- 새 아키텍처 규칙이 통과하는가
- 메시지 전송 흐름이 깨지지 않았는가
- 친구 요청 흐름이 깨지지 않았는가

즉 단순히 컴파일만 맞춘 게 아니라, 구조와 동작 둘 다 확인했다.

---

## 이번 작업에서 배운 점

MSA는 서비스를 쪼개는 작업이 아니라, 경계를 설계하는 작업이다.

그리고 그 경계 설계는 보통 "거대한 분리 작업"으로 시작하지 않는다. 오히려 이런 식의 아주 작은 리팩토링으로 시작된다.

- 다른 모듈의 인바운드 포트를 직접 부르지 않게 만들기
- 공용 outbound port로 바꾸기
- 그 규칙을 테스트로 잠그기

이런 조각들이 쌓여야 나중에 진짜 서비스 분리가 가능해진다.

결국 MSA의 시작은 Kafka가 아니라 `의존 방향`이다.

---

## 다음 단계

이번에 notification 경계를 하나 정리했으니, 다음으로 볼 후보는 이런 것들이다.

- `realtime` 과 `chat-core` 경계
- `social` 과 `user` 경계
- 계정 삭제 같은 cross-domain 트랜잭션 흐름
- notification / realtime / file 같은 보조 기능의 독립 가능성

개인적으로는 다음 작업도 같은 방식으로 가는 게 맞다고 본다.

1. 경계 후보를 고른다
2. ArchUnit 테스트로 먼저 규칙을 만든다
3. 실패를 확인한다
4. 최소 리팩토링으로 통과시킨다

즉 "한 번에 MSA"가 아니라 "테스트로 경계를 굳히는 모듈러 모놀리스" 전략이다.

---

## 마무리

이번 리팩토링은 눈에 띄는 기능 추가가 아니다. 사용자는 아마 아무 변화도 못 느낄 것이다.

하지만 이런 작업이 쌓여야 시스템은 "기능이 많은 코드베이스"에서 "경계가 있는 시스템"으로 변한다.

그리고 MSA를 준비할 때 가장 먼저 필요한 건 바로 그 변화다.

`기술보다 경계, 분리보다 규칙`

Co-Talk의 MSA 준비는 그렇게 시작했다.
