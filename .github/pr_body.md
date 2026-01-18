## 📋 개요
Refresh Token 기능, Redis Worker ID 할당 및 분산 락 기능 추가

## 🎯 변경사항

### Refresh Token 기능 구현
- RefreshTokenService, RefreshToken entity 추가
- RefreshTokenRepository 포트 및 어댑터 구현
- AuthController에 토큰 갱신 엔드포인트 추가
- InvalidRefreshTokenException 예외 추가
- RefreshTokenServiceTest 테스트 추가

### Redis Worker ID 할당 기능 추가
- RedisWorkerIdAllocator 구현 (분산 환경 Worker ID 자동 할당)
- IdGeneratorConfig에 Redis 기반 할당 로직 통합
- SnowflakeIdGenerator와 연동
- RedisWorkerIdAllocatorTest 테스트 추가

### 분산 락 기능 추가
- DistributedLockExecutor 구현
- DistributedLockException 예외 추가
- Redis 기반 분산 락 지원

### 보안 및 인프라 개선
- SecurityContextHelper 추가 (보안 컨텍스트 관리)
- CustomUserPrincipal 개선
- GlobalExceptionHandler에 예외 핸들러 추가

### 테스트 코드 보완
- RefreshTokenServiceTest 추가
- RefreshTokenTest 추가
- RedisWorkerIdAllocatorTest 추가
- 기존 테스트 코드 업데이트

### 설정 및 문서 업데이트
- build.gradle.kts 의존성 추가
- application.yml 설정 업데이트
- project-rules.md 업데이트

## 🔗 관련 이슈
Closes #5

## ✅ 체크리스트
- [x] 코드 리뷰 준비 완료
- [x] 테스트 통과
- [x] 문서 업데이트 완료

## 📝 테스트 방법
1. Refresh Token: 로그인 후 토큰 갱신 엔드포인트 호출하여 새 Access Token 발급 확인
2. Redis Worker ID: 여러 인스턴스 실행 시 각각 고유한 Worker ID 할당 확인
3. 분산 락: 동시 요청 시 락이 정상적으로 작동하는지 확인
4. 보안: SecurityContextHelper를 통한 보안 컨텍스트 관리 확인

## 🔍 리뷰 포인트
- Refresh Token 저장 및 검증 로직이 안전한지
- Redis Worker ID 할당 로직이 분산 환경에서 올바르게 동작하는지
- 분산 락이 데드락 없이 정상적으로 해제되는지
- 예외 처리가 적절한지

## 통계
- 변경 파일: 50개
- 추가: 2,782줄 / 삭제: 565줄
- 커밋: 1개
