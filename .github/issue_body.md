## 📋 개요

예외 처리 개선 및 보안 강화를 위한 리팩토링 작업입니다.

## 🎯 목표

1. 도메인 예외 체계 구축
2. 인프라스트럭처 레이어 예외 처리 개선
3. 파일 업로드 보안 강화
4. Controller 레벨 접근 제어 강화
5. 서비스 로직 개선

## 📝 주요 변경사항

### 1. 도메인 예외 추가
- `FileStorageException`: 파일 저장소 관련 예외 처리
- `MessageBrokerException`: 메시지 브로커 관련 예외 처리
- 정적 팩토리 메서드를 통한 예외 생성 패턴 적용

### 2. 인프라스트럭처 예외 처리 개선
- `MinioFileStorage`: FileStorageException 사용
- `RedisChatMessageBroker`: MessageBrokerException 사용
- 일반 예외를 도메인 특화 예외로 교체

### 3. 파일 업로드 보안 강화
- 매직넘버 검증 추가로 파일 확장자 위조 공격 방지
- JPEG, PNG, GIF, WebP, PDF 형식 지원
- `FileUploadException` 개선

### 4. Controller 보안 강화
- 사용자 접근 검증 메서드 추가
- `ChatRoomController`, `FriendController`, `MessageSearchController`, `UserController`에 적용
- 인가되지 않은 리소스 접근 방지

### 5. 서비스 로직 개선
- 채팅방 서비스 메서드 개선
- 사용자 계정 삭제 서비스 개선
- Repository 메서드 확장

## ✅ 체크리스트

- [x] 도메인 예외 클래스 추가
- [x] 인프라스트럭처 예외 처리 개선
- [x] 파일 업로드 보안 강화
- [x] Controller 보안 강화
- [x] 서비스 로직 개선
- [ ] 테스트 코드 작성
- [ ] 문서 업데이트
