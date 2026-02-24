<!-- Generated: 2026-02-23 | Updated: 2026-02-23 -->

# Co-Talk

## 개요
실시간 채팅 애플리케이션 백엔드. Java 25 / Spring Boot 3.5.6 기반, 헥사고날 아키텍처(Ports & Adapters) 적용.

## 기술 스택
| 기술 | 버전 | 용도 |
|------|------|------|
| Java | 25 | 언어 |
| Spring Boot | 3.5.6 | 프레임워크 |
| PostgreSQL | 16 | 주 데이터베이스 |
| Redis | 7 | 캐시, Pub/Sub, 분산락, Rate Limit, 접속자 추적 |
| MinIO | - | 파일 저장소 (S3 호환) |
| Firebase FCM | - | 푸시 알림 |
| WebSocket (STOMP) | - | 실시간 채팅 |
| Flyway | - | DB 마이그레이션 |
| Docker | - | 컨테이너화 + 운영 배포 |

## 주요 파일
| 파일 | 설명 |
|------|------|
| `build.gradle.kts` | 빌드 설정. JaCoCo 60% 커버리지 강제, QueryDSL 5.1.0 |
| `docker-compose.yml` | 운영 서비스 구성 (App×3 + PostgreSQL + Redis + MinIO + Nginx) |
| `docker-compose.dev.yml` | 개발 환경 (인프라만, 앱은 로컬 실행) |
| `docker-compose.monitoring.yml` | 모니터링 스택 (Prometheus + Grafana + Loki + Alertmanager) |
| `docker-compose.nas.yml` | NAS 배포 전용 구성 |
| `Dockerfile` | 멀티스테이지 빌드 (Gradle → JRE 런타임) |
| `scripts/deploy.sh` | 카나리 롤링 배포 (app-1 → 메트릭 검증 → app-2/3) |

## 디렉토리 구조
| 디렉토리 | 용도 |
|-----------|------|
| `src/main/java/com/cotalk/` | 애플리케이션 소스 코드 (`src/main/java/com/cotalk/AGENTS.md` 참조) |
| `src/test/java/com/cotalk/` | 테스트 코드 (`src/test/java/com/cotalk/AGENTS.md` 참조) |
| `docker/` | Docker 인프라 설정 (`docker/AGENTS.md` 참조) |
| `k8s/` | Kubernetes 배포 매니페스트 (`k8s/AGENTS.md` 참조) |
| `k6/` | 부하 테스트 시나리오 |
| `scripts/` | 배포/유틸리티 스크립트 |
| `docs/` | 프로젝트 문서 |
| `rules/` | 프로젝트 규칙 (project-rules.md) |

## AI 에이전트 가이드

### 아키텍처 규칙 (ArchUnit 자동 검증됨)
1. **domain** → application, adapter, infrastructure 의존 금지
2. **application** → adapter, infrastructure 의존 금지
3. **inbound adapter** → outbound adapter 직접 의존 금지
4. 최상위 패키지 간 순환 의존 금지

### 새 기능 추가 순서
1. `domain/port/inbound/` → UseCase 인터페이스 정의
2. `domain/port/outbound/` → 필요한 Repository/서비스 포트 정의
3. 테스트 먼저 작성 (TDD 필수)
4. `application/service/` → UseCase 구현체
5. `adapter/inbound/rest/` → 컨트롤러 (UseCase 포트만 의존)
6. `adapter/outbound/persistence/` → Repository 어댑터

### 테스트 실행
```bash
./gradlew test           # 전체 테스트
./gradlew jacocoTestReport  # 커버리지 리포트
```

### 빌드 & 실행
```bash
./gradlew bootRun --args='--spring.profiles.active=dev'  # 로컬 실행
docker-compose -f docker-compose.dev.yml up -d            # 인프라 실행
```

## 의존성

### 주요 라이브러리
- Spring Data JPA + QueryDSL 5.1.0
- Spring Security + JWT (jjwt 0.12.6)
- Spring WebSocket (STOMP)
- Spring Cache + Redis (Lettuce)
- Redisson (분산락)
- Flyway (DB 마이그레이션)
- Micrometer + Prometheus (메트릭)
- Firebase Admin SDK (푸시 알림)
- MinIO SDK (파일 저장)
- Jsoup (HTML 새니타이징, 링크 미리보기)

### 테스트 라이브러리
- JUnit 5 + Mockito
- ArchUnit 1.4.1 (아키텍처 테스트)
- H2 (인메모리 테스트 DB, PostgreSQL 모드)
- Awaitility 4.2.0 (비동기 테스트)

<!-- MANUAL: -->
