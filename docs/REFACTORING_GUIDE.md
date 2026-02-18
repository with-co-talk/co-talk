# Co-Talk 리팩토링 가이드

> **관련 문서**: DTO 분리·중복 분석은 [DTO_REFACTORING_ANALYSIS.md](./DTO_REFACTORING_ANALYSIS.md), 문서 목차는 [README.md](./README.md) 참고.

## 프로젝트 현황 요약

| 항목 | 현재 상태 |
|------|----------|
| 소스 파일 | 366개 |
| 테스트 파일 | 84개 (23%) |
| 엔티티 | 14개 |
| DTO | 68개 |
| 컨트롤러 | 17개 |

---

## 1. 아키텍처 및 설계 패턴

### 1.1 BaseEntity 적용 (우선순위: 높음) ✅ 부분 완료

**진행 상황**: 
- ✅ `BaseEntity` 클래스 생성 완료 (`domain/entity/BaseEntity.java`)
- ✅ `JpaAuditingConfig` 설정 완료 (`infrastructure/config/JpaAuditingConfig.java`)
- ✅ `BaseEntityTest` 테스트 작성 완료

**BaseEntity 상속 완료된 엔티티** (5개):
- ✅ `domain/entity/User.java`
- ✅ `domain/entity/Message.java`
- ✅ `domain/entity/ChatRoom.java`
- ✅ `domain/entity/Friend.java`
- ✅ `domain/entity/FriendRequest.java`

**BaseEntity 상속 필요 엔티티** (8개):
- ⏳ `domain/entity/Report.java` - `@PrePersist`/`@PreUpdate` 사용 중 (createdAt, updatedAt 모두 필요)
- ⏳ `domain/entity/Block.java` - `@PrePersist`만 사용 (createdAt만 있음, updatedAt 불필요)
- ⏳ `domain/entity/ChatRoomMember.java` - `@PrePersist`만 사용 (`joinedAt` 필드, BaseEntity의 `createdAt`과 매핑 가능)
- ⏳ `domain/entity/MessageReaction.java` - `@PrePersist`만 사용 (createdAt만 있음, updatedAt 불필요)
- ⏳ `domain/entity/NotificationSetting.java` - `@PrePersist`/`@PreUpdate` 사용 중 (createdAt, updatedAt 모두 필요)
- ⏳ `domain/entity/PasswordResetToken.java` - `@PrePersist`만 사용 (createdAt만 있음, updatedAt 불필요)
- ⏳ `domain/entity/TermsAgreement.java` - `@PrePersist`만 사용 (createdAt만 있음, updatedAt 불필요)
- ⏳ `domain/entity/DeviceToken.java` - 생성자에서 직접 설정 (createdAt, updatedAt 모두 있음)

**참고**: `ChatRoomSummary`는 record 타입이므로 엔티티가 아닙니다.

**남은 작업**:
1. 나머지 8개 엔티티에서 `@PrePersist`/`@PreUpdate` 제거 및 `BaseEntity` 상속
2. 특수 필드 처리:
   - `Report`, `NotificationSetting`: `BaseEntity` 상속으로 완전 대체 가능
   - `Block`, `MessageReaction`, `PasswordResetToken`, `TermsAgreement`: `createdAt`만 필요 → `BaseEntity` 상속 후 `updatedAt`은 무시 (필드 유지하되 사용 안 함)
   - `ChatRoomMember`: `joinedAt` → `BaseEntity`의 `createdAt`으로 대체 또는 별도 필드 유지 (비즈니스 로직 확인 필요)
   - `DeviceToken`: 생성자에서 설정하는 부분을 `BaseEntity`로 대체

**예상 효과**: 약 150줄 코드 감소 (5개 완료로 약 50줄 이미 감소)

---

### 1.2 UseCase 인터페이스 폭발 (우선순위: 낮음)

**현재 상태**: 유스케이스별로 개별 인터페이스 존재
```
domain/port/inbound/friend/
├── SendFriendRequestUseCase.java
├── AcceptFriendRequestUseCase.java
├── RejectFriendRequestUseCase.java
├── CancelFriendRequestUseCase.java
├── RemoveFriendUseCase.java
├── GetFriendListUseCase.java
├── GetFriendRequestsUseCase.java
└── GetSentFriendRequestsUseCase.java
```

**개선안**: 관련 유스케이스 그룹화
```java
public interface FriendUseCase {
    // 친구 요청
    Long sendFriendRequest(Long requesterId, Long receiverId);
    void acceptFriendRequest(Long userId, Long requestId);
    void rejectFriendRequest(Long userId, Long requestId);
    void cancelFriendRequest(Long userId, Long requestId);

    // 친구 관리
    void removeFriend(Long userId, Long friendId);
    List<FriendDto> getFriendList(Long userId);

    // 조회
    List<FriendRequestDto> getReceivedRequests(Long userId);
    List<FriendRequestDto> getSentRequests(Long userId);
}
```

---

## 2. JPA/데이터 접근

### 2.1 인덱스 추가 (우선순위: 높음) ✅ 부분 완료

**진행 상황**: 
- ✅ `V2__add_indexes.sql` 마이그레이션 파일 생성 및 적용 완료

**이미 추가된 인덱스** (`V2__add_indexes.sql`):
- ✅ Messages: `idx_messages_chat_room_id`, `idx_messages_chat_room_id_created_at`, `idx_messages_sender_id`, `idx_messages_content_search` (GIN 인덱스)
- ✅ ChatRoomMembers: `idx_chat_room_members_user_id`, `idx_chat_room_members_chat_room_id`, `idx_chat_room_members_chat_room_user`
- ✅ ChatRooms: `idx_chat_rooms_type`, `idx_chat_rooms_created_at`
- ✅ Users: `idx_users_status`, `idx_users_email`, `idx_users_nickname`
- ✅ Friends: `idx_friends_user_id`, `idx_friends_friend_id`, `idx_friends_status`
- ✅ FriendRequests: `idx_friend_requests_sender_id`, `idx_friend_requests_receiver_id`, `idx_friend_requests_status`
- ✅ Reports: `idx_reports_status`, `idx_reports_reporter_id`, `idx_reports_reported_user_id`, `idx_reports_created_at`

**추가 검토 필요 인덱스**:
```sql
-- V4__add_optimization_indexes.sql (선택적)

-- 메시지 삭제 필터링 최적화 (부분 인덱스)
CREATE INDEX IF NOT EXISTS idx_messages_not_deleted
    ON messages(chat_room_id, created_at DESC) 
    WHERE is_deleted = false;

-- 친구 복합 조회 최적화 (이미 개별 인덱스 존재, 필요시 추가)
CREATE INDEX IF NOT EXISTS idx_friends_user_friend_status
    ON friends(user_id, friend_id, status);

-- 친구 요청 수신자+상태 복합 인덱스 (이미 개별 인덱스 존재)
CREATE INDEX IF NOT EXISTS idx_friend_requests_receiver_status
    ON friend_requests(receiver_id, status);
```

**참고**: 대부분의 필수 인덱스가 이미 추가되어 있습니다. 위 인덱스들은 실제 쿼리 성능 분석 후 필요시 추가하는 것을 권장합니다.

---

### 2.2 N+1 쿼리 잠재 위험 (우선순위: 중간)

**위험 영역**: `MessageJpaRepository.java`

```java
// 현재: 중첩 서브쿼리
@Query("SELECT m FROM Message m WHERE m.chatRoomId IN " +
       "(SELECT cm.chatRoomId FROM ChatRoomMember cm WHERE cm.userId = :userId) " +
       "AND m.deleted = false AND LOWER(m.content) LIKE LOWER(...)")
```

**개선안**: QueryDSL로 마이그레이션
```java
public List<Message> searchMessages(Long userId, String keyword) {
    return queryFactory
        .selectFrom(message)
        .join(chatRoomMember).on(message.chatRoomId.eq(chatRoomMember.chatRoomId))
        .where(
            chatRoomMember.userId.eq(userId),
            message.deleted.isFalse(),
            message.content.containsIgnoreCase(keyword)
        )
        .orderBy(message.createdAt.desc())
        .fetch();
}
```

---

### 2.3 Soft Delete 일관성 (우선순위: 중간)

**현재 상태**:
- `Message`: soft delete 구현 (`deleted`, `deletedAt` 필드)
- `User`: 상태 필드 사용 (`status: ACTIVE/INACTIVE/SUSPENDED`)
- 기타 엔티티: 물리 삭제

**개선안**: SoftDeletable 인터페이스
```java
public interface SoftDeletable {
    void softDelete();
    boolean isDeleted();
    LocalDateTime getDeletedAt();
}

@MappedSuperclass
public abstract class SoftDeletableEntity extends BaseEntity implements SoftDeletable {

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Override
    public void softDelete() {
        this.deleted = true;
        this.deletedAt = LocalDateTime.now();
    }
}
```

---

## 3. API 설계

### 3.1 인증 정보 전달 방식 불일치 (우선순위: 중간)

**현재 문제**:
```java
// 방식 1: @RequestParam으로 userId 전달
@PostMapping("/requests/{requestId}/accept")
public ResponseEntity<...> acceptFriendRequest(
    @PathVariable Long requestId,
    @RequestParam Long userId)  // 보안 위험: 조작 가능

// 방식 2: @RequestBody로 userId 전달
@PostMapping("/requests")
public ResponseEntity<...> sendFriendRequest(
    @RequestBody SendFriendRequestRequest request)  // request.requesterId()
```

**개선안**: SecurityContext에서 추출
```java
@PostMapping("/requests/{requestId}/accept")
public ResponseEntity<...> acceptFriendRequest(
    @PathVariable Long requestId,
    @AuthenticationPrincipal Long userId) {  // JWT에서 자동 추출
    // ...
}
```

**또는 유틸리티 메서드 사용**:
```java
@Component
public class SecurityUtils {
    public Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Long) auth.getPrincipal();
    }
}
```

---

### 3.2 응답 형식 불일치 (우선순위: 중간)

**현재 문제**:
```java
// 응답 1: 단순 객체
public record FriendListResponse(List<FriendDto> friends) { }

// 응답 2: 메시지 포함
public record SendFriendRequestResponse(Long requestId, String message) { }

// 응답 3: 단순 메시지
public record MessageResponse(String message) { }
```

**개선안**: 통합 응답 래퍼
```java
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
    boolean success,
    String message,
    T data,
    LocalDateTime timestamp
) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, null, data, LocalDateTime.now());
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, message, data, LocalDateTime.now());
    }

    public static ApiResponse<Void> successMessage(String message) {
        return new ApiResponse<>(true, message, null, LocalDateTime.now());
    }
}
```

**사용 예시**:
```java
@GetMapping
public ResponseEntity<ApiResponse<List<FriendDto>>> getFriends() {
    List<FriendDto> friends = friendService.getFriends(getCurrentUserId());
    return ResponseEntity.ok(ApiResponse.success(friends));
}

@PostMapping("/requests/{requestId}/accept")
public ResponseEntity<ApiResponse<Void>> acceptRequest(@PathVariable Long requestId) {
    friendService.acceptRequest(requestId, getCurrentUserId());
    return ResponseEntity.ok(ApiResponse.successMessage("친구 요청을 수락했습니다."));
}
```

---

## 4. 예외 처리

### 4.1 예외 클래스 구조 (현재: 양호)

**현재 구조**:
```
DomainException (기본 예외)
├── UserNotFoundException
├── ChatRoomNotFoundException
├── DuplicateEmailException
├── InvalidCredentialsException
├── FriendRequestNotFoundException
├── ChatRoomAccessDeniedException
└── ... (25개 예외)
```

### 4.2 에러 응답 개선 (우선순위: 낮음)

**현재**:
```java
public record ErrorResponse(
    String error,
    String code,
    LocalDateTime timestamp
) { }
```

**개선안**:
```java
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
    String error,
    String code,
    String path,
    LocalDateTime timestamp,
    List<FieldError> fieldErrors
) {
    public record FieldError(String field, String message) { }

    public static ErrorResponse of(String error, String code, String path) {
        return new ErrorResponse(error, code, path, LocalDateTime.now(), null);
    }

    public static ErrorResponse withFieldErrors(String error, String code,
            String path, List<FieldError> fieldErrors) {
        return new ErrorResponse(error, code, path, LocalDateTime.now(), fieldErrors);
    }
}
```

---

## 5. 테스트

### 5.1 테스트 커버리지 부족 (우선순위: 높음)

**현재 상태**: 366개 소스 vs 84개 테스트 (23%)

**부족한 영역**:

| 계층 | 현재 | 목표 |
|------|------|------|
| 도메인 엔티티 | 0% | 80% |
| 서비스 | ~20% | 80% |
| 컨트롤러 | ~50% | 70% |
| 리포지토리 | ~30% | 60% |

**추가 필요 테스트**:
```
test/java/com/cotalk/
├── domain/entity/
│   ├── UserTest.java
│   ├── MessageTest.java
│   └── FriendTest.java
├── application/service/
│   ├── friend/
│   │   ├── SendFriendRequestServiceTest.java
│   │   ├── AcceptFriendRequestServiceTest.java
│   │   └── ...
│   ├── chat/
│   │   ├── SendMessageServiceTest.java
│   │   └── ...
│   └── user/
│       ├── SignUpServiceTest.java
│       └── ...
└── adapter/outbound/persistence/
    ├── FriendRepositoryAdapterTest.java
    └── ...
```

---

### 5.2 테스트 픽스처 (우선순위: 중간)

**개선안**: Test Builder 패턴
```java
// test/java/com/cotalk/fixture/UserFixture.java
public class UserFixture {

    public static User.UserBuilder aUser() {
        return User.builder()
            .id(1L)
            .email("test@example.com")
            .nickname("테스트유저")
            .passwordHash("encodedPassword")
            .status(User.UserStatus.ACTIVE)
            .role(User.Role.USER);
    }

    public static User.UserBuilder anAdmin() {
        return aUser()
            .role(User.Role.ADMIN)
            .email("admin@example.com")
            .nickname("관리자");
    }
}

// 사용 예시
@Test
void should_return_user_when_valid_id() {
    User user = UserFixture.aUser().id(123L).build();
    // ...
}
```

---

## 6. 성능

### 6.1 캐싱 미적용 (우선순위: 중간)

**캐싱 적용 대상**:

| 대상 | 캐시 키 | TTL | 무효화 시점 |
|------|---------|-----|------------|
| 사용자 정보 | `user:{id}` | 1시간 | 프로필 수정 시 |
| 친구 목록 | `friends:{userId}` | 30분 | 친구 추가/삭제 시 |
| 채팅방 목록 | `chatrooms:{userId}` | 10분 | 채팅방 생성/나가기 시 |
| 알림 설정 | `notification:{userId}` | 1시간 | 설정 변경 시 |

**구현 예시**:
```java
@Service
public class FriendQueryService {

    @Cacheable(value = "friends", key = "#userId")
    public List<FriendDto> getFriendList(Long userId) {
        return friendRepository.findAcceptedFriends(userId)
            .stream()
            .map(FriendDto::from)
            .toList();
    }

    @CacheEvict(value = "friends", key = "#userId")
    public void evictFriendCache(Long userId) {
        // 친구 목록 변경 시 호출
    }
}
```

---

### 6.2 비동기 처리 필요 영역 (우선순위: 중간)

**대상 작업**:

| 작업 | 현재 | 개선 |
|------|------|------|
| 이메일 발송 | 동기 | `@Async` |
| 푸시 알림 | 동기 | `@Async` |
| 파일 업로드 후처리 | 동기 | `@Async` |
| 활동 로그 저장 | 동기 | `@Async` |

**구현 예시**:
```java
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean
    public Executor asyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-");
        executor.initialize();
        return executor;
    }
}

@Service
public class NotificationService {

    @Async
    public CompletableFuture<Void> sendPushNotification(Long userId, String message) {
        // 푸시 알림 발송 로직
        return CompletableFuture.completedFuture(null);
    }
}
```

---

## 7. 보안

### 7.1 민감 정보 관리 (우선순위: 높음)

**현재 문제**:
```yaml
# application.yml
jwt:
  secret: ${JWT_SECRET:your-secret-key-change-in-production-min-256-bits}
```
- 기본값이 노출되어 있음
- 프로덕션에서 변경하지 않으면 보안 취약

**개선안**:
```yaml
# application.yml (기본값 제거)
jwt:
  secret: ${JWT_SECRET}

# application-local.yml (개발용)
jwt:
  secret: local-dev-secret-key-for-development-only-256-bits
```

**추가 보안 조치**:
1. 환경변수 필수 설정 검증
```java
@Configuration
public class SecurityValidation {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @PostConstruct
    public void validateSecurityConfig() {
        if (jwtSecret == null || jwtSecret.contains("change-in-production")) {
            throw new IllegalStateException("JWT_SECRET must be configured in production!");
        }
    }
}
```

---

## 8. 코드 품질

### 8.1 DTO 변환 중복 (우선순위: 중간)

**현재 패턴** (68개 DTO에서 반복):
```java
public record FriendDto(Long id, String nickname, String email, String avatarUrl) {
    public static FriendDto from(User user) {
        return new FriendDto(
            user.getId(),
            user.getNickname(),
            user.getEmail(),
            user.getAvatarUrl()
        );
    }
}
```

**개선안 1**: MapStruct 도입
```java
@Mapper(componentModel = "spring")
public interface UserMapper {

    FriendDto toFriendDto(User user);

    UserDto toUserDto(User user);

    List<FriendDto> toFriendDtoList(List<User> users);
}
```

**개선안 2**: 통합 변환 서비스
```java
@Component
public class DtoConverter {

    public FriendDto toFriendDto(User user) {
        return new FriendDto(
            user.getId(),
            user.getNickname(),
            user.getEmail(),
            user.getAvatarUrl()
        );
    }

    public List<FriendDto> toFriendDtoList(List<User> users) {
        return users.stream()
            .map(this::toFriendDto)
            .toList();
    }
}
```

---

## 리팩토링 우선순위 매트릭스

### 즉시 수행 (1주 이내)

| 작업 | 예상 시간 | 영향도 | 상태 |
|------|----------|--------|------|
| BaseEntity 생성 및 적용 | 4시간 | 높음 | ✅ 부분 완료 (5/14) |
| 인덱스 마이그레이션 추가 | 1시간 | 높음 | ✅ 완료 |
| JWT 시크릿 기본값 제거 | 30분 | 높음 | ⏳ 미완료 |
| 나머지 엔티티 BaseEntity 적용 | 3시간 | 높음 | ⏳ 미완료 (9개 남음) |

### 단기 (2주 이내)

| 작업 | 예상 시간 | 영향도 |
|------|----------|--------|
| 서비스 계층 테스트 추가 | 3일 | 높음 |
| API 응답 형식 통합 | 1일 | 중간 |
| 테스트 픽스처 구성 | 4시간 | 중간 |

### 중기 (1개월 이내)

| 작업 | 예상 시간 | 영향도 |
|------|----------|--------|
| 캐싱 전략 적용 | 2일 | 중간 |
| 비동기 처리 적용 | 2일 | 중간 |
| DTO 변환 로직 통합 | 1일 | 낮음 |
| QueryDSL 마이그레이션 | 3일 | 중간 |

### 장기 (분기 내)

| 작업 | 예상 시간 | 영향도 |
|------|----------|--------|
| UseCase 인터페이스 리팩토링 | 1주 | 낮음 |
| Soft Delete 일관성 | 3일 | 낮음 |
| Repository 어댑터 단순화 | 1주 | 낮음 |

---

## 체크리스트

### 코드 품질
- [x] BaseEntity 생성 및 JPA Auditing 설정 ✅
- [ ] 나머지 엔티티 BaseEntity 적용 (9개 남음)
- [ ] DTO 변환 로직 통합 (MapStruct 또는 Converter)
- [ ] 테스트 커버리지 60% 이상 달성
- [ ] 테스트 픽스처 구성

### 성능
- [x] 인덱스 마이그레이션 적용 ✅
- [ ] 캐싱 전략 적용
- [ ] N+1 쿼리 검증 및 수정
- [ ] 비동기 처리 적용

### API
- [ ] 응답 형식 통합
- [ ] SecurityContext 기반 인증 정보 추출
- [ ] API 문서 업데이트

### 보안
- [ ] JWT 시크릿 기본값 제거 및 환경변수 필수화
- [ ] Rate Limiting 검증 (설정은 완료, 검증 필요)
- [ ] 입력 검증 강화

---

## 참고 자료

- [Spring Data JPA Auditing](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#auditing)
- [MapStruct Documentation](https://mapstruct.org/documentation/stable/reference/html/)
- [QueryDSL Reference](http://querydsl.com/static/querydsl/latest/reference/html/)
- [Spring Cache Abstraction](https://docs.spring.io/spring-framework/docs/current/reference/html/integration.html#cache)
