<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-02-23 | Updated: 2026-02-23 -->

# adapter - 어댑터 계층

## 개요
외부 인터페이스와 도메인을 연결하는 어댑터. Inbound(REST, WebSocket)와 Outbound(JPA, 파일) 분리.

## 디렉토리
| 디렉토리 | 용도 |
|-----------|------|
| `inbound/rest/` | REST 컨트롤러 22개. `/api/v1/` prefix |
| `inbound/rest/dto/` | 요청/응답 DTO (도메인별 하위 패키지) |
| `inbound/websocket/` | WebSocket STOMP 컨트롤러 |
| `inbound/websocket/dto/` | WebSocket DTO |
| `outbound/persistence/` | JPA Repository 어댑터 (도메인별 하위 패키지) |
| `outbound/persistence/entity/` | JPA 전용 엔티티 (UserJpaEntity 등) |
| `outbound/persistence/mapper/` | 도메인 ↔ JPA 엔티티 변환 매퍼 |
| `outbound/persistence/file/` | 파일 관련 영속성 |

## 주요 파일

### Inbound (REST 컨트롤러)
| 파일 | 엔드포인트 | 설명 |
|------|-----------|------|
| `rest/AuthController.java` | `/api/v1/auth/**` | 회원가입, 로그인, 토큰갱신, 로그아웃 |
| `rest/ChatMessageController.java` | `/api/v1/chat/messages/**` | 메시지 CRUD, 답장/전달, 미디어갤러리 |
| `rest/ChatRoomController.java` | `/api/v1/chatrooms/**` | 1:1 채팅방 생성/조회 |
| `rest/GroupChatRoomController.java` | `/api/v1/group-chatrooms/**` | 그룹 채팅방 관리 |
| `rest/FriendController.java` | `/api/v1/friends/**` | 친구 요청/수락/거절/삭제 |
| `rest/BlockController.java` | `/api/v1/blocks/**` | 사용자 차단/해제 |
| `rest/UserController.java` | `/api/v1/users/**` | 사용자 검색, 프로필 수정 |
| `rest/AdminController.java` | `/api/v1/admin/**` | 관리자 전용 (ADMIN 롤 필요) |
| `rest/FileController.java` | `/api/v1/files/**` | 파일 업로드 |

### Outbound (영속성 어댑터)
| 패키지 | 설명 |
|---------|------|
| `persistence/user/` | UserJpaRepository + UserRepositoryAdapter (Redis 캐시 포함) |
| `persistence/chatroom/` | ChatRoom/ChatRoomMember JPA 어댑터 |
| `persistence/message/` | Message JPA 어댑터 |
| `persistence/friend/` | Friend/FriendRequest/Block/HiddenFriend JPA 어댑터 |
| `persistence/auth/` | EmailVerificationToken JPA 어댑터 |
| `persistence/refreshtoken/` | RefreshToken JPA 어댑터 |
| `persistence/notification/` | DeviceToken/NotificationSetting JPA 어댑터 |
| `persistence/report/` | Report JPA 어댑터 |
| `persistence/profile/` | ProfileHistory JPA 어댑터 |
| `persistence/entity/` | JPA 전용 엔티티 (UserJpaEntity - 도메인과 완전 분리) |
| `persistence/mapper/` | UserMapper (도메인 User ↔ UserJpaEntity 변환) |

## AI 에이전트 가이드

### 컨트롤러 작성 규칙
- `domain/port/inbound`의 UseCase 인터페이스만 의존 (아웃바운드 포트 직접 접근 금지)
- `@AuthenticationPrincipal CustomUserPrincipal`로 인증 사용자 정보 획득
- DTO는 Java record 사용, 요청/응답 분리
- Swagger `@Operation`, `@ApiResponse` 문서화 필수
- REST API prefix: `/api/v1/`

### 영속성 어댑터 패턴 (3파일)
```
XXXJpaRepository     — Spring Data JPA 인터페이스
XXXRepositoryAdapter — 도메인 포트 구현 (JPA ↔ 도메인 변환)
XXXMapper           — 도메인 ↔ JPA 엔티티 양방향 변환 (분리된 엔티티만)
```

### 테스트 패턴
- 컨트롤러: `@WebMvcTest(addFilters=false)` + `@MockitoBean` + `RateLimitTestConfiguration`
- 영속성: `@DataJpaTest` + `@Import({Adapter.class, Mapper.class, JpaAuditingConfig.class})`

### inbound → outbound 직접 의존 금지
- ArchUnit으로 자동 검증됨

<!-- MANUAL: -->
