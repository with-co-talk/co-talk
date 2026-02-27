# CI만 터지는 이유 — MinIO 설정 한 줄의 차이

> "57개 테스트가 전부 실패했다. 에러 메시지는 전부 `ConnectException`."

로컬에서는 아무 문제 없다. 그런데 GitHub Actions를 열면 빨간불이 57개. 스택 트레이스를 따라가 보면 MinIO에 연결하려다 실패하고 있었다.

이 글은 Co-Talk 프로젝트에서 실제로 겪은 [이슈 #30](https://github.com/with-co-talk/co-talk/issues/30)의 기록이다.

---

## 프로젝트 맥락

Co-Talk은 파일 업로드를 위해 MinIO를 S3 호환 오브젝트 스토리지로 사용한다.

로컬 개발 환경은 Docker Compose로 MinIO를 띄운다. 프로덕션은 실제 MinIO 인스턴스에 연결한다.
그런데 CI 환경에는 MinIO가 없다.

문제는 여기서 시작됐다.

---

## 증상

<!-- IMAGE: CI 실패 GitHub Actions 스크린샷 — 57건 실패가 표시된 Actions 워크플로우 화면. 빨간 X 아이콘과 실패 개수가 잘 보이도록 캡처 -->

GitHub Actions 로그를 열면 이런 스택 트레이스가 57번 반복된다.

```
software.amazon.awssdk.core.exception.SdkClientException:
  Unable to execute HTTP request: Connect to localhost:9000 [localhost/127.0.0.1]
  failed: Connection refused (Connection refused)

Caused by: org.apache.http.conn.HttpHostConnectException:
  Connect to localhost:9000 [localhost/127.0.0.1] failed: Connection refused

Caused by: java.net.ConnectException: Connection refused
```

`localhost:9000`. MinIO 기본 포트다. CI 환경에는 MinIO가 없으니 연결이 거부되는 것은 당연하다.

그런데 왜 테스트 컨텍스트에서 MinIO에 연결을 시도하는 걸까?

---

## 원인 — `matchIfMissing = true`의 함정

프로젝트의 `InMemoryFileStorage`를 먼저 살펴보자.

```java
@Component
@ConditionalOnProperty(name = "minio.enabled", havingValue = "false", matchIfMissing = true)
public class InMemoryFileStorage implements FileStorage {
    // ...
}
```

`matchIfMissing = true`. 이 설정의 의미는 **`minio.enabled` 속성이 존재하지 않으면 이 빈을 활성화한다**는 뜻이다.

`MinioFileStorage`와 `MinioConfig`는 반대 방향이다.

```java
@Configuration
@ConditionalOnProperty(name = "minio.enabled", havingValue = "true", matchIfMissing = false)
public class MinioConfig {
    // S3Client, S3Presigner 빈 생성
}

@Component
@ConditionalOnProperty(name = "minio.enabled", havingValue = "true", matchIfMissing = false)
public class MinioFileStorage implements FileStorage {
    // 생성자에서 MinIO 버킷 존재 여부 확인 → 여기서 연결 시도
}
```

`MinioFileStorage`의 `matchIfMissing = false`다. **`minio.enabled` 속성이 없으면 활성화되지 않는다.**

이론적으로는 완벽하다. 속성이 없으면 `InMemoryFileStorage`가 활성화되고, `minio.enabled=true`로 명시해야만 MinIO가 활성화된다.

그런데 현실은?

`application-test.yml`에 `minio.enabled: false`가 **없었다**.

속성이 아예 없으니 두 조건 모두 평가된다. `matchIfMissing = false`인 `MinioFileStorage`는 활성화되지 않아야 한다. 그런데 문제는 `MinioConfig`에 있었다.

실제로 이 프로젝트의 초기 `application-test.yml`에는 `minio.enabled` 설정 자체가 빠져 있었다. `MinioConfig`에서 `S3Client` 빈을 만들려다 MinIO에 연결을 시도하고, CI 환경에 MinIO가 없으니 57개 테스트가 전부 컨텍스트 로드 단계에서 터진 것이다.

---

## 해결 — 한 줄 추가

```yaml
# application-test.yml (수정 후)
minio:
  enabled: false  # CI 환경에는 MinIO가 없으므로 InMemoryFileStorage 사용
```

<!-- IMAGE: 수정 후 CI 통과 스크린샷 — minio.enabled: false 추가 커밋 이후 Actions 워크플로우에서 모든 테스트 초록불인 화면 캡처 -->

끝이다. 이 한 줄이 57개 테스트 실패의 원인이었다.

`minio.enabled: false`를 명시하면:

- `MinioConfig` → 비활성화 (`havingValue = "true"` 조건 불충족)
- `MinioFileStorage` → 비활성화 (같은 이유)
- `InMemoryFileStorage` → 활성화 (`havingValue = "false"` 조건 충족)

S3 SDK는 아예 초기화되지 않는다. 당연히 연결 시도도 없다.

---

## 왜 로컬에서는 통과했나

로컬 `application.yml`에는 `minio.enabled: true`가 설정되어 있고, Docker Compose로 MinIO가 실행 중이다. 테스트를 실행할 때 `test` 프로파일을 적용하지만, `minio.enabled`가 test 프로파일에 없으면 기본 프로파일의 값이 사용될 수 있다. 혹은 로컬 개발자는 `application-local.yml`에서 별도 관리하고 있었다.

CI는 외부 설정 파일이 없다. 레포에 커밋된 파일이 전부다. `application-test.yml`에 `minio.enabled`가 없으면 기본값 로직이 동작하고, `MinioConfig`가 로드되어 연결을 시도한다.

**로컬 환경은 실제로 MinIO가 있으니 성공한다. CI는 없으니 실패한다.** 전형적인 "로컬 올 그린, CI 올 레드" 패턴이다.

---

## 더 넓은 교훈 — `@ConditionalOnProperty` 패턴의 명시성

이 프로젝트는 인프라 의존성마다 `@ConditionalOnProperty`로 온/오프를 전환하는 패턴을 쓴다.

| 인프라 | 활성화 조건 | 비활성화 대체 |
|--------|------------|-------------|
| MinIO | `minio.enabled=true` | `InMemoryFileStorage` |
| Redis | 별도 프로파일 | `InMemoryBroker` |
| Firebase | `firebase.enabled=true` | Noop 구현체 |

이 패턴의 장점은 명확하다. 인프라 없이도 애플리케이션이 돌아가고, 테스트 환경에서 외부 의존성 없이 단위/통합 테스트를 실행할 수 있다.

단점은 **명시적으로 false를 설정하지 않으면 기본값에 의해 예상치 못한 빈이 활성화될 수 있다**는 점이다.

`matchIfMissing = true`는 편리하지만, 새 인프라를 추가할 때 반드시 테스트 프로파일에 해당 설정을 추가해야 한다는 책임을 개발자에게 부여한다. 이 책임을 잊으면 57개 테스트가 CI에서 빨간불을 켠다.

---

## 테스트 프로파일 체크리스트

새 테스트 프로파일을 추가하거나 새 인프라 의존성을 추가할 때 반드시 확인해야 할 목록이다.

```yaml
spring:
  flyway:
    enabled: false       # H2는 PostgreSQL 전용 마이그레이션 불가

jwt:
  secret: test-secret-key-for-testing-purposes-only-1234567890

firebase:
  enabled: false         # FCM 연결 시도 방지

minio:
  enabled: false         # S3 클라이언트 초기화 방지 — 이게 핵심

app:
  encryption:
    key: dGhpc2lzYXRlc3RrZXlmb3JkZXZlbG9wbWVudG9ubHk=
    enabled: false       # 환경변수 ${ENCRYPTION_KEY} 없어도 컨텍스트 로드 가능
```

기본값 없는 환경변수(`${ENCRYPTION_KEY}` 같은 형태)를 참조하는 설정이 있다면 테스트 프로파일에서 더미 값으로 채워줘야 한다. 그렇지 않으면 CI에서 컨텍스트 로드 자체가 실패한다.

---

## 핵심 교훈

**`@ConditionalOnProperty(matchIfMissing = true)`를 쓰는 빈이 있다면, 테스트 프로파일에 명시적으로 `false`를 설정하지 않는 한 그 빈은 항상 활성화된다.**

속성 부재(missing)가 암묵적인 `true`로 해석되는 것이다. 새 인프라 의존성을 추가할 때마다 테스트 프로파일 업데이트는 선택이 아니라 필수다.

이 패턴은 이 시리즈에서 반복해서 등장한다. Flyway 설정 누락(1편)도, MinIO 설정 누락(이번 글)도, 암호화 키 누락도 모두 같은 구조의 문제다. **CI 환경에 없는 것은 테스트 프로파일에서 명시적으로 끄거나, 더미 값으로 대체해야 한다.**

로컬에서는 모든 것이 있으니 통과한다. CI에서는 없으니 터진다. 그 차이를 코드로 방어하는 것이 테스트 프로파일의 역할이다.
