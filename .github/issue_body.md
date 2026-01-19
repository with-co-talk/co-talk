## 📋 개요
보안 강화, 코드 품질 개선, 출시 전 준비 작업을 포함한 종합적인 개선 작업

## 🎯 목표
- 보안 강화: 비밀번호 검증, XSS 방지, 로깅 개선
- JWT 토큰 관리 개선: Access/Refresh 토큰 타입 구분
- 코드 품질 개선: 예외 처리, 분산 락 개선
- 출시 전 준비: Secret 관리, 체크리스트 작성

## 💡 주요 변경사항

### 보안 강화
- 비밀번호 강도 검증 어노테이션 추가 (8-128자, 대문자/소문자/숫자/특수문자 각 1개 이상)
- XSS 방지를 위한 HtmlSanitizer 유틸리티 추가
- RefreshTokenService 로깅 레벨 조정 및 개인정보 제거

### JWT 토큰 관리
- Access 토큰과 Refresh 토큰을 구분하기 위한 token_type claim 추가
- generateRefreshToken() 메서드 추가
- 토큰 타입 기반 검증 및 관리 강화

### 코드 품질 개선
- 파일 업로드 및 예외 처리 개선
- 분산 락 Executor 개선 및 NoOp 구현 추가 (테스트 환경용)
- GlobalExceptionHandler 예외 처리 개선

### 출시 전 준비
- k8s/base/secret.yaml Git 추적 제거 (보안)
- RELEASE_PRE_CHECKLIST.md 추가: 환경변수 설정, 프로파일 활성화, Secret 관리 가이드

## ✅ 체크리스트
- [x] 보안 강화 기능 구현
- [x] JWT 토큰 타입 구분 기능 구현
- [x] 코드 품질 개선
- [x] 테스트 코드 업데이트
- [x] 출시 전 체크리스트 작성
- [x] Secret 파일 Git 추적 제거
