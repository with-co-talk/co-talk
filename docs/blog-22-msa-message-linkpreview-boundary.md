# Message와 LinkPreview를 어떻게 끊을 것인가 - 세 번째 경계 리팩토링

> "작은 도메인일수록 경계를 대충 넘기기 쉽다. 그런데 그런 작은 예외가 나중에 제일 성가신 결합이 된다."

이전 두 글에서는 `notification`, `auth-user` 경계를 정리했다.  
이번에는 `message` 와 `linkpreview` 경계를 다뤘다.

겉보기엔 이 둘은 아주 밀접하다.

- 메시지 안에 URL이 들어 있고
- 링크 미리보기를 메시지에 붙이고
- 결과를 다시 채팅방에 브로드캐스트한다

그래서 자연스럽게 "같은 흐름 아닌가?"라고 생각하기 쉽다.

하지만 MSA를 준비하는 관점에서는 이런 질문을 던져야 한다.

`message가 linkpreview 모듈의 인바운드 유스케이스를 직접 알아야 할까?`

이번 작업의 답은 `아니오`였다.

---

## 문제는 MessageLinkPreviewService에 있었다

기존 `MessageLinkPreviewService` 는 링크 미리보기 조회를 위해 `linkpreview` 모듈의 인바운드 유스케이스를 직접 의존하고 있었다.

대략 구조는 이랬다.

```java
private final GetLinkPreviewUseCase getLinkPreviewUseCase;
```

그리고 내부에서 이렇게 호출했다.

```java
LinkPreviewResult result = getLinkPreviewUseCase.getLinkPreview(url);
```

모놀리스 관점에서는 익숙한 코드다.  
하지만 경계 관점에서는 `message` 가 `linkpreview` 의 진입 인터페이스를 직접 알고 있는 구조다.

이 구조를 그대로 두면 다음 문제가 생긴다.

- message가 linkpreview 모듈의 내부 계약을 알아야 한다
- linkpreview의 입구가 외부 모듈에 노출된다
- 나중에 linkpreview를 별도 서비스나 별도 모듈로 떼어낼 때 수정 범위가 커진다

즉 지금은 편해 보여도, 미래 비용은 분명히 올라간다.

---

## 이번 작업의 목표

이번 작업의 목표는 이전 두 글과 동일한 패턴이었다.

1. `linkpreview` 외부 application 서비스는 `linkpreview` 인바운드 포트에 의존하지 않는다
2. 대신 outbound query port를 통해 링크 미리보기를 요청한다
3. 기존 메시지 링크 미리보기 동작은 그대로 유지한다
4. 아키텍처 테스트로 규칙을 잠근다

즉 "링크 미리보기 기능 확장"이 아니라, "경계 정리"가 핵심이었다.

---

## TDD - 먼저 아키텍처 테스트를 깨뜨렸다

먼저 ArchUnit 테스트에 이런 규칙을 추가했다.

- `application.service.linkpreview` 밖의 서비스는
- `domain.port.inbound.linkpreview` 에 의존하면 안 된다

테스트 이름도 의도를 그대로 드러냈다.

```java
@DisplayName("LinkPreview 외 Application 서비스는 LinkPreview 인바운드 포트에 의존하지 않는다")
```

이 테스트를 추가하고 돌리면 바로 실패한다.  
실제로 `MessageLinkPreviewService` 가 그 포트를 직접 의존하고 있었기 때문이다.

이번에도 실패하는 테스트가 리팩토링의 기준점이 됐다.

---

## 새로 만든 포트 - LinkPreviewQueryPort

이번에는 `command` 가 아니라 `query` 가 맞았다.

왜냐하면 `message` 가 linkpreview에 요청하는 건 상태 변경이 아니라 조회이기 때문이다.

그래서 새로 추가한 포트는 `LinkPreviewQueryPort` 다.

```java
public interface LinkPreviewQueryPort {
    LinkPreviewQueryResult queryLinkPreview(String url);
}
```

이 포트의 의미는 단순하다.

- 외부 모듈은 "이 URL의 미리보기를 조회해달라"고만 요청한다
- 실제 HTML 파싱, Open Graph 추출, fallback 처리 방식은 linkpreview 모듈이 책임진다

즉 `message` 는 더 이상 linkpreview 모듈의 유스케이스 진입 방식에 대해 알 필요가 없다.

---

## 반환 타입도 같이 분리해야 했다

이번 작업에서 흥미로웠던 부분은 "포트만 바꾸면 끝"이 아니었다는 점이다.

처음엔 `GetLinkPreviewUseCase` 대신 `LinkPreviewQueryPort` 로 의존만 바꾸면 된다고 생각할 수 있다.  
그런데 실제로는 반환 타입도 같이 문제였다.

기존 반환값은 `GetLinkPreviewUseCase.LinkPreviewResult` 였다.  
즉 타입 자체가 여전히 `linkpreview` 인바운드 포트에 속해 있었다.

이 상태에서는 생성자 의존만 outbound로 바꿔도, 타입 참조 때문에 여전히 ArchUnit 규칙을 위반한다.

그래서 이번에는 반환 모델도 함께 분리했다.

```java
public record LinkPreviewQueryResult(
    String url,
    String title,
    String description,
    String imageUrl,
    String domain,
    String siteName,
    String favicon
) {}
```

이 작업 덕분에 `message` 모듈은 이제 `linkpreview` 인바운드 타입을 전혀 참조하지 않게 됐다.

작아 보이지만 이런 타입 경계가 실제로 꽤 중요하다.

---

## 구현은 기존 GetLinkPreviewService를 재사용했다

이번에도 구현을 새로 만들지 않았다.

기존의 `GetLinkPreviewService` 가 그대로 두 역할을 함께 맡는다.

- `GetLinkPreviewUseCase`
- `LinkPreviewQueryPort`

그리고 outbound 쪽에서는 `queryLinkPreview(...)` 로 별도 진입점을 제공한다.

즉:

- linkpreview 모듈 내부에서는 기존 인바운드 유스케이스 유지
- 외부 모듈에서는 outbound query port만 사용

이렇게 하면 구현 중복 없이 경계만 분리할 수 있다.

---

## MessageLinkPreviewService는 어떻게 바뀌었나

이제 `MessageLinkPreviewService` 는 `GetLinkPreviewUseCase` 를 직접 보지 않는다.

대신 이렇게 바뀌었다.

```java
private final LinkPreviewQueryPort linkPreviewQueryPort;
```

호출도 이렇게 바뀐다.

```java
LinkPreviewQueryResult result = linkPreviewQueryPort.queryLinkPreview(url);
```

나머지 흐름은 그대로다.

- URL 추출
- 링크 미리보기 조회
- 메시지에 미리보기 데이터 저장
- `LINK_PREVIEW_UPDATED` 이벤트 브로드캐스트

즉 이번에도 기능이 아니라 경계를 바꾼 것이다.

---

## 왜 이게 중요한가

이번 작업은 단순히 "링크 미리보기 포트 하나 뺐다" 이상의 의미가 있다.

### 1. 조회 책임의 소유권이 분명해졌다

링크 미리보기를 어떻게 가져오고, 실패 시 무엇을 반환할지는 `linkpreview` 모듈의 책임이다.  
이제 그 책임이 더 명확하게 그 모듈 안에 남게 됐다.

### 2. message는 업무 의도만 표현하게 됐다

message는 "URL이 있으면 미리보기 조회를 요청한다"까지만 알면 된다.  
실제 구현 세부사항은 모를수록 좋다.

### 3. 나중에 linkpreview 분리가 쉬워진다

향후 linkpreview를 별도 서비스로 떼더라도,

- message는 query port를 통해 요청만 하고
- 실제 데이터 수집은 외부 서비스가 담당하는 구조로 바꾸기 쉬워진다

즉 이번 outbound query port는 미래 분리를 위한 완충 지점 역할을 한다.

---

## 검증

이번 변경은 아래 테스트로 검증했다.

```bash
./gradlew test \
  --tests com.cotalk.architecture.HexagonalArchitectureTest \
  --tests com.cotalk.application.service.message.MessageLinkPreviewServiceTest
```

검증 포인트는 두 가지였다.

- `message -> linkpreview 인바운드` 직접 의존이 사라졌는가
- 링크 미리보기 저장 및 이벤트 발행 동작이 그대로 유지되는가

즉 구조와 기능을 함께 확인했다.

---

## 이번 작업에서 배운 점

이번 작업은 특히 이런 교훈을 남겼다.

`의존성은 생성자 주입만이 아니다. 타입도 의존성이다.`

처음에는 생성자에 들어가는 인터페이스만 바꾸면 끝날 것처럼 보였다.  
하지만 반환 타입이 여전히 인바운드 포트 안쪽 타입이면, 사실상 경계는 여전히 새고 있는 셈이다.

그래서 MSA를 준비할 때는 다음을 같이 봐야 한다.

- 누가 누구를 주입받는가
- 메서드 시그니처가 어떤 타입을 노출하는가
- 반환 모델이 어느 모듈 소속인가

이런 세부가 결국 분리 비용을 결정한다.

---

## 다음 단계

이제 다음으로 볼 만한 후보는 이런 것들이다.

- `chat-core` 와 `realtime` 경계
- `user` 와 `social` 경계
- 계정 삭제처럼 여러 모듈 저장소를 한 번에 건드리는 플로우
- 이벤트 발행과 실제 외부 전송 로직의 분리

여전히 원칙은 같다.

1. 먼저 규칙을 테스트로 세운다
2. 실패를 확인한다
3. 최소 리팩토링으로 통과시킨다
4. 그 결과를 문서화한다

작아 보여도 이 방식이 가장 덜 흔들린다.

---

## 마무리

MSA를 준비한다는 건 서비스 개수를 늘리는 일이 아니다.  
오히려 지금 모놀리스 안에서 각 책임이 어디까지인지 더 엄격하게 묻는 일에 가깝다.

이번 `message-linkpreview` 리팩토링은 그걸 다시 보여줬다.

같이 동작하는 기능이라도, 같은 진입점을 공유해야 하는 건 아니다.  
필요한 건 직접 진입이 아니라, 잘 정의된 경계다.

결국 MSA 준비는 이렇게 작은 리팩토링들의 누적으로 완성된다.
