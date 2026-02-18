# 출시 전 검토 체크리스트

**검토 일자**: 2024년
**검토 대상**: Co-Talk Backend v0.0.1-SNAPSHOT

---

## ✅ 통과 항목

### 1. 보안 설정
- ✅ JWT 시크릿 키 검증 로직 구현 (`JwtTokenProvider.validateSecret()`)
  - 최소 32자(256비트) 검증
  - 빈 값 체크
- ✅ 보안 헤더 설정 완료 (`SecurityConfig`)
  - X-Frame-Options: DENY
  - X-Content-Type-Options: nosniff
  - Content-Security-Policy 설정
  - HSTS 설정 (1년, preload)
  - Referrer-Policy 설정
  - Permissions-Policy 설정
- ✅ CORS 설정 적절히 구성
- ✅ CSRF 비활성화 (JWT 사용으로 적절)
- ✅ 세션 관리 STATELESS 설정

### 2. 프로덕션 설정
- ✅ `application-prod.yml` 존재 및 적절한 설정
  - 에러 정보 숨김 처리 (`include-stacktrace: never`)
  - Swagger UI 비활성화
  - 로깅 레벨 최적화 (WARN/INFO)
- ✅ `application-kubernetes.yml` 존재
  - Flyway 마이그레이션 활성화
  - 헬스체크 설정 완료
  - Graceful shutdown 설정

### 3. 로깅 설정
- ✅ `logback-spring.xml` 프로파일별 설정 완료
  - 프로덕션: WARN 레벨, 파일/JSON 출력
  - 로그 롤링 정책 설정 (100MB, 30일)
  - 분산 추적 지원 (traceId, spanId)

### 4. 예외 처리
- ✅ `GlobalExceptionHandler` 구현 완료
  - 모든 도메인 예외 처리
  - 적절한 HTTP 상태 코드 매핑
  - 일관된 에러 응답 형식

### 5. 헬스체크
- ✅ Kubernetes Liveness/Readiness 프로브 설정
  - `/actuator/health/liveness`
  - `/actuator/health/readiness`
  - 커스텀 헬스 인디케이터 (DB, Redis)

### 6. 데이터베이스
- ✅ Flyway 마이그레이션 설정
  - `baseline-on-migrate: true`
  - 인덱스 최적화 마이그레이션 존재
- ✅ JPA 설정 적절
  - `ddl-auto: validate` (프로덕션)
  - `open-in-view: false`

### 7. 코드 품질
- ✅ System.out.println 사용 없음
- ✅ printStackTrace 사용 없음
- ✅ TODO/FIXME 주석 없음 (문서 파일 제외)

### 8. 의존성 관리
- ✅ Spring Boot 3.5.6 사용
- ✅ Java 25 사용
- ✅ 필수 라이브러리 포함 (JWT, Redis, Rate Limiting 등)

---

## ⚠️ 주의 필요 항목

### 1. Secret 관리 (긴급)
**위치**: `k8s/base/secret.yaml`

**문제점**:
- 기본값이 그대로 설정되어 있음
- 프로덕션 배포 전 반드시 변경 필요

**조치 사항**:
```yaml
# 반드시 변경해야 할 항목:
SPRING_DATASOURCE_PASSWORD: "CHANGE_ME_IN_PRODUCTION"
JWT_SECRET: "CHANGE_ME_IN_PRODUCTION_USE_STRONG_SECRET_KEY"
MINIO_ACCESS_KEY: "minio-access-key"
MINIO_SECRET_KEY: "minio-secret-key"
```

**권장 사항**:
- 외부 Secret Manager 사용 (Vault, AWS Secrets Manager 등)
- Git에 실제 Secret 커밋 금지
- CI/CD 파이프라인에서 Secret 주입

### 2. 기본 설정 파일 (application.yml)
**위치**: `src/main/resources/application.yml`

**현재 상태**:
- 로깅 레벨: `com.cotalk: DEBUG` (개발용)
- 에러 스택트레이스: `on_param` (개발용)

**영향**:
- 프로덕션 프로파일(`prod`, `kubernetes`)에서는 오버라이드되므로 문제 없음
- 하지만 명확성을 위해 기본값도 조정 권장

**권장 조치** (선택사항):
```yaml
# application.yml의 기본값을 더 보수적으로 설정
logging:
  level:
    root: INFO
    com.cotalk: INFO  # DEBUG → INFO

server:
  error:
    include-stacktrace: never  # on_param → never
```

### 3. Swagger UI 프로덕션 비활성화
**상태**: ✅ `application-prod.yml`에서 비활성화됨

**확인 사항**:
- 프로덕션 환경에서 `spring.profiles.active=prod` 또는 `kubernetes` 설정 확인
- Swagger 엔드포인트 접근 불가 확인 필요

### 4. 환경 변수 필수 체크
**필수 환경 변수**:
- `JWT_SECRET` - 최소 32자 이상
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `SPRING_DATA_REDIS_HOST`

**확인 방법**:
- 애플리케이션 시작 시 `JwtTokenProvider` 생성자에서 검증됨
- 누락 시 즉시 실패 (안전)

### 5. Rate Limiting 설정
**상태**: ✅ 설정 완료

**확인 사항**:
- 프로덕션 환경에서 `app.rate-limit.enabled=true` 확인
- Rate Limit 값이 비즈니스 요구사항에 맞는지 검토

---

## 📋 배포 전 최종 체크리스트

### 보안
- [ ] Secret 파일의 모든 기본값 변경
- [ ] JWT_SECRET 최소 32자 이상 설정
- [ ] 데이터베이스 비밀번호 강력한 값으로 변경
- [ ] MinIO Access/Secret Key 변경
- [ ] 프로덕션 환경에서 Swagger UI 접근 불가 확인
- [ ] CORS 설정이 실제 프론트엔드 도메인과 일치하는지 확인

### 설정
- [ ] 프로덕션 프로파일 활성화 확인 (`spring.profiles.active=prod,kubernetes`)
- [ ] 로깅 레벨 프로덕션 적합 여부 확인
- [ ] 데이터베이스 연결 풀 크기 적절 여부 확인
- [ ] Redis 연결 설정 확인
- [ ] Rate Limiting 활성화 확인

### 인프라
- [ ] Kubernetes Secret 생성 및 주입 확인
- [ ] ConfigMap 설정 확인
- [ ] 헬스체크 엔드포인트 동작 확인
- [ ] Liveness/Readiness 프로브 설정 확인
- [ ] 리소스 제한 (CPU/Memory) 적절 여부 확인
- [ ] HPA 설정 확인

### 데이터베이스
- [ ] Flyway 마이그레이션 스크립트 검증
- [ ] 프로덕션 데이터베이스 백업 계획 수립
- [ ] 마이그레이션 롤백 계획 수립

### 모니터링
- [ ] Prometheus 메트릭 수집 확인
- [ ] 로그 수집 파이프라인 확인 (Loki 등)
- [ ] 분산 추적 (Zipkin) 설정 확인
- [ ] 알람 설정 확인

### 테스트
- [ ] 통합 테스트 통과 확인
- [ ] 부하 테스트 수행 (선택사항)
- [ ] 장애 시나리오 테스트 (선택사항)

---

## 🔍 추가 권장 사항

### 1. 보안 강화
- [ ] API Rate Limiting 값 재검토 (현재 설정이 적절한지)
- [ ] JWT 토큰 만료 시간 검토 (현재: 1일)
- [ ] Refresh Token 만료 시간 검토 (현재: 7일)
- [ ] 비밀번호 정책 강화 (최소 길이, 복잡도 등)

### 2. 성능 최적화
- [ ] 데이터베이스 인덱스 성능 테스트
- [ ] Redis 캐시 전략 검토
- [ ] Connection Pool 크기 튜닝
- [ ] JPA 배치 크기 최적화 (현재: 50)

### 3. 운영 준비
- [ ] 로그 보관 정책 수립
- [ ] 백업 및 복구 절차 문서화
- [ ] 장애 대응 매뉴얼 작성
- [ ] 모니터링 대시보드 구성

### 4. 문서화
- [ ] API 문서 최신화
- [ ] 배포 가이드 문서화
- [ ] 운영 가이드 작성

---

## 📊 검토 요약

| 카테고리 | 상태 | 비고 |
|---------|------|------|
| 보안 설정 | ✅ 양호 | JWT 검증, 보안 헤더 완료 |
| 프로덕션 설정 | ✅ 양호 | 적절한 오버라이드 설정 |
| 로깅 | ✅ 양호 | 프로파일별 설정 완료 |
| 예외 처리 | ✅ 양호 | 전역 핸들러 구현 완료 |
| 헬스체크 | ✅ 양호 | K8s 프로브 설정 완료 |
| Secret 관리 | ⚠️ 주의 | 배포 전 반드시 변경 필요 |
| 기본 설정 | ⚠️ 주의 | 프로파일 오버라이드로 문제 없음 |

---

## 🚀 배포 권장 사항

1. **Secret 변경**: 배포 전 가장 우선적으로 처리
2. **스테이징 환경 테스트**: 프로덕션 배포 전 스테이징에서 검증
3. **점진적 배포**: Blue-Green 또는 Canary 배포 전략 고려
4. **롤백 계획**: 문제 발생 시 즉시 롤백 가능하도록 준비

---

**검토 완료**: 대부분의 항목이 양호하나, Secret 관리가 가장 중요한 주의 사항입니다.
