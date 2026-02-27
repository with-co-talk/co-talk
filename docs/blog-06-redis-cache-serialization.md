# Redis 캐시 직렬화 삽질기 — LocalDateTime과 다형성 ([이슈 #48](https://github.com/with-co-talk/co-talk/issues/48))

> "캐시에 넣을 때는 아무 문제 없었다. 꺼낼 때 터졌다."

`@Cacheable("users")`를 붙이고 User 엔티티를 Redis에 캐싱하기 시작한 날, 배포 직후 첫 번째 로그인 요청에서 `InvalidDefinitionException`이 발생했다.

이 글은 Co-Talk 프로젝트에서 실제로 겪은 [이슈 #48](https://github.com/with-co-talk/co-talk/issues/48)의 기록이다. Redis 직렬화는 "저장"이 아니라 "복원"에서 문제가 생긴다는 것을 몸으로 배운 과정이다.

---

## 프로젝트 맥락

Co-Talk의 인증 흐름은 거의 모든 API 요청에서 User 엔티티를 조회한다. 3편에서 `@Cacheable("users")`로 인메모리 캐시를 붙였고, 멀티 인스턴스 환경을 대비해 Redis 캐시로 전환했다.

`User` 엔티티는 `DomainBaseEntity`를 상속한다. JPA 어노테이션 없이 순수 도메인 모델로 분리된 구조다.

```java
// DomainBaseEntity.java
public abstract class DomainBaseEntity {
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

// User.java
public class User extends DomainBaseEntity {
    private Long id;
    private Email email;
    private LocalDateTime lastActiveAt;
    // ...
}
```

`LocalDateTime` 필드가 세 개다. 이게 첫 번째 문제의 원인이 됐다.

---

## 문제 1 — LocalDateTime 역직렬화 실패

<!-- IMAGE: LocalDateTime 직렬화 설정 전 Redis CLI 화면 — redis-cli에서 GET "users::1" 실행 결과. LocalDateTime이 [2026,2,25,12,0,0,0] 배열 형태로 저장된 raw JSON이 보여야 함 -->

### 증상

API를 처음 호출하면 정상적으로 DB에서 데이터를 읽어 캐시에 저장한다. 두 번째 호출부터 캐시에서 읽어야 하는데, 그 순간 터진다.

```
com.fasterxml.jackson.databind.exc.InvalidDefinitionException:
  Cannot construct instance of `java.time.LocalDateTime`
  (no Creators, like default constructor, exist):
  cannot deserialize from Object value
  (no delegate- or property-based Creator)
```

### 원인

`GenericJackson2JsonRedisSerializer`는 Jackson ObjectMapper를 사용해 직렬화한다. 문제는 기본 `new ObjectMapper()`에는 Java 8 날짜/시간 모듈이 포함되어 있지 않다는 것이다.

기본 설정에서 `LocalDateTime`은 배열로 직렬화된다.

```json
{
  "createdAt": [2026, 2, 25, 12, 0, 0, 0],
  "lastActiveAt": [2026, 2, 25, 11, 30, 0, 0]
}
```

7개 원소짜리 배열이다. 역직렬화할 때 Jackson은 이걸 `LocalDateTime`으로 변환하지 못하고 예외를 던진다.

더 정확히는, `LocalDateTime`에는 Jackson이 인식할 수 있는 생성자나 팩토리 메서드가 없다. `JavaTimeModule`이 그 다리 역할을 한다.

### 해결

`JavaTimeModule`을 등록하고 타임스탬프 직렬화를 비활성화한다.

```java
ObjectMapper redisObjectMapper = new ObjectMapper();
redisObjectMapper.registerModule(new JavaTimeModule());
redisObjectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
```

`WRITE_DATES_AS_TIMESTAMPS`를 끄면 `LocalDateTime`이 ISO-8601 문자열로 직렬화된다.

```json
{
  "createdAt": "2026-02-25T12:00:00",
  "lastActiveAt": "2026-02-25T11:30:00"
}
```

이제 역직렬화가 된다.

---

## 문제 2 — 다형성 타입 정보 누락

### 증상

`LocalDateTime` 문제를 해결하고 다시 테스트했다. 캐시에서 꺼낸 값이 `User` 타입이 아니라 `LinkedHashMap`으로 돌아왔다.

```
java.lang.ClassCastException:
  class java.util.LinkedHashMap cannot be cast to class com.cotalk.domain.entity.User
```

### 원인

`GenericJackson2JsonRedisSerializer`는 기본적으로 JSON에 타입 정보를 포함하지 않는다. 저장된 JSON은 그냥 평범한 객체 구조다.

```json
{
  "id": 1,
  "email": { "value": "test@example.com" },
  "nickname": "testUser"
}
```

이 JSON을 역직렬화할 때 Jackson은 "이게 어떤 타입인지"를 모른다. 타겟 타입 정보가 없으면 기본 타입인 `LinkedHashMap`으로 역직렬화한다.

`@Cacheable`의 반환 타입이 `User`이지만, Spring은 캐시에서 꺼낸 값을 캐스팅하려다 실패한다.

### 해결

`activateDefaultTyping`으로 JSON에 타입 정보를 포함시킨다.

```java
redisObjectMapper.activateDefaultTyping(
    redisObjectMapper.getPolymorphicTypeValidator(),
    ObjectMapper.DefaultTyping.NON_FINAL,
    JsonTypeInfo.As.PROPERTY
);
```

이제 직렬화된 JSON에 `@class` 필드가 붙는다.

```json
{
  "@class": "com.cotalk.domain.entity.User",
  "id": 1,
  "email": {
    "@class": "com.cotalk.domain.model.Email",
    "value": "test@example.com"
  },
  "nickname": "testUser",
  "createdAt": "2026-02-25T12:00:00"
}
```

역직렬화 시 Jackson이 `@class` 값을 보고 정확한 타입으로 인스턴스를 생성한다.

`DefaultTyping.NON_FINAL`은 `final`이 아닌 모든 타입에 타입 정보를 포함한다는 의미다. `User`, `Email` 같은 일반 클래스에 적용된다.

---

## 문제 3 — FAIL_ON_UNKNOWN_PROPERTIES

### 증상

세 번째 문제는 당장 터지지 않았다. 필드를 하나 추가하는 순간 발생할 잠재적 폭탄이었다.

User 엔티티에 `backgroundUrl` 필드가 추가됐다. 기존에 캐시된 데이터에는 이 필드가 없다. 역직렬화할 때 Jackson 기본 설정에서는 알 수 없는 필드가 있으면 그냥 무시하는데, 반대 상황이 문제다.

엔티티에서 필드를 제거하거나 이름을 변경하면 캐시 데이터에 있는 필드를 Jackson이 처리하지 못한다.

```
com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException:
  Unrecognized field "oldFieldName" (class com.cotalk.domain.entity.User),
  not marked as ignorable
```

### 원인

Jackson의 `FAIL_ON_UNKNOWN_PROPERTIES`가 기본값 `true`다. 역직렬화 대상 클래스에 없는 필드가 JSON에 있으면 예외를 던진다.

`new ObjectMapper()`는 이 기본값을 그대로 갖는다. Spring Boot의 기본 `ObjectMapper` 빈은 `application.yml`에서 `spring.jackson.deserialization.fail-on-unknown-properties=false`가 설정되어 있어 비활성화되어 있다. 그런데 우리가 Redis용으로 직접 만든 `ObjectMapper`는 Spring Boot 자동 설정과 무관한 새 인스턴스다.

이 설정을 빼먹으면 TTL이 남아있는 기존 캐시 데이터가 엔티티 변경 후 역직렬화 실패를 일으킨다. 운영 중에 발생하면 TTL이 만료되기 전까지 캐시 기능 전체가 오류를 뿜는다.

### 해결

```java
redisObjectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
```

이 설정이 없으면 엔티티 스키마 변경 시 기존 캐시가 폭탄이 된다.

**이 패턴은 12편에서 STOMP 메시지 역직렬화에서도 그대로 반복된다.** 직렬화 설정 실수는 한 번 겪은 영역에서 또 겪는다. 같은 ObjectMapper 설정 문제가 WebSocket 메시지 처리 레이어에서 재현된다.

---

<!-- IMAGE: 직렬화 설정 전/후 JSON 비교 스크린샷 — 좌측에 배열 형태([2026,2,25,...])의 기존 캐시 JSON, 우측에 JavaTimeModule 적용 후 ISO-8601 문자열("2026-02-25T12:00:00")과 @class 필드가 포함된 JSON을 나란히 캡처 또는 편집 -->

## 최종 CacheConfig

세 가지 문제를 해결한 `CacheConfig`의 전체 코드다.

```java
@Bean
@Profile("!test")
public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
    // Spring Boot 기본 ObjectMapper와 독립적인 Redis 전용 ObjectMapper 생성
    ObjectMapper redisObjectMapper = new ObjectMapper();

    // 문제 1 해결: LocalDateTime 직렬화/역직렬화
    redisObjectMapper.registerModule(new JavaTimeModule());
    redisObjectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    // 문제 3 해결: 엔티티 필드 변경 시 기존 캐시 데이터 역직렬화 실패 방지
    redisObjectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    // 문제 2 해결: 다형성 타입 정보 포함 (LinkedHashMap 역직렬화 방지)
    redisObjectMapper.activateDefaultTyping(
        redisObjectMapper.getPolymorphicTypeValidator(),
        ObjectMapper.DefaultTyping.NON_FINAL,
        JsonTypeInfo.As.PROPERTY
    );

    GenericJackson2JsonRedisSerializer valueSerializer =
        new GenericJackson2JsonRedisSerializer(redisObjectMapper);

    RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(Duration.ofHours(1))
        .serializeKeysWith(RedisSerializationContext.SerializationPair
            .fromSerializer(new StringRedisSerializer()))
        .serializeValuesWith(RedisSerializationContext.SerializationPair
            .fromSerializer(valueSerializer))
        .disableCachingNullValues();

    Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
    cacheConfigurations.put(USER_CACHE, defaultConfig.entryTtl(Duration.ofHours(1)));
    cacheConfigurations.put(CHAT_ROOM_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(30)));
    cacheConfigurations.put(STATISTICS_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(5)));

    return RedisCacheManager.builder(connectionFactory)
        .cacheDefaults(defaultConfig)
        .withInitialCacheConfigurations(cacheConfigurations)
        .build();
}
```

---

## Spring Boot 기본 ObjectMapper vs 직접 생성한 ObjectMapper

이것이 핵심이다. 왜 Redis용 ObjectMapper를 따로 만들어야 하는가.

| 설정 | Spring Boot 기본 `ObjectMapper` | `new ObjectMapper()` |
|---|---|---|
| `JavaTimeModule` | 자동 등록 (`spring-boot-autoconfigure`) | 미포함 |
| `WRITE_DATES_AS_TIMESTAMPS` | `false` (Spring Boot 기본) | `true` |
| `FAIL_ON_UNKNOWN_PROPERTIES` | `false` (`spring.jackson.*` 설정) | `true` |
| `activateDefaultTyping` | 비활성화 | 비활성화 |
| 타입 정보 포함 | 없음 | 없음 |

Spring Boot가 자동 구성하는 `ObjectMapper`는 `application.yml`의 `spring.jackson.*` 설정을 반영한다. `new ObjectMapper()`로 직접 생성한 인스턴스는 Jackson 라이브러리의 기본값만 갖는다. Spring Boot 자동 설정과 완전히 독립적이다.

Redis 캐시에서 타입 정보(`@class`)를 포함하려면 어차피 Spring Boot 기본 `ObjectMapper`를 그대로 쓸 수 없다. `activateDefaultTyping`을 활성화하면 Jackson의 타입 검증 메커니즘이 바뀌어 보안 이슈로 이어질 수 있어서, Spring Boot는 기본 `ObjectMapper`에 이 설정을 넣지 않는다. 따라서 Redis 캐시용 ObjectMapper는 별도로 만들고, 위의 세 가지 설정을 명시적으로 추가해야 한다.

---

## 테스트: RedisCacheSerializationTest

운영 환경의 `CacheConfig`는 `@Profile("!test")`로 테스트 컨텍스트에서 로드되지 않는다. 테스트는 `ConcurrentMapCacheManager`를 사용하기 때문에 Redis 직렬화 경로가 아예 실행되지 않는다.

이 상태에서는 `CacheConfig`의 `ObjectMapper` 설정이 잘못되어도 테스트가 모두 통과한다. 런타임에 배포 후에야 터진다.

이 문제를 막기 위해 `RedisCacheSerializationTest`를 추가했다. Spring 컨텍스트 없이 `GenericJackson2JsonRedisSerializer`를 직접 생성하고, `User` 엔티티를 실제로 직렬화/역직렬화한다.

```java
@Test
@DisplayName("User 엔티티(LocalDateTime 포함)를 직렬화·역직렬화할 수 있다")
void should_serializeAndDeserializeUser_withLocalDateTime() throws Exception {
    LocalDateTime now = LocalDateTime.now();
    User user = User.builder()
        .id(1L)
        .email(new Email("serialize-test@example.com"))
        .nickname("serializeTest")
        .status(User.UserStatus.ACTIVE)
        .role(User.Role.USER)
        .onlineStatus(User.OnlineStatus.OFFLINE)
        .lastActiveAt(now.minusHours(1))
        .build();

    byte[] bytes = valueSerializer.serialize(user);
    Object deserialized = valueSerializer.deserialize(bytes);

    assertThat(deserialized).isInstanceOf(User.class);
    User restored = (User) deserialized;
    assertThat(restored.getLastActiveAt()).isEqualTo(user.getLastActiveAt());
    assertThat(restored.getCreatedAt()).isEqualTo(user.getCreatedAt());
}

@Test
@DisplayName("JavaTimeModule 없이 User를 직렬화하면 예외가 발생한다 (회귀 방지)")
void should_throwWhenSerializingUser_withoutJavaTimeModule() {
    // JavaTimeModule 없는 직렬화기 — 과거에 터진 설정
    GenericJackson2JsonRedisSerializer broken =
        new GenericJackson2JsonRedisSerializer(new ObjectMapper());

    assertThrows(SerializationException.class, () -> broken.serialize(user));
}
```

회귀 방지 테스트가 핵심이다. `JavaTimeModule`이 없는 과거 설정으로 직렬화를 시도하면 `SerializationException`이 발생하는 것을 검증한다. 누군가 `JavaTimeModule` 등록을 제거하면 이 테스트가 즉시 실패한다.

---

## TTL과 스키마 변경의 관계

`FAIL_ON_UNKNOWN_PROPERTIES`를 비활성화해도 완전히 안심할 수는 없다. 필드 타입이 바뀌거나 필드가 제거될 때의 이야기다.

| 변경 유형 | 영향 | 대응 |
|---|---|---|
| 필드 추가 | 기존 캐시에 없음 → 기본값(null)으로 처리 | 안전. `FAIL_ON_UNKNOWN_PROPERTIES=false` 불필요 |
| 필드 제거 | 캐시에 있는 필드를 역직렬화 대상에서 찾을 수 없음 | `FAIL_ON_UNKNOWN_PROPERTIES=false` 필수 |
| 필드명 변경 | 기존 캐시의 구 필드명을 알 수 없는 필드로 처리 + 신규 필드는 null | `FAIL_ON_UNKNOWN_PROPERTIES=false` 필수. 의미적 데이터 손실 가능 |
| 필드 타입 변경 | JSON 타입이 다르면 역직렬화 예외 | 캐시 키 변경 또는 TTL 대기 필요 |

근본적인 해결책은 **스키마 변경이 호환되지 않을 때 캐시 키를 변경하거나 캐시를 플러시**하는 것이다. `FAIL_ON_UNKNOWN_PROPERTIES=false`는 운영 중 무중단을 위한 완충재지, 영원한 해답은 아니다.

TTL이 1시간이라면, 호환되지 않는 스키마 변경 배포 후 1시간 동안은 일부 캐시 히트가 잘못된 데이터를 반환할 수 있다. 이 점을 팀에 공유하고 배포 시 캐시 플러시 여부를 판단해야 한다.

---

## 핵심 교훈

**Redis 직렬화는 "넣을 때"가 아니라 "꺼낼 때" 문제가 발생한다.**

저장은 된다. Jackson이 `LocalDateTime`을 배열로 쓰는 것 자체는 실패하지 않는다. 문제는 그 배열을 다시 `LocalDateTime`으로 복원하려는 순간이다. 타입 정보 없이 저장된 JSON을 구체 타입으로 복원하려는 순간이다. 존재하지 않는 필드를 역직렬화하려는 순간이다.

"저장이 됐으니 괜찮겠지"는 Redis 캐시에서 통하지 않는다.

그리고 **Spring Boot 기본 `ObjectMapper`와 `new ObjectMapper()`는 다른 물건이다.** Spring Boot가 자동으로 설정해주는 것들을 직접 만든 인스턴스는 하나도 모른다. Redis, Kafka, WebSocket 등 직접 ObjectMapper를 생성하는 모든 곳에서 필요한 설정을 명시적으로 추가해야 한다. 이 교훈은 12편에서 다시 등장한다.
