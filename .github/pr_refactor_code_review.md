## 📋 개요
PR #6 코드 리뷰 결과 반영 - 성능 개선 및 코드 품질 향상

## 🎯 변경사항

### AuthController.login() - 중복 DB 조회 제거
- `LoginResult` record 추가하여 Access Token과 User ID 함께 반환
- `LoginUseCase.login()`이 `LoginResult` 반환하도록 변경
- `extractUserIdFromLoginProcess()` 메서드 제거
- **성능 개선**: 로그인 시 1회의 DB 조회로 감소 (기존 2회 → 1회)

### SecurityContextHelper - CustomUserPrincipal 활용
- `getCurrentUserId()`에서 `CustomUserPrincipal` 타입 우선 체크
- 타입 안정성 향상 및 코드 가독성 개선
- 하위 호환성 유지 (Long, String 타입도 지원)

### RefreshTokenService - 트랜잭션 경계 명확화
- `revoke()` 호출 후 명시적으로 `save()` 호출
- JPA 변경 감지에만 의존하지 않고 명시적 저장

### 테스트 코드 업데이트
- `LoginResult` 사용하도록 테스트 코드 수정
- `AuthControllerTest`, `LoginServiceTest`, `SecurityConfigTest` 업데이트

## 🔗 관련 이슈
Closes #8

## ✅ 체크리스트
- [x] AuthController.login() 중복 조회 제거
- [x] SecurityContextHelper CustomUserPrincipal 활용
- [x] RefreshTokenService 트랜잭션 경계 명확화
- [x] 테스트 코드 업데이트
- [x] 코드 리뷰 반영 확인

## 📝 테스트 방법
1. 로그인 API 호출하여 Access Token과 Refresh Token 정상 발급 확인
2. SecurityContextHelper를 통한 사용자 ID 조회 확인
3. Refresh Token 생성 시 기존 토큰 폐기 확인

## 🔍 리뷰 포인트
- LoginResult 도입으로 인한 API 변경이 기존 코드에 영향을 주지 않는지
- SecurityContextHelper의 하위 호환성 유지 확인
- 트랜잭션 경계 명확화로 인한 성능 영향 확인

## 통계
- 변경 파일: 8개
- 추가: 약 50줄 / 삭제: 약 20줄
- 커밋: 1개
