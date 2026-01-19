## 📋 개요
보안 강화, 코드 품질 개선, 출시 전 준비 작업을 포함한 종합적인 개선 작업

## 🎯 변경사항

### 보안 강화
- ✅ PasswordValidator 어노테이션 추가: 8-128자, 대문자/소문자/숫자/특수문자 각 1개 이상 요구
- ✅ HtmlSanitizer 유틸리티 추가: XSS 공격 방지를 위한 HTML 이스케이프 처리
- ✅ SignUpRequest에 비밀번호 검증 및 닉네임 길이 검증 추가
- ✅ RefreshTokenService 로깅 레벨 조정 및 개인정보 제거 (userId 제거)

### JWT 토큰 관리 개선
- ✅ Access 토큰과 Refresh 토큰을 구분하기 위한 token_type claim 추가
- ✅ generateRefreshToken() 메서드 추가
- ✅ getTokenType(), isAccessToken() 메서드 추가
- ✅ 토큰 타입 기반 검증 및 관리 강화

### 코드 품질 개선
- ✅ 파일 업로드 및 예외 처리 개선
- ✅ 분산 락 Executor 개선 및 NoOp 구현 추가 (테스트 환경용)
- ✅ GlobalExceptionHandler 예외 처리 개선

### 출시 전 준비
- ✅ k8s/base/secret.yaml Git 추적 제거 (보안)
- ✅ RELEASE_PRE_CHECKLIST.md 추가: 환경변수 설정, 프로파일 활성화, Secret 관리 가이드

### 테스트
- ✅ 보안 및 기능 변경에 따른 테스트 코드 업데이트 (11개 파일)

## 🔗 관련 이슈
Closes #10

## ✅ 체크리스트
- [x] 코드 리뷰 준비 완료
- [x] 테스트 통과
- [x] 문서 업데이트 (RELEASE_PRE_CHECKLIST.md)
- [x] 보안 검토 완료

## 📝 테스트 방법
1. 회원가입 시 비밀번호 강도 검증 확인
   - 8자 미만, 특수문자 없음 등 약한 비밀번호 입력 시 검증 실패 확인
2. JWT 토큰 타입 구분 확인
   - Access 토큰과 Refresh 토큰의 token_type claim 확인
3. 로깅 레벨 확인
   - RefreshTokenService의 로그가 debug 레벨로 출력되는지 확인
   - 로그에 userId가 포함되지 않는지 확인

## 🔍 리뷰 포인트
- 비밀번호 검증 규칙이 적절한지 확인
- JWT 토큰 타입 구분 로직이 올바른지 확인
- 로깅 레벨 조정이 적절한지 확인
- Secret 파일이 Git에서 제거되었는지 확인

## 통계
- 변경 파일: 24개
- 추가: 613줄 / 삭제: 92줄
- 커밋: 8개
