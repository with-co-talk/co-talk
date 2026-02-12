# Co-Talk 미구현 기능 및 작업계획서

> 작성일: 2026-02-12
> 현재 상태: v1.0 출시 준비 완료 (테스트 2,765개 전체 통과)

---

## 1. 미구현/부분구현 기능 목록

### 1.1 부분 구현 (백엔드 완료, Flutter 미연결)

| # | 기능 | 백엔드 | Flutter | 설명 |
|---|------|:------:|:-------:|------|
| B-1 | 답장/인용 (Reply) | ✅ | 🔸 데이터모델만 | `POST /api/v1/chat/messages/{id}/reply` 완료. Flutter에 `Message.replyToMessageId` 필드 있으나 BLoC 이벤트·UI 없음 |
| B-2 | 메시지 전달 (Forward) | ✅ | 🔸 데이터모델만 | `POST /api/v1/chat/messages/{id}/forward` 완료. Flutter에 `Message.forwardedFromMessageId` 필드 있으나 BLoC 이벤트·UI 없음 |
| B-3 | 신고하기 | ✅ | ❌ | `POST /api/v1/reports/users`, `POST /api/v1/reports/messages` 완료. Flutter 페이지·BLoC 없음 |

### 1.2 부분 구현 (Flutter 완료, 백엔드 미연결)

| # | 기능 | 백엔드 | Flutter | 설명 |
|---|------|:------:|:-------:|------|
| F-1 | 비밀번호 변경 | 🔸 reset만 | ✅ UI 완료 | `ChangePasswordPage` UI·폼 검증 완료. 현재 비밀번호 확인 후 변경하는 API 엔드포인트 없음 (비밀번호 초기화만 존재) |
| F-2 | 계정 삭제 | ❌ | ✅ UI+BLoC 완료 | `AccountDeletionPage`·`AccountDeletionBloc` 완료. 백엔드 `User.deactivate()` 메서드 있으나 REST 엔드포인트 없음 |

### 1.3 양쪽 미구현

| # | 기능 | 우선순위 | 설명 |
|---|------|:--------:|------|
| N-1 | 소셜 로그인 (카카오/구글/애플) | P1 | 백엔드 `User.OAuthProvider` enum만 존재, 실제 OAuth 플로우 미구현 |
| N-2 | 비디오 재생 | P1 | 비디오 파일 전송은 가능하나 앱 내 재생 불가. `video_player` 패키지 없음 |
| N-3 | 딥링크 | P2 | 알림 탭 → 앱 내 이동은 되지만, 외부 URL → 특정 채팅방 진입 미지원 |
| N-4 | 이메일 인증 | P2 | 회원가입 시 이메일 검증 없이 바로 계정 생성됨 |
| N-5 | 생체 인증 | P3 | 앱 잠금(Face ID/지문) 미지원 |
| N-6 | 그룹 채팅방 이미지 설정 | P3 | 1:1은 상대 아바타 사용, 그룹은 커스텀 이미지 설정 불가 |
| N-7 | 채팅방 검색 | P3 | 채팅 목록에서 방 이름/참여자로 검색 불가 |
| N-8 | 다국어 지원 | P3 | 설정에 언어 항목 있으나 한국어 고정 |
| N-9 | 관리자 페이지 | P3 | 백엔드 ADMIN 역할·신고 처리 모델 있으나 관리자 엔드포인트 없음 |

---

## 2. 작업계획서

### Phase 1: v1.1 — 핵심 사용성 (예상 1~2주)

출시 직후 사용자 경험에 직접적으로 영향을 미치는 기능들.

#### Task 1-1: 답장/인용 (Reply) — Flutter 연결
- **범위**: Flutter만
- **작업 내용**:
  - `ChatRoomEvent`에 `ReplyToMessageSelected(Message)`, `ReplyCancelled` 이벤트 추가
  - `ChatRoomState`에 `replyToMessage` 필드 추가
  - `ChatRoomBloc`에서 메시지 전송 시 `replyToMessageId` 포함
  - 메시지 입력창 상단에 인용 미리보기 위젯 추가
  - 메시지 버블에 인용된 원본 메시지 표시 위젯 추가
  - 메시지 롱프레스 → 답장 메뉴 추가
- **테스트**: BLoC 단위 테스트 + 위젯 테스트
- **의존성**: 없음 (백엔드 완료)

#### Task 1-2: 메시지 전달 (Forward) — Flutter 연결
- **범위**: Flutter만
- **작업 내용**:
  - 메시지 롱프레스 → 전달 메뉴 추가
  - 채팅방 선택 다이얼로그 (친구 목록 / 채팅방 목록)
  - `ChatRepository.forwardMessage(messageId, targetRoomId)` 호출
  - 전달된 메시지에 "전달됨" 표시
- **테스트**: Repository mock 단위 테스트 + 위젯 테스트
- **의존성**: 없음 (백엔드 완료)

#### Task 1-3: 신고하기 — Flutter 연결
- **범위**: Flutter만
- **작업 내용**:
  - `ReportBloc` / `ReportEvent` / `ReportState` 생성
  - `ReportPage` — 신고 유형 선택 + 사유 입력 폼
  - 프로필 페이지에 "신고하기" 버튼 추가
  - 메시지 롱프레스 → "메시지 신고" 메뉴 추가
  - `ReportRepository` + `ReportRemoteDataSource` 구현
- **테스트**: BLoC 단위 테스트
- **의존성**: 없음 (백엔드 완료)

#### Task 1-4: 비디오 재생
- **범위**: Flutter만
- **작업 내용**:
  - `video_player` (또는 `chewie`) 패키지 추가
  - 비디오 메시지 버블에 재생 버튼 오버레이
  - 전체화면 비디오 플레이어 페이지
  - 미디어 갤러리에서 비디오 탭 재생 지원
- **테스트**: 위젯 테스트 (모킹)
- **의존성**: 없음

---

### Phase 2: v1.2 — 계정 관리 (예상 1주)

계정 보안과 관리 기능.

#### Task 2-1: 비밀번호 변경 — 백엔드 API 연결
- **범위**: 백엔드 + Flutter
- **작업 내용**:
  - **백엔드**: `PasswordController`에 `PUT /api/v1/password/change` 추가
    - Request: `{ currentPassword, newPassword }`
    - 현재 비밀번호 BCrypt 검증 → 새 비밀번호 해싱 저장
    - `ChangePasswordUseCase` 인터페이스 + `ChangePasswordService` 구현
  - **Flutter**: `ChangePasswordPage._handleChangePassword()`에서 API 호출 연결
- **테스트**: 백엔드 단위 + 통합 테스트, Flutter BLoC 테스트
- **의존성**: 없음

#### Task 2-2: 계정 삭제 — 백엔드 엔드포인트 추가
- **범위**: 백엔드만
- **작업 내용**:
  - `UserController`에 `DELETE /api/v1/users/me` 추가
  - 비밀번호 재확인 필수
  - `DeactivateAccountUseCase` 구현:
    - 모든 채팅방 퇴장
    - 친구 관계 정리
    - RefreshToken 전체 폐기
    - DeviceToken 비활성화
    - User 상태 INACTIVE 변경
  - 30일 유예 후 데이터 완전 삭제 (선택적)
- **테스트**: 통합 테스트 (트랜잭션 검증)
- **의존성**: 없음 (Flutter UI 완료)

#### Task 2-3: 이메일 인증
- **범위**: 백엔드 + Flutter
- **작업 내용**:
  - **백엔드**:
    - `EmailVerificationToken` 엔티티 (token, userId, expiresAt)
    - 회원가입 시 인증 이메일 발송 (기존 SMTP 인프라 활용)
    - `GET /api/v1/auth/verify-email?token=xxx` 엔드포인트
    - `User.emailVerified` 필드 추가
  - **Flutter**:
    - 회원가입 후 "이메일을 확인해주세요" 안내 페이지
    - 인증 완료 전 로그인 차단 또는 제한 기능
- **테스트**: 이메일 발송 모킹 단위 테스트
- **의존성**: 없음

---

### Phase 3: v1.3 — 소셜 로그인 (예상 2주)

사용자 유입 확대를 위한 핵심 기능.

#### Task 3-1: 카카오 로그인
- **범위**: 백엔드 + Flutter
- **작업 내용**:
  - **백엔드**:
    - `OAuthController` 생성
    - `POST /api/v1/auth/oauth/kakao` — 카카오 액세스 토큰 검증 → JWT 발급
    - `OAuthLoginUseCase` — 기존 계정 연동 또는 신규 생성
  - **Flutter**:
    - `kakao_flutter_sdk` 패키지 추가
    - 로그인 페이지에 카카오 로그인 버튼
    - OAuth 토큰 → 백엔드 전달 → JWT 수신
- **테스트**: OAuth 토큰 검증 모킹 테스트
- **의존성**: 카카오 개발자 앱 등록 필요

#### Task 3-2: 구글 로그인
- **범위**: 백엔드 + Flutter
- **작업 내용**:
  - **백엔드**: `POST /api/v1/auth/oauth/google` — Google ID 토큰 검증
  - **Flutter**: `google_sign_in` 패키지, 로그인 버튼 추가
- **테스트**: Google ID 토큰 검증 모킹 테스트
- **의존성**: Google Cloud Console 설정 필요

#### Task 3-3: Apple 로그인
- **범위**: 백엔드 + Flutter
- **작업 내용**:
  - **백엔드**: `POST /api/v1/auth/oauth/apple` — Apple ID 토큰 검증
  - **Flutter**: `sign_in_with_apple` 패키지, iOS 로그인 버튼 (App Store 필수 요구)
- **테스트**: Apple ID 토큰 검증 모킹 테스트
- **의존성**: Apple Developer 설정 필요

---

### Phase 4: v1.4 — 편의 기능 (예상 1~2주)

사용성 향상을 위한 기능.

#### Task 4-1: 딥링크
- **범위**: Flutter + 백엔드 (선택)
- **작업 내용**:
  - `app_links` (Android) + Universal Links (iOS) 설정
  - URL 스킴: `cotalk://chat/{roomId}`, `cotalk://profile/{userId}`
  - `GoRouter`에 딥링크 리다이렉트 핸들러 추가
  - 초대 링크 생성: `https://cotalk.app/invite/{code}`
- **테스트**: 라우팅 단위 테스트
- **의존성**: 도메인 설정 필요 (Universal Links)

#### Task 4-2: 생체 인증 (앱 잠금)
- **범위**: Flutter만
- **작업 내용**:
  - `local_auth` 패키지 추가
  - 설정 → "앱 잠금" 토글
  - 앱 포그라운드 복귀 시 인증 요청
  - `flutter_secure_storage`로 잠금 설정 저장
- **테스트**: 모킹 단위 테스트
- **의존성**: 없음

#### Task 4-3: 그룹 채팅방 이미지
- **범위**: 백엔드 + Flutter
- **작업 내용**:
  - **백엔드**: `ChatRoom.imageUrl` 필드 활용, `PUT /api/v1/chat/rooms/{id}/image` 엔드포인트
  - **Flutter**: 그룹 설정 페이지에서 이미지 업로드 → URL 설정
- **테스트**: 단위 테스트
- **의존성**: 없음

#### Task 4-4: 채팅방 검색
- **범위**: Flutter만
- **작업 내용**:
  - 채팅 목록 상단에 검색바 추가
  - `ChatListState`에 `searchQuery` 필드 추가
  - 로컬 필터링 (방 이름, 참여자 닉네임)
- **테스트**: BLoC 단위 테스트
- **의존성**: 없음

---

### Phase 5: v2.0 — 확장 (장기)

| 기능 | 설명 | 예상 기간 |
|------|------|-----------|
| 다국어 지원 (i18n) | `flutter_localizations` + ARB 파일, 영어 우선 | 1주 |
| 관리자 웹 대시보드 | 신고 처리, 사용자 관리, 통계 | 3~4주 |
| E2E 암호화 | Signal Protocol 기반, 키 교환 + 메시지 암복호화 | 4~6주 |
| 음성/영상 통화 | WebRTC 기반, TURN 서버 필요 | 6~8주 |

---

## 3. 우선순위 요약

```
v1.1 (1~2주)  답장 · 전달 · 신고 · 비디오 재생
     ↓
v1.2 (1주)    비밀번호 변경 · 계정 삭제 · 이메일 인증
     ↓
v1.3 (2주)    카카오 · 구글 · 애플 로그인
     ↓
v1.4 (1~2주)  딥링크 · 생체 인증 · 그룹 이미지 · 방 검색
     ↓
v2.0 (장기)   다국어 · 관리자 · E2E · 음성통화
```

---

## 4. 기술 부채 참고사항

| 항목 | 현재 상태 | 비고 |
|------|-----------|------|
| 알림 테스트 부재 | FCM/Notification 서비스에 단위 테스트 없음 | 출시 후 안정화 단계에서 추가 권장 |
| Flutter 테스트 4개 skip | `chat_room_page_test.dart` 1개 등 | 기능에 영향 없음 |
| OAuth 모델만 존재 | `User.OAuthProvider` enum 사용 안 됨 | Phase 3에서 활성화 |
| 관리자 엔드포인트 부재 | `ADMIN` 역할과 Report 처리 모델 준비됨 | Phase 5에서 구현 |
