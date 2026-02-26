# Java 25를 쓰려면 Gradle 9가 필수다

> Co-Talk 개발 일지 10편 — 이슈 #126, PR #125

---

## TL;DR

Java 25를 쓰고 싶으면 Gradle 9.1 이상으로 올려야 한다. Gradle 8.x는 Java 25 class file(major version 69)을 읽지 못하고 빌드 자체가 터진다. 플러그인 호환성도 같이 챙겨야 한다.

---

## 1. 증상 — CI가 처음 터졌을 때

어느 날 GitHub Actions CI가 빌드 단계에서 바로 실패했다. 로컬에서는 멀쩡하게 돌아가는데.

<!-- IMAGE: CI 빌드 실패 로그 — GitHub Actions 로그에서 "Unsupported class file major version 69" 에러가 빨간색으로 표시된 화면 캡처 -->

```
BUG! exception in phase 'semantic analysis' in source unit '_BuildScript_'
Unsupported class file major version 69
```

에러 메시지가 꽤 명확했다. `class file major version 69`를 지원하지 않는다고 한다.

CI 환경과 로컬 환경의 차이를 먼저 떠올렸다. 로컬에서는 IntelliJ IDEA가 자체 컴파일러로 돌리기 때문에 Gradle을 안 쓰는 경우가 생긴다. 덕분에 문제가 로컬에서 안 보였던 것이다.

---

## 2. 원인 — class file major version이 뭔데?

Java 컴파일러는 `.class` 파일 헤더에 **major version** 숫자를 박아둔다. JVM은 이 숫자를 보고 "이 파일이 어느 Java 버전으로 컴파일됐는지" 판단한다.

| Java 버전 | Class File Major Version |
|-----------|--------------------------|
| Java 8    | 52                       |
| Java 11   | 55                       |
| Java 17   | 61                       |
| Java 21   | 65                       |
| Java 23   | 67                       |
| Java 24   | 68                       |
| **Java 25**   | **69**               |

Co-Talk은 `build.gradle.kts`에서 Java 25 툴체인을 명시하고 있다.

```kotlin
// build.gradle.kts
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}
```

Java 25로 컴파일된 `.class` 파일의 major version은 69다. 그런데 당시 사용 중이던 Gradle 8.7은 69를 모른다. Gradle 자체가 Groovy/Kotlin DSL 스크립트를 컴파일할 때 JVM 위에서 돌아가는데, 새 버전 class file을 만나면 `semantic analysis` 단계에서 바로 터진다.

### Gradle 8.x에서 Java 25 백포트 계획 없음

[gradle/gradle#35111](https://github.com/gradle/gradle/issues/35111) 이슈를 보면 Gradle 팀은 Java 25 지원을 8.x 브랜치에 백포트하지 않겠다고 명확히 밝혔다. 이유는 간단하다 — Gradle 9가 이미 나왔으니까.

---

## 3. 해결 — Gradle 9.3.1로 업그레이드

### 3-1. Gradle Wrapper 버전 변경

```properties
# gradle/wrapper/gradle-wrapper.properties (변경 전)
distributionUrl=https\://services.gradle.org/distributions/gradle-8.7-bin.zip
```

```properties
# gradle/wrapper/gradle-wrapper.properties (변경 후)
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-9.3.1-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

Java 25 공식 지원은 Gradle 9.1부터 시작됐다. 9.3.1은 현재(2026.02) 기준 최신 안정 버전이다.

### 3-2. foojay-resolver-convention 플러그인 업그레이드

Gradle wrapper만 올리면 끝날 것 같았지만 두 번째 문제가 있었다.

```
> Could not resolve org.gradle.toolchains.foojay-resolver-convention:1.0.0
  Plugin class 'io.github.gradlex.javaecosystem.toolchains.FoojayToolchainsConventionPlugin'
  references 'JvmVendorSpec.IBM_SEMERU' which is not available in this version of Gradle
```

`foojay-resolver-convention` 플러그인은 JDK 툴체인 자동 다운로드를 담당하는 플러그인이다. 기존 0.9.0 버전은 내부적으로 `JvmVendorSpec.IBM_SEMERU`를 참조하는데, Gradle 9가 이 API를 제거했다.

```kotlin
// settings.gradle.kts (변경 전)
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}
```

```kotlin
// settings.gradle.kts (변경 후)
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
```

1.0.0 버전에서 `IBM_SEMERU` 참조가 제거되고 Gradle 9 호환성이 확보됐다. 최종 `settings.gradle.kts`는 딱 이 세 줄이다.

```kotlin
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "co-talk"
```

---

## 4. Gradle 9 마이그레이션 — 주의할 포인트들

Gradle 메이저 버전 업그레이드는 항상 Deprecated API 정리를 동반한다.

### 4-1. `configurations.compile` 제거

Gradle 7부터 deprecated됐던 `configurations.compile`이 Gradle 9에서 완전히 제거됐다. `configurations.implementation`을 써야 한다.

```kotlin
// 틀림 (Gradle 9에서 빌드 실패)
configurations.compile.exclude(group = "commons-logging")

// 맞음
configurations.all {
    exclude(group = "commons-logging", module = "commons-logging")
}
```

Co-Talk `build.gradle.kts`는 이미 `configurations.all`을 쓰고 있었기 때문에 이 부분은 문제가 없었다.

```kotlin
// build.gradle.kts — 현재 코드
configurations.all {
    exclude(group = "commons-logging", module = "commons-logging")
}
```

### 4-2. Gradle 9 호환성 매트릭스

| Gradle 버전 | 지원 Java (빌드 도구) | 지원 Java (컴파일 대상) |
|-------------|----------------------|------------------------|
| 8.5         | Java 8 ~ 21          | Java 8 ~ 21            |
| 8.7         | Java 8 ~ 22          | Java 8 ~ 22            |
| 9.0         | Java 8 ~ 24          | Java 8 ~ 24            |
| **9.1+**    | **Java 8 ~ 25**      | **Java 8 ~ 25**        |
| 9.3.1       | Java 8 ~ 25+         | Java 8 ~ 25+           |

### 4-3. 플러그인 호환성 체크리스트

Gradle 9로 올릴 때 기존 플러그인이 호환되는지 반드시 확인해야 한다. 체크 순서:

1. 플러그인 GitHub/릴리스 노트에서 "Gradle 9 compatible" 언급 확인
2. 플러그인 issue tracker에서 Gradle 9 관련 이슈 검색
3. 로컬에서 `./gradlew dependencies` 실행해서 resolution 에러 없는지 확인

---

## 5. Java 25 — 왜 쓰나

### LTS 릴리스

Java 21 다음 LTS가 Java 25다. 6개월마다 feature release가 나오고, 3년마다 LTS가 나오는 패턴 기준으로 보면:

| 버전 | 릴리스 | LTS 여부 |
|------|--------|----------|
| Java 17 | 2021.09 | LTS |
| Java 21 | 2023.09 | LTS |
| **Java 25** | **2025.09** | **LTS** |
| Java 29 | 2027.09 | LTS (예정) |

프로덕션 서비스라면 LTS를 타겠지만, 사이드 프로젝트에서는 최신 기능을 써볼 수 있다.

### Virtual Threads

Java 21에서 정식 출시된 Virtual Threads가 Java 25에서 더욱 안정화됐다. Spring Boot 3.5도 공식 지원한다.

```yaml
# application.yml — 한 줄로 Virtual Threads 활성화
spring:
  threads:
    virtual:
      enabled: true
```

이걸 켜면 Tomcat, Spring MVC, `@Async`, JPA 등이 모두 Virtual Thread로 실행된다. 스레드 풀 관리 없이 높은 동시성을 처리할 수 있다.

Co-Talk은 WebSocket + 실시간 채팅 구조라서 Virtual Threads의 이점이 크다. 연결 수가 늘어도 스레드 풀 고갈 없이 처리 가능하다.

### Pattern Matching, Records, Sealed Classes 안정화

Java 25에는 수년간 preview를 거쳐 안정화된 기능들이 들어있다:

- **Record Patterns** (JEP 440) — instanceof + 구조 분해를 한 번에
- **Sealed Classes** (JEP 409) — 상속 계층 명시적 제한
- **Pattern Matching for switch** (JEP 441) — switch로 타입 분기

Co-Talk의 도메인 예외 계층 (`DomainException` + 34개 구체 예외)이 sealed classes와 잘 맞는 구조다. 향후 리팩토링 포인트다.

---

## 6. 교훈

### IDE가 뒤에서 알아서 해주면 문제를 모르고 지나간다

IntelliJ IDEA는 프로젝트의 Java 버전 설정을 보고 자체 JDK로 컴파일한다. Gradle wrapper 버전과 무관하게 컴파일이 되니까 로컬에서는 에러가 전혀 없다.

CI는 다르다. `./gradlew build`를 직접 실행하니까 Gradle wrapper 버전이 그대로 쓰인다. 덕분에 이 문제는 CI에서 먼저 발견됐다. 어떻게 보면 CI가 로컬 환경을 너무 믿지 않게 해주는 안전망 역할을 한 셈이다.

### 빌드 도구와 언어 버전은 세트로 관리해야 한다

Java 버전 올릴 때 `build.gradle.kts`의 `languageVersion`만 바꾸면 끝이라고 생각하기 쉽다. 실제로는 Gradle 버전, 플러그인 버전까지 전부 함께 확인해야 한다.

체크리스트로 정리하면:

- [ ] Gradle wrapper 버전이 목표 Java 버전을 지원하는가
- [ ] 사용 중인 플러그인들이 새 Gradle 버전과 호환되는가
- [ ] CI 환경의 JDK 버전이 목표 버전과 일치하는가

### Gradle 메이저 버전 업그레이드는 플러그인 호환성 체크가 필수다

Gradle 8 → 9는 마이너 업그레이드가 아니다. Deprecated API가 실제로 제거되고, 플러그인이 참조하던 API가 사라진다. `foojay-resolver-convention`의 `IBM_SEMERU` 이슈가 딱 그 케이스였다.

---

<!-- IMAGE: 수정 후 CI 빌드 성공 화면 — GitHub Actions에서 build-and-push Job이 초록 체크로 통과된 화면. 특히 "Run tests" 스텝이 SUCCESS인 부분 캡처 -->

## 7. 최종 변경 요약

| 파일 | 변경 내용 |
|------|----------|
| `gradle/wrapper/gradle-wrapper.properties` | `gradle-8.7-bin.zip` → `gradle-9.3.1-bin.zip` |
| `settings.gradle.kts` | `foojay-resolver-convention` `0.9.0` → `1.0.0` |

단 두 파일, 두 줄 변경이었다. CI가 바로 통과했다.

---

## 참고

- [Gradle Java compatibility matrix](https://docs.gradle.org/current/userguide/compatibility.html)
- [gradle/gradle#35111 — Java 25 support](https://github.com/gradle/gradle/issues/35111)
- [foojay-resolver-convention releases](https://github.com/gradle/foojay-toolchains/releases)
- [JEP 444 — Virtual Threads](https://openjdk.org/jeps/444)
- [Spring Boot 3.5 Virtual Threads support](https://docs.spring.io/spring-boot/docs/3.5.x/reference/html/features.html#features.spring-application.virtual-threads)
