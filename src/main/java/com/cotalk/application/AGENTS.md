<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-02-23 | Updated: 2026-02-23 -->

# application - 애플리케이션 계층

## 개요
도메인 포트(UseCase)의 구현체. 비즈니스 오케스트레이션 담당. ~65개 서비스 클래스.

## 디렉토리
| 디렉토리 | 용도 |
|-----------|------|
| `service/auth/` | 인증 서비스 (회원가입, 로그인, OAuth, 이메일인증, 비밀번호) |
| `service/message/` | 메시지 서비스 (CRUD, 검색, 브로드캐스트, 링크 미리보기) |
| `service/chatroom/` | 채팅방 서비스 (생성, 관리, 멤버, 초대) |
| `service/chat/` | 실시간 채팅 서비스 (브로드캐스트, 타이핑, 접속상태) |
| `service/friend/` | 친구 서비스 (요청, 수락, 차단, 숨김) |
| `service/notification/` | 알림 서비스 (디바이스 토큰, 푸시, 설정) |
| `service/profile/` | 프로필 이력 서비스 |
| `service/user/` | 사용자 서비스 (검색, 프로필 수정, 탈퇴) |
| `service/report/` | 신고 서비스 |
| `service/admin/` | 관리자 서비스 |
| `service/linkpreview/` | 링크 미리보기 서비스 |

## 주요 파일
| 파일 | 설명 |
|------|------|
| `service/auth/SignUpService.java` | 회원가입. 이메일 중복검사 → 비밀번호 해싱 → User 저장 → 이메일인증 토큰 발송 |
| `service/auth/LoginService.java` | 로그인. 자격 검증 → JWT 발급 → RefreshToken 저장 |
| `service/message/SendMessageService.java` | 메시지 전송. TransactionTemplate으로 DB만 트랜잭션, Redis/FCM은 트랜잭션 외부 |
| `service/chatroom/CreateChatRoomService.java` | 1:1 채팅방 생성. 분산락 사용 |
| `service/chatroom/ChatRoomSummaryAssembler.java` | 채팅방 목록 조합. 안읽은 메시지 수, 최근 메시지 포함 |

## AI 에이전트 가이드

### 작업 시 주의사항
- 이 계층은 `domain` 패키지만 의존 가능 (adapter, infrastructure 직접 참조 금지)
- 모든 외부 의존은 아웃바운드 포트(인터페이스)를 통해 접근
- **TDD 필수**: 테스트 먼저 작성 후 구현
- 서비스 클래스는 1-2개 UseCase만 구현 (단일 책임)
- 생성자 주입, `@Service` 어노테이션 사용

### 성능 패턴
- `TransactionTemplate`: DB 작업만 트랜잭션으로 감싸고, Redis/FCM/외부 호출은 트랜잭션 밖에서 실행 → DB 커넥션 점유 최소화
- 중복 쿼리 방지: sender + members를 한 번만 Pre-fetch하여 재사용
- 이메일 마스킹 로그: `te**@example.com`

### 테스트 패턴
```java
@ExtendWith(MockitoExtension.class)
class XxxServiceTest {
    @Mock private SomeRepository repository;
    @InjectMocks private XxxService service;

    @Test
    void should_예상결과_when_조건() { ... }
}
```

<!-- MANUAL: -->
