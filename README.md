# Co-Talk

대화에 집중한 실시간 커뮤니케이션 플랫폼 백엔드

## 기술 스택

| 영역 | 기술 |
|------|------|
| Language | Java 21 (Virtual Threads) |
| Framework | Spring Boot 3.3 |
| Database | PostgreSQL, Redis |
| Messaging | WebSocket (STOMP), Redis Pub/Sub |
| Storage | MinIO (S3 호환) |
| Push | Firebase Cloud Messaging |
| Auth | JWT (Access + Refresh Token) |
| Docs | SpringDoc OpenAPI (Swagger) |
| Observability | Prometheus, Zipkin, Loki |
| Container | Docker, Kubernetes |

## 아키텍처

**Hexagonal Architecture (Ports and Adapters)**

```
src/main/java/com/cotalk/
├── adapter/
│   ├── inbound/        # REST Controllers, WebSocket
│   └── outbound/       # Repository, External APIs
├── application/        # Use Cases, Services
├── domain/             # Entities, Business Logic
└── infrastructure/     # Config, Security, Exception
```

## 시작하기

### 사전 요구사항

- Java 21+
- Docker & Docker Compose
- Gradle 8+

### 로컬 개발 환경

```bash
# 1. 인프라 실행 (PostgreSQL, Redis, MinIO)
docker-compose -f docker-compose.dev.yml up -d

# 2. 애플리케이션 실행
./gradlew bootRun --args='--spring.profiles.active=local'
```

### 테스트

```bash
# 전체 테스트 실행
./gradlew test

# 커버리지 리포트 생성
./gradlew jacocoTestReport
# 리포트 위치: build/reports/jacoco/test/html/index.html
```

### API 문서

로컬 실행 후 접속: http://localhost:8080/swagger-ui.html

## 환경 변수

| 변수 | 설명 | 필수 |
|------|------|------|
| `DB_USERNAME` | PostgreSQL 사용자명 | O |
| `DB_PASSWORD` | PostgreSQL 비밀번호 | O |
| `JWT_SECRET` | JWT 서명 키 (최소 32자) | O |
| `REDIS_HOST` | Redis 호스트 | O |
| `REDIS_PASSWORD` | Redis 비밀번호 | - |
| `MINIO_ACCESS_KEY` | MinIO 접근 키 | - |
| `MINIO_SECRET_KEY` | MinIO 비밀 키 | - |
| `FIREBASE_CREDENTIALS_PATH` | FCM 인증 파일 경로 | - |

## 배포

### Docker

```bash
# 이미지 빌드
docker build -t cotalk-backend .

# 전체 스택 실행
docker-compose up -d
```

### Kubernetes

```bash
# 개발 환경
kubectl apply -k k8s/overlays/dev

# 프로덕션 환경
kubectl apply -k k8s/overlays/prod
```

자세한 내용은 [INFRASTRUCTURE.md](INFRASTRUCTURE.md) 참고

## 프로젝트 구조

```
├── docker/                 # Docker 관련 설정
├── k8s/                    # Kubernetes 매니페스트
│   ├── base/               # 기본 리소스
│   └── overlays/           # 환경별 설정 (dev, prod)
├── src/
│   ├── main/
│   │   ├── java/           # 소스 코드
│   │   └── resources/      # 설정 파일
│   └── test/               # 테스트 코드
├── docker-compose.yml      # 전체 스택
└── docker-compose.dev.yml  # 개발용 인프라
```

## 주요 기능

- 실시간 1:1 / 그룹 채팅 (WebSocket)
- 읽음 표시 기능 (카톡/라인 스타일)
- 사용자 인증 및 권한 관리
- 친구 관리 및 차단
- 파일 업로드 (이미지, 동영상)
- 푸시 알림 (FCM)
- 메시지 검색
- Rate Limiting

### 읽기 기능

카카오톡/라인 스타일의 읽음 표시 시스템을 제공합니다.

- **REST API**: `POST /api/v1/chat/rooms/{roomId}/read` (하이브리드 방식: 요청은 REST, 업데이트는 WebSocket)
- **실시간 동기화**: 읽음 처리 후 WebSocket으로 업데이트된 메시지 및 채팅 목록 전송

자세한 내용은 [읽기 기능 가이드](docs/READ_FEATURE.md)를 참조하세요.

## 문서

- [Co-Talk Docs](https://github.com/with-co-talk/co-talk-docs) - API 문서, 아키텍처, 기술 결정 등
- [INFRASTRUCTURE.md](INFRASTRUCTURE.md) - 인프라 및 배포 가이드
- [RELEASE_CHECKLIST.md](RELEASE_CHECKLIST.md) - 출시 전 체크리스트

## 라이선스

Private
