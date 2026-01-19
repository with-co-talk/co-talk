## 📋 개요

예외 처리 개선 및 보안 강화를 위한 리팩토링 작업입니다. 도메인 예외 체계를 구축하고, 인프라스트럭처 레이어의 예외 처리를 개선하며, 파일 업로드 보안을 강화하고, Controller 레벨의 접근 제어를 강화했습니다.

## 🎯 변경사항

### 1. 도메인 예외 추가
- `FileStorageException`: 파일 저장소 관련 예외 처리
  - 파일 삭제 실패, 파일 조회 실패 시나리오 지원
- `MessageBrokerException`: 메시지 브로커 관련 예외 처리
  - 메시지 직렬화 실패, 리액션 이벤트 직렬화 실패, 메시지 발행 실패 시나리오 지원
- 정적 팩토리 메서드를 통한 예외 생성 패턴 적용

### 2. 인프라스트럭처 예외 처리 개선
- `MinioFileStorage`: FileStorageException 사용으로 예외 처리 일관성 향상
- `RedisChatMessageBroker`: MessageBrokerException 사용으로 예외 처리 일관성 향상
- 일반 예외를 도메인 특화 예외로 교체하여 에러 메시지 및 컨텍스트 개선

### 3. 파일 업로드 보안 강화
- 매직넘버 검증 추가로 파일 확장자 위조 공격 방지
- JPEG, PNG, GIF, WebP, PDF 형식 지원
- `FileUploadException`에 추가 팩토리 메서드 제공
- BufferedInputStream을 사용한 스트림 처리 개선

### 4. Controller 보안 강화
- `validateUserAccess` 메서드 추가로 사용자 접근 검증
- `ChatRoomController`, `FriendController`, `MessageSearchController`, `UserController`에 적용
- 인가되지 않은 리소스 접근 방지

### 5. 서비스 로직 개선
- 채팅방 서비스 메서드 개선 (CreateGroupChatRoomService, GetChatRoomsService, InviteGroupChatMemberService)
- 사용자 계정 삭제 서비스 개선 (DeleteAccountService)
- Repository 메서드 확장 (ChatRoomMemberRepository)

## 📊 통계

- **변경된 파일**: 16개
- **추가된 줄**: 327줄
- **삭제된 줄**: 36줄
- **커밋 수**: 6개

## 🔗 관련 이슈

Closes #[이슈번호]

## ✅ 체크리스트

- [x] 코드 리뷰 준비 완료
- [ ] 테스트 통과
- [ ] 문서 업데이트 (필요시)
- [ ] CHANGELOG 업데이트 (필요시)

## 📝 테스트 방법

1. 파일 업로드 시 잘못된 확장자의 파일이 거부되는지 확인
2. 매직넘버 검증이 올바르게 동작하는지 확인
3. Controller에서 다른 사용자의 리소스에 접근 시도 시 401/403 에러가 발생하는지 확인
4. 파일 저장소 및 메시지 브로커 예외가 올바르게 처리되는지 확인

## 🔍 리뷰 포인트

1. **예외 처리 패턴**: 도메인 예외 사용이 적절한지 확인
2. **보안 강화**: 매직넘버 검증 및 사용자 접근 검증 로직 검토
3. **코드 일관성**: 예외 처리 방식의 일관성 확인
4. **성능 영향**: 매직넘버 검증이 파일 업로드 성능에 미치는 영향 검토
