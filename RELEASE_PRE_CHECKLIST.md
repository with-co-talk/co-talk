# 🚀 출시 전 확인 체크리스트

## ✅ 완료된 항목

### 1. Secret 파일 Git 추적 제거
- ✅ `k8s/base/secret.yaml` 파일을 Git 추적에서 제거 완료
- ✅ `.gitignore`에 이미 `k8s/**/secret.yaml` 포함되어 있음
- ⚠️ **주의**: 파일은 로컬에 남아있지만 Git에는 더 이상 추적되지 않음

---

## 🔴 필수 확인 사항 (배포 전 반드시 확인)

### 1. 환경변수 설정

#### 필수 환경변수
- [ ] **JWT_SECRET** - 최소 32자 이상의 안전한 키
  - 현재 검증: `JwtTokenProvider`에서 32자 이상 검증 구현됨
  - 설정 방법: `export JWT_SECRET="your-strong-secret-key-minimum-32-characters"`
  
- [ ] **MINIO_ACCESS_KEY** - MinIO 접근 키
  - 설정 방법: `export MINIO_ACCESS_KEY="your-minio-access-key"`
  
- [ ] **MINIO_SECRET_KEY** - MinIO 비밀 키
  - 설정 방법: `export MINIO_SECRET_KEY="your-minio-secret-key"`

#### 데이터베이스 환경변수
- [ ] **SPRING_DATASOURCE_URL** - 데이터베이스 연결 URL
- [ ] **SPRING_DATASOURCE_USERNAME** - 데이터베이스 사용자명
- [ ] **SPRING_DATASOURCE_PASSWORD** - 데이터베이스 비밀번호

#### Redis 환경변수 (선택)
- [ ] **SPRING_DATA_REDIS_HOST** - Redis 호스트
- [ ] **SPRING_DATA_REDIS_PORT** - Redis 포트 (기본값: 6379)
- [ ] **SPRING_DATA_REDIS_PASSWORD** - Redis 비밀번호 (필요시)

### 2. 프로덕션 프로파일 활성화

#### Java 실행 시
```bash
java -jar app.jar --spring.profiles.active=prod
```

#### Kubernetes 배포 시
- `application-kubernetes.yml` 프로파일 자동 활성화
- 또는 환경변수: `SPRING_PROFILES_ACTIVE=kubernetes`

#### Docker 배포 시
- 환경변수: `SPRING_PROFILES_ACTIVE=docker`

### 3. Kubernetes Secret 설정

#### ⚠️ 중요: `k8s/base/secret.yaml` 파일 처리

**현재 상태:**
- ✅ Git 추적에서 제거됨 (보안)
- ⚠️ 파일은 로컬에 존재하지만 Git에는 커밋되지 않음
- ⚠️ `kustomization.yaml`에서 여전히 참조됨

**배포 전 조치:**

1. **Secret 파일 생성** (로컬 또는 CI/CD 파이프라인에서):
   ```bash
   # 예시: k8s/base/secret.yaml 파일 생성
   kubectl create secret generic cotalk-secret \
     --from-literal=SPRING_DATASOURCE_PASSWORD='your-strong-password' \
     --from-literal=JWT_SECRET='your-strong-jwt-secret-minimum-32-characters' \
     --from-literal=MINIO_ACCESS_KEY='your-minio-access-key' \
     --from-literal=MINIO_SECRET_KEY='your-minio-secret-key' \
     --namespace=cotalk \
     --dry-run=client -o yaml > k8s/base/secret.yaml
   ```

2. **또는 외부 Secret Manager 사용** (권장):
   - AWS Secrets Manager
   - HashiCorp Vault
   - Kubernetes External Secrets Operator

3. **kustomization.yaml 수정** (Secret Manager 사용 시):
   ```yaml
   # k8s/base/kustomization.yaml
   resources:
     - namespace.yaml
     - serviceaccount.yaml
     - configmap.yaml
     # - secret.yaml  # 주석 처리 (외부 Secret Manager 사용 시)
     - deployment.yaml
     ...
   ```

### 4. 프로덕션 설정 확인

#### Swagger UI 비활성화
- ✅ `application-prod.yml`에서 Swagger UI 비활성화됨
- ✅ `application-kubernetes.yml`에서도 확인 필요

#### 로깅 레벨
- ✅ 프로덕션: `INFO` 레벨
- ✅ 개발: `DEBUG` 레벨 (프로덕션에서는 오버라이드됨)

#### 에러 스택트레이스
- ✅ 프로덕션: `never` (스택트레이스 노출 안 함)
- ✅ 개발: `on_param` (프로덕션에서는 오버라이드됨)

---

## 📋 배포 체크리스트

### 배포 전
- [ ] 모든 환경변수 설정 확인
- [ ] JWT_SECRET이 32자 이상인지 확인
- [ ] 프로덕션 프로파일 활성화 확인
- [ ] Secret 파일이 Git에 커밋되지 않았는지 확인
- [ ] 데이터베이스 연결 정보 확인
- [ ] Redis 연결 정보 확인 (사용하는 경우)
- [ ] MinIO 연결 정보 확인 (사용하는 경우)

### 배포 후
- [ ] 애플리케이션 정상 시작 확인
- [ ] 헬스체크 엔드포인트 확인 (`/actuator/health`)
- [ ] Swagger UI 접근 불가 확인 (프로덕션)
- [ ] 로그 레벨 확인 (INFO 이상)
- [ ] JWT 토큰 생성/검증 테스트
- [ ] 데이터베이스 연결 테스트
- [ ] Redis 연결 테스트 (사용하는 경우)
- [ ] MinIO 연결 테스트 (사용하는 경우)

---

## 🔐 보안 체크리스트

- [ ] 모든 Secret 값이 기본값이 아닌지 확인
- [ ] JWT_SECRET이 충분히 강력한지 확인 (32자 이상, 랜덤)
- [ ] 데이터베이스 비밀번호가 강력한지 확인
- [ ] MinIO 자격증명이 안전한지 확인
- [ ] Git 히스토리에 Secret이 포함되지 않았는지 확인
- [ ] 프로덕션 환경에서 디버그 로그가 노출되지 않는지 확인

---

## 📝 참고 사항

### 환경변수 설정 예시

#### Linux/macOS
```bash
export JWT_SECRET="your-strong-secret-key-minimum-32-characters-long"
export MINIO_ACCESS_KEY="your-minio-access-key"
export MINIO_SECRET_KEY="your-minio-secret-key"
export SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/cotalk"
export SPRING_DATASOURCE_USERNAME="cotalk"
export SPRING_DATASOURCE_PASSWORD="your-strong-password"
```

#### Kubernetes
```yaml
# k8s/base/secret.yaml (로컬에서만 관리, Git에 커밋 금지)
apiVersion: v1
kind: Secret
metadata:
  name: cotalk-secret
  namespace: cotalk
type: Opaque
stringData:
  SPRING_DATASOURCE_PASSWORD: "your-strong-password"
  JWT_SECRET: "your-strong-jwt-secret-minimum-32-characters"
  MINIO_ACCESS_KEY: "your-minio-access-key"
  MINIO_SECRET_KEY: "your-minio-secret-key"
```

### JWT_SECRET 생성 방법
```bash
# OpenSSL 사용
openssl rand -base64 32

# 또는
openssl rand -hex 32
```

---

**마지막 업데이트**: 2026-01-19
**검토자**: 출시 전 반드시 모든 항목 확인 필요
