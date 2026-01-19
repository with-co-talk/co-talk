## 📋 개요
PR #6 코드 리뷰 결과 반영 - 성능 개선 및 코드 품질 향상

## 🎯 목표
- AuthController의 중복 DB 조회 제거
- SecurityContextHelper에서 CustomUserPrincipal 활용
- RefreshTokenService의 트랜잭션 경계 명확화
- 코드 품질 및 유지보수성 개선

## 💡 리팩토링 계획

### 1. AuthController.login() - 중복 DB 조회 제거
**현재 문제점:**
- `login()` 호출 후 `getUserIdByEmail()`로 추가 DB 조회 발생
- 로그인 시 이미 사용자 정보를 조회했는데 재조회

**개선 방안:**
- `LoginUseCase`에 `LoginResult` 반환 타입 추가 또는
- `LoginService.login()`에서 사용자 ID도 함께 반환하도록 수정

### 2. SecurityContextHelper - CustomUserPrincipal 활용
**현재 문제점:**
- `CustomUserPrincipal`이 있으나 사용하지 않음
- 타입 체크가 복잡하고 유지보수 어려움

**개선 방안:**
- `SecurityContextHelper.getCurrentUserId()`에서 `CustomUserPrincipal` 타입 체크 우선
- 타입 안정성 향상

### 3. RefreshTokenService - 트랜잭션 경계 명확화
**현재 문제점:**
- `revoke()` 호출 후 명시적인 `save()` 없음
- JPA 변경 감지에 의존하지만 명시적이지 않음

**개선 방안:**
- `revoke()` 후 명시적으로 `save()` 호출 또는
- Repository에 `revokeByUserId()` 메서드 추가

## ✅ 체크리스트
- [ ] AuthController.login() 중복 조회 제거
- [ ] SecurityContextHelper CustomUserPrincipal 활용
- [ ] RefreshTokenService 트랜잭션 경계 명확화
- [ ] 테스트 코드 업데이트
- [ ] 코드 리뷰 반영 확인

## 📝 참고사항
- PR #6 코드 리뷰 결과 반영
- 기존 기능 동작 유지 필수
- 테스트 커버리지 유지
