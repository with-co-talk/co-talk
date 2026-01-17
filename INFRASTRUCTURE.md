# Co-Talk 인프라 가이드

## 목차
- [로컬 개발 환경](#로컬-개발-환경)
- [Docker Compose](#docker-compose)
- [모니터링](#모니터링)
- [Kubernetes 배포](#kubernetes-배포)

---

## 로컬 개발 환경

### 의존성 서비스 실행 (PostgreSQL, Redis, MinIO)

```bash
# 개발용 인프라만 실행
docker-compose -f docker-compose.dev.yml up -d

# 상태 확인
docker-compose -f docker-compose.dev.yml ps

# 종료
docker-compose -f docker-compose.dev.yml down
```

### 애플리케이션 실행

```bash
# Gradle로 실행
./gradlew bootRun --args='--spring.profiles.active=local'

# 또는 IDE에서 CoTalkApplication 실행 (프로파일: local)
```

---

## Docker Compose

### 전체 스택 실행 (애플리케이션 + 인프라 + 모니터링)

```bash
# 이미지 빌드 및 실행
docker-compose up -d --build

# 로그 확인
docker-compose logs -f cotalk-app

# 특정 서비스 재시작
docker-compose restart cotalk-app

# 종료 및 정리
docker-compose down -v
```

### 서비스 URL

| 서비스 | URL | 설명 |
|--------|-----|------|
| Co-Talk API | http://localhost:8080 | 메인 애플리케이션 |
| Prometheus | http://localhost:9090 | 메트릭 수집 |
| Grafana | http://localhost:3000 | 대시보드 (admin/admin) |
| Zipkin | http://localhost:9411 | 분산 추적 |
| MinIO Console | http://localhost:9001 | 파일 스토리지 (minio/minio123) |

---

## 모니터링

### Actuator 엔드포인트

```bash
# 헬스 체크
curl http://localhost:8080/actuator/health

# Liveness/Readiness (Kubernetes용)
curl http://localhost:8080/actuator/health/liveness
curl http://localhost:8080/actuator/health/readiness

# Prometheus 메트릭
curl http://localhost:8080/actuator/prometheus

# 애플리케이션 정보
curl http://localhost:8080/actuator/info
```

### Grafana 대시보드

1. http://localhost:3000 접속 (admin/admin)
2. Dashboards > Browse에서 "Co-Talk Overview" 선택
3. 제공되는 패널:
   - Application Status
   - Response Time (p99)
   - Error Rate
   - Requests Per Second
   - JVM Memory
   - Database Connections

### 커스텀 메트릭

| 메트릭 이름 | 타입 | 설명 |
|------------|------|------|
| cotalk.messages.sent | Counter | 전송된 메시지 수 |
| cotalk.messages.received | Counter | 수신된 메시지 수 |
| cotalk.messages.processing.time | Timer | 메시지 처리 시간 |
| cotalk.users.registered | Counter | 사용자 등록 수 |
| cotalk.auth.login.success | Counter | 로그인 성공 수 |
| cotalk.auth.login.failure | Counter | 로그인 실패 수 |
| cotalk.websocket.connections | Gauge | 활성 WebSocket 연결 수 |
| cotalk.chatrooms.active | Gauge | 활성 채팅방 수 |

### 알림 규칙 (Alertmanager)

- **CoTalkAppDown**: 애플리케이션이 5분 이상 다운
- **HighErrorRate**: 에러율 5% 이상
- **HighResponseTime**: 응답 시간 2초 이상
- **HighCpuUsage**: CPU 사용률 80% 이상
- **HighMemoryUsage**: 메모리 사용률 85% 이상
- **DatabaseConnectionsHigh**: DB 연결 풀 90% 이상 사용

---

## Kubernetes 배포

### Kustomize 구조

```
k8s/
├── base/                    # 기본 리소스
│   ├── namespace.yaml
│   ├── configmap.yaml
│   ├── secret.yaml
│   ├── deployment.yaml
│   ├── service.yaml
│   ├── ingress.yaml
│   ├── hpa.yaml
│   ├── pdb.yaml
│   ├── serviceaccount.yaml
│   ├── networkpolicy.yaml
│   ├── servicemonitor.yaml  # Prometheus Operator용
│   └── kustomization.yaml
├── overlays/
│   ├── dev/                 # 개발 환경
│   │   ├── namespace.yaml
│   │   └── kustomization.yaml
│   └── prod/                # 운영 환경
│       ├── namespace.yaml
│       ├── ingress-patch.yaml
│       └── kustomization.yaml
```

### 배포 명령어

```bash
# 개발 환경 배포
kubectl apply -k k8s/overlays/dev

# 운영 환경 배포
kubectl apply -k k8s/overlays/prod

# 배포 확인
kubectl get all -n cotalk-dev
kubectl get all -n cotalk-prod

# 롤아웃 상태 확인
kubectl rollout status deployment/cotalk-app-dev -n cotalk-dev

# 로그 확인
kubectl logs -f deployment/cotalk-app-dev -n cotalk-dev

# 삭제
kubectl delete -k k8s/overlays/dev
```

### 이미지 빌드 및 푸시

```bash
# Docker 이미지 빌드
docker build -t your-registry/cotalk-app:v1.0.0 .

# 이미지 푸시
docker push your-registry/cotalk-app:v1.0.0

# Kustomize로 이미지 태그 변경
cd k8s/overlays/prod
kustomize edit set image cotalk-app=your-registry/cotalk-app:v1.0.0
kubectl apply -k .
```

### 환경별 설정

| 설정 | Dev | Prod |
|------|-----|------|
| Replicas | 1 | 3 |
| Memory Request | 512Mi | 1Gi |
| Memory Limit | 1Gi | 2Gi |
| CPU Request | 250m | 500m |
| CPU Limit | 1000m | 2000m |
| HPA Min | 1 | 3 |
| HPA Max | 3 | 20 |
| Tracing Sample Rate | 100% | 5% |

### Secret 관리

```bash
# Secret 생성 (base64 인코딩)
kubectl create secret generic cotalk-secret \
  --from-literal=SPRING_DATASOURCE_PASSWORD=your-db-password \
  --from-literal=JWT_SECRET=your-jwt-secret \
  -n cotalk-prod

# 또는 Sealed Secrets / External Secrets Operator 사용 권장
```

---

## 분산 추적 (Zipkin)

### 추적 정보 확인

1. http://localhost:9411 접속
2. 서비스 이름: "co-talk" 선택
3. 요청별 trace 확인 가능

### 추적 설정

- **Dev 환경**: 100% 샘플링 (`management.tracing.sampling.probability=1.0`)
- **Prod 환경**: 5% 샘플링 (`management.tracing.sampling.probability=0.05`)

---

## 로깅

### 로그 파일 위치

- **일반 로그**: `logs/application.log`
- **JSON 로그**: `logs/application-json.log` (ELK/Loki용)

### 프로파일별 로그 레벨

| 프로파일 | Root Level | com.cotalk Level |
|----------|------------|------------------|
| local, dev | INFO | DEBUG |
| docker | INFO | INFO |
| prod | WARN | INFO |
| test | WARN | INFO |

### Loki에서 로그 조회 (Grafana)

1. Grafana > Explore 메뉴 선택
2. 데이터소스: Loki 선택
3. 쿼리 예시:
   ```logql
   {app="co-talk"} |= "ERROR"
   {app="co-talk", level="ERROR"} | json
   ```

---

## 헬스 체크

### 커스텀 헬스 인디케이터

- **DatabaseHealthIndicator**: PostgreSQL 연결 상태 및 응답 시간
- **RedisHealthIndicator**: Redis 연결 상태

### Kubernetes Probes

```yaml
livenessProbe:   # /actuator/health/liveness
  초기 지연: 60s, 주기: 10s

readinessProbe:  # /actuator/health/readiness
  초기 지연: 30s, 주기: 5s

startupProbe:    # /actuator/health
  초기 지연: 10s, 주기: 5s, 최대 시도: 30회
```

---

## 문제 해결

### 일반적인 문제

1. **애플리케이션이 시작되지 않음**
   - DB/Redis 연결 확인: `docker-compose ps`
   - 로그 확인: `docker-compose logs cotalk-app`

2. **메트릭이 수집되지 않음**
   - Prometheus 타겟 확인: http://localhost:9090/targets
   - Actuator 엔드포인트 확인: `curl localhost:8080/actuator/prometheus`

3. **Kubernetes 배포 실패**
   - Pod 상태 확인: `kubectl describe pod <pod-name> -n <namespace>`
   - 이벤트 확인: `kubectl get events -n <namespace>`

### 유용한 명령어

```bash
# Docker 리소스 정리
docker system prune -a

# Kubernetes 디버깅
kubectl run debug --rm -it --image=curlimages/curl -- sh
```
