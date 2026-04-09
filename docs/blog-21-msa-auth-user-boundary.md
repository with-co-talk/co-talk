# Auth와 User를 어떻게 나눌 것인가 - 두 번째 경계 리팩토링

> "모놀리스 안에서 경계를 제대로 세우지 못하면, MSA로 나눌 때 그 대가를 훨씬 크게 치른다."

이전 글에서는 `notification` 경계를 먼저 끊는 작업을 정리했다.  
이번에는 그 다음 단계로 `auth` 와 `user` 경계를 다뤘다.

겉으로 보면 둘은 아주 자연스럽게 붙어 있는 도메인처럼 보인다.

- 로그인은 사용자와 당연히 연결돼 있고
- 온라인 상태도 결국 사용자 상태고
- 계정 정보 역시 user 도메인에 속한다

그래서 보통은 그냥 한 덩어리처럼 다루기 쉽다.

문제는 "자연스럽게 붙어 있다"와 "분리 불가능해야 한다"는 전혀 다른 이야기라는 점이다.

이번 작업의 핵심은 아주 작다.

`auth 서비스가 user 모듈의 인바운드 유스케이스를 직접 호출하지 않게 만들기`

이번에도 기술보다 경계를 먼저 손봤다.

---

## 문제는 LoginService에 있었다

기존 `LoginService`는 로그인 성공 후 사용자를 온라인 상태로 바꾸기 위해 `user` 모듈의 인바운드 유스케이스를 직접 의존하고 있었다.

대략 이런 구조였다.

```java
private final UpdateUserOnlineStatusUseCase updateUserOnlineStatusUseCase;
```

그리고 로그인 성공 후 이렇게 호출했다.

```java
updateUserOnlineStatusUseCase.setOnline(user.getId());
```

모놀리스 안에서는 딱히 이상해 보이지 않는다.  
하지만 경계 관점에서 보면 `auth` 가 `user` 의 "진입점"을 직접 알고 있는 구조다.

이 구조를 그대로 두면 나중에 이렇게 된다.

- `auth` 서비스가 `user` 내부 계약을 알아야 한다
- user 모듈 변경 시 auth도 같이 수정될 가능성이 커진다
- auth/user를 나누려 할 때 분리 비용이 커진다

즉 지금은 작은 의존처럼 보여도, 나중엔 서비스 분리 비용으로 돌아온다.

---

## 이번 작업의 목표

이번 리팩토링의 목표는 다음 네 가지였다.

1. `user` 외부 application 서비스는 `user` 인바운드 포트를 직접 보지 않는다
2. 대신 outbound port를 통해 사용자 상태 변경을 요청한다
3. 기존 로그인 동작은 그대로 유지한다
4. ArchUnit 테스트로 규칙을 고정한다

즉 이번에도 "구조를 테스트로 잠그는 것"이 먼저였다.

---

## TDD - 먼저 실패하는 아키텍처 테스트를 추가했다

먼저 ArchUnit 테스트에 이런 규칙을 추가했다.

- `application.service.user` 밖의 서비스는
- `domain.port.inbound.user` 에 의존하면 안 된다

테스트 이름도 의도를 그대로 담았다.

```java
@DisplayName("User 외 Application 서비스는 User 인바운드 포트에 의존하지 않는다")
```

이 테스트를 돌리면 당연히 실패한다.  
현재 `LoginService`가 그 규칙을 어기고 있었기 때문이다.

이 실패 덕분에 이번 작업은 명확한 기준을 가진 상태에서 시작할 수 있었다.

---

## 새로 만든 포트 - UserStatusCommandPort

이번에도 해결 방식은 `notification` 때와 같다.

다른 모듈이 `user` 에 어떤 작업을 요청해야 한다면, `user` 인바운드를 직접 보는 대신 별도 outbound port를 통해 요청하게 했다.

새로 만든 포트는 `UserStatusCommandPort` 다.

```java
public interface UserStatusCommandPort {
    void setOnline(Long userId);
    void setOffline(Long userId);
    void updateLastActiveAt(Long userId);
}
```

이 포트의 의미는 분명하다.

- 다른 모듈은 "사용자 상태를 바꿔달라"는 요청만 한다
- 그 작업이 내부적으로 어떤 유스케이스를 거치는지는 user 모듈이 책임진다

즉 `auth` 는 이제 "로그인 후 온라인 상태를 켠다"는 업무 의도만 표현하면 된다.

---

## 구현은 기존 서비스를 재사용했다

이번에도 구현을 새로 만들지 않았다.

기존의 `UpdateUserOnlineStatusService` 가 그대로 두 역할을 함께 담당한다.

- `UpdateUserOnlineStatusUseCase`
- `UserStatusCommandPort`

이 방식의 장점은 명확하다.

- 기존 비즈니스 로직을 다시 작성하지 않는다
- 중복 구현이 없다
- 의존 방향만 바꾸고 동작은 그대로 유지할 수 있다

즉 "큰 구조 개편"이 아니라 "작은 경계 정리"로 끝낼 수 있다.

---

## LoginService는 어떻게 달라졌나

이제 `LoginService` 는 `UpdateUserOnlineStatusUseCase` 를 직접 보지 않는다.

대신 이렇게 바뀌었다.

```java
private final UserStatusCommandPort userStatusCommandPort;
```

로그인 성공 후 호출도 이렇게 바뀐다.

```java
userStatusCommandPort.setOnline(user.getId());
```

중요한 건 동작은 그대로라는 점이다.

- 로그인 성공 시 온라인 상태를 켠다
- JWT 토큰 발급 흐름은 그대로다
- 실패 정책도 바뀌지 않는다

즉 이번 변경도 기능 변경이 아니라 경계 변경이다.

---

## 왜 이게 중요한가

이번 작업의 진짜 의미는 `auth` 와 `user` 의 책임을 더 선명하게 만든 데 있다.

### 1. auth는 인증에 집중한다

auth의 책임은 로그인, 토큰 발급, 인증 검증이다.  
사용자 상태 변경이 필요하더라도, 그 내부 처리 방식까지 auth가 알 필요는 없다.

### 2. user는 사용자 상태 변경을 책임진다

온라인/오프라인/마지막 활동 시간 갱신은 user 도메인의 책임이다.  
이 책임의 입구를 user 모듈이 계속 소유하게 된다.

### 3. 나중에 서비스 분리할 때 자연스럽다

향후 auth와 user를 따로 분리한다면,

- auth는 "사용자 상태 변경 요청"만 보내고
- 실제 사용자 상태 반영은 user 서비스가 담당하는 형태로 확장하기 쉬워진다

즉 지금 만든 outbound port는 미래의 서비스 경계를 미리 연습하는 셈이다.

---

## 검증

이번 변경은 아래 테스트로 검증했다.

```bash
./gradlew test \
  --tests com.cotalk.architecture.HexagonalArchitectureTest \
  --tests com.cotalk.application.service.auth.LoginServiceTest
```

검증 포인트는 두 가지였다.

- `auth -> user 인바운드` 직접 의존이 사라졌는가
- 로그인 성공 시 온라인 상태 갱신 동작은 그대로인가

즉 구조와 동작을 같이 확인했다.

---

## 이번 작업에서 배운 점

모듈 경계 리팩토링은 생각보다 작은 코드 수정으로 시작할 수 있다.

이번 작업에서 실제로 바뀐 건 많지 않다.

- 테스트 규칙 하나 추가
- outbound port 하나 추가
- 생성자 주입 타입 변경
- 관련 테스트 수정

하지만 그 결과는 꽤 크다.

- auth가 user 인바운드를 직접 잡지 않게 됐다
- user 모듈의 진입점 소유권이 더 분명해졌다
- auth/user 분리 가능성이 한 단계 올라갔다

이런 변화들이 누적되어야 모놀리스가 "나중에 찢을 수 있는 구조"가 된다.

---

## 다음 단계

이제 다음으로 볼 만한 후보는 이런 것들이다.

- `message` 와 `linkpreview` 경계
- `chat-core` 와 `realtime` 경계
- `user` 와 `social` 경계
- 계정 삭제처럼 여러 도메인이 한 트랜잭션으로 묶인 플로우

중요한 건 순서다.

1. 먼저 경계를 정의하고
2. 테스트로 규칙을 세우고
3. 실패를 확인한 뒤
4. 최소 리팩토링으로 통과시킨다

이번 작업도 그 원칙을 그대로 따랐다.

---

## 마무리

MSA를 준비할 때 흔히 "서비스를 어떻게 나눌까"부터 생각한다.

하지만 실제로는 그 전에 이런 질문이 더 중요하다.

`지금 이 모놀리스 안에서 경계가 제대로 지켜지고 있는가?`

이번 auth-user 리팩토링은 그 질문에 대한 두 번째 답이다.

서비스를 나누기 전에, 먼저 경계를 지켰다.  
그리고 그 규칙을 테스트로 고정했다.

MSA는 그렇게 준비하는 편이 훨씬 덜 아프다.
