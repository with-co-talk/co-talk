<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-02-23 | Updated: 2026-02-23 -->

# domain - 도메인 계층

## 개요
비즈니스 핵심 로직. 외부 프레임워크(Spring, JPA 등)에 의존하지 않는 순수 도메인 모델.

## 디렉토리
| 디렉토리 | 용도 |
|-----------|------|
| `entity/` | 도메인 엔티티 (User, Message, ChatRoom, Friend 등) |
| `model/` | 값 객체 (Email, GroupedReaction, HiddenFriendInfo) |
| `port/inbound/` | UseCase 인터페이스 (~50개). 1 UseCase = 1 인터페이스 원칙 |
| `port/outbound/` | Repository/외부서비스 포트 인터페이스 (~30개) |
| `exception/` | 도메인 예외 (DomainException + 34개 구체 예외) |
| `validator/` | 도메인 검증기 (UserValidator, MessageValidator 등) |
| `validation/` | Bean Validation 커스텀 어노테이션 |
| `service/` | 도메인 서비스 |
| `constants/` | 도메인 상수 |
| `converter/` | 도메인 값 변환기 |
| `util/` | 도메인 유틸 (HtmlSanitizer 등) |

## 주요 파일

### 엔티티
| 파일 | 설명 |
|------|------|
| `entity/User.java` | 사용자 엔티티. `DomainBaseEntity` 상속(JPA 없음), `Email` 값 객체, 상태 전이 메서드 |
| `entity/Message.java` | 메시지 엔티티. 암호화(`EncryptedStringConverter`), 소프트 삭제, 링크 미리보기, 답장/전달 |
| `entity/ChatRoom.java` | 채팅방 엔티티. DIRECT/GROUP/SELF 3타입, 이름/공지 유효성 규칙 내장 |
| `entity/ChatRoomMember.java` | 채팅방 멤버십. 권한(OWNER/ADMIN/MEMBER), 읽음 위치 추적 |
| `entity/Friend.java` | 양방향 친구 관계 |
| `entity/BaseEntity.java` | JPA 매핑 포함 공통 베이스 (미분리 엔티티용, 향후 제거 예정) |
| `entity/DomainBaseEntity.java` | 순수 도메인 베이스 (JPA 없음). 완전 분리된 엔티티용 |

### 포트
| 파일 | 설명 |
|------|------|
| `port/outbound/UserRepository.java` | 사용자 저장소 포트 (14개 메서드) |
| `port/outbound/IdGenerator.java` | Snowflake ID 생성 포트 |
| `port/outbound/TimeProvider.java` | 시간 제공 포트 (테스트 대체 용이) |
| `port/outbound/PasswordEncoderPort.java` | 비밀번호 인코딩 포트 |
| `port/outbound/AuthTokenPort.java` | JWT 토큰 생성/검증 포트 |
| `port/outbound/ChatMessageBroker.java` | Redis Pub/Sub 채팅 브로커 포트 |
| `port/outbound/DistributedLockPort.java` | 분산락 포트 |
| `port/outbound/EncryptionPort.java` | 암호화 포트 |
| `port/outbound/FileStorage.java` | 파일 저장 포트 |
| `port/outbound/EmailSender.java` | 이메일 발송 포트 |
| `port/outbound/PushNotificationSender.java` | FCM 푸시 알림 포트 |

### 예외
| 파일 | 설명 |
|------|------|
| `exception/DomainException.java` | 기반 예외. `errorCode` + `HttpStatusHint` 보유 |
| `exception/HttpStatusHint.java` | HTTP 상태 힌트 enum (도메인이 HTTP에 직접 의존하지 않음) |

## AI 에이전트 가이드

### 작업 시 주의사항
- 이 패키지는 **순수 도메인**. Spring, JPA 어노테이션 사용 금지 (Jakarta Validation은 허용)
- 예외 생성 시 반드시 `HttpStatusHint` 지정 (GlobalExceptionHandler가 HTTP 상태 결정에 사용)
- 새 엔티티는 `DomainBaseEntity` 상속 권장 (JPA 분리 패턴)
- 값 객체는 Java record 사용

### 테스트
- `src/test/java/com/cotalk/domain/` 에 대응 테스트 존재
- 엔티티 비즈니스 로직, 예외, 검증기 모두 단위 테스트 필수

### 인바운드 포트 도메인별 분류
- `auth/`: 회원가입, 로그인, OAuth, 이메일인증, 비밀번호 (10개)
- `message/`: 메시지 CRUD, 검색, 읽음, 답장/전달, 리액션, 미디어갤러리 (10개)
- `chatroom/`: 채팅방 생성/관리, 멤버 관리, 초대 (10개)
- `friend/`: 친구요청, 차단, 숨김, 목록 조회 (10개)
- `chat/`: 실시간 브로드캐스트, 타이핑, 접속상태 (5개)
- `notification/`, `profile/`, `user/`, `report/`, `admin/`, `file/`, `linkpreview/`

<!-- MANUAL: -->
