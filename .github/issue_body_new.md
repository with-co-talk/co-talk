## 📋 개요
Refresh Token 기능, Redis Worker ID 할당 및 분산 락 기능 추가

## 🎯 목표
- JWT 토큰 갱신을 위한 Refresh Token 기능 구현
- 분산 환경에서 Snowflake ID 생성기의 Worker ID 자동 할당
- Redis 기반 분산 락 기능 구현

## 💡 제안 사항

### Refresh Token 기능
- RefreshTokenService를 통한 토큰 생성, 갱신, 폐기
- RefreshToken 엔티티 및 Repository 구현
- AuthController에 토큰 갱신 엔드포인트 추가

### Redis Worker ID 할당
- RedisWorkerIdAllocator 구현
- 분산 환경에서 각 인스턴스가 고유한 Worker ID 자동 할당
- TTL 기반 자동 해제로 좀비 잠금 방지

### 분산 락 기능
- DistributedLockExecutor 구현
- Redis 기반 분산 락 지원
- 예외 처리 및 잠금 해제 보장

## ✅ 체크리스트
- [x] 요구사항 분석 완료
- [x] 설계 검토 완료
- [x] 구현 완료
- [x] 테스트 완료
- [x] 문서화 완료

## 📝 참고사항
- IdGenerator 인터페이스를 통한 ID 생성 추상화
- SecurityContextHelper 추가로 보안 컨텍스트 관리 개선
- 모든 기능에 대한 단위 테스트 및 통합 테스트 작성 완료

