# Co-Talk 개발기 — 전체 시리즈 목차

> 사이드 프로젝트 하나로 백엔드 아키텍처, Flutter 앱, 배포 자동화, 모니터링까지 — 실시간 채팅 앱을 처음부터 끝까지 만들어본 기록.

---

## 시리즈 소개

Co-Talk은 Java/Spring Boot 백엔드 + Flutter 프론트엔드로 구성된 실시간 채팅 애플리케이션이다. 이 시리즈는 프로젝트를 처음 시작한 시점부터 프로덕션 배포, Flutter 앱 개발, CI/CD 자동화까지의 전 과정을 **실제 개발 순서대로** 기록한 20편의 글이다.

각 글은 독립적으로 읽을 수 있지만, 순서대로 읽으면 프로젝트가 성장하는 흐름을 따라갈 수 있다.

---

## 프로젝트 소개

| # | 제목 | 핵심 키워드 |
|---|------|------------|
| 00 | [Co-Talk: 실시간 채팅 백엔드를 처음부터 만들어보며 배운 것들](blog-00-project-introduction.md) | 프로젝트 소개, 기술 스택, Java 25, Spring Boot 3.5 |

> 전체 시리즈의 진입점. 왜 이 프로젝트를 시작했는지, 어떤 기술 스택을 선택했는지, 전체 아키텍처 개요를 다룬다.

---

## 백엔드 — 아키텍처와 핵심 기능 구현

| 순서 | # | 제목 | 핵심 키워드 | 이슈/PR |
|------|---|------|------------|---------|
| 1 | 01 | [실시간 채팅 아키텍처를 밑바닥부터 쌓아올리기](blog-01-realtime-chat-architecture.md) | WebSocket, STOMP, Redis Pub/Sub, Snowflake ID, AES 암호화 | #1~#17 |
| 2 | 02 | [헥사고날 아키텍처로 리팩토링하기](blog-02-hexagonal-architecture.md) | Ports & Adapters, ArchUnit, 도메인 독립성, 패키지 구조 | #19, #22, #24 |
| 3 | 03 | [CI만 터지는 이유 — MinIO 설정 한 줄의 차이](blog-03-ci-minio-connection.md) | CI 트러블슈팅, MinIO, @ConditionalOnProperty | #30 |
| 4 | 05 | [DTO에서 userId를 제거한 이유 — 보안 리팩토링](blog-05-dto-userid-security.md) | SecurityContext, 인증 정보 신뢰, DTO 설계 | #46 |
| 5 | 06 | [Redis 캐시 직렬화 삽질기](blog-06-redis-cache-serialization.md) | LocalDateTime 직렬화, 다형성, ObjectMapper 설정 | #48 |

---

## 백엔드 — 인프라와 운영

| 순서 | # | 제목 | 핵심 키워드 | 이슈/PR |
|------|---|------|------------|---------|
| 6 | 04 | [사이드 프로젝트를 프로덕션에 올리기까지](blog-04-production-readiness.md) | 시크릿 관리, Flyway, Redis 캐시, Actuator 보안, 백업 | — |
| 7 | 07 | [사이드 프로젝트를 NAS에 자동 배포하기](blog-07-nas-auto-deployment.md) | GitHub Actions, GHCR, SSH, 카나리아 롤링 | PR #61, #95, #97 |
| 8 | 08 | [WebSocket 브로드캐스트가 실패해도 메시지는 살려야 한다](blog-08-websocket-broadcast-resilience.md) | TransactionTemplate, 실패 격리, 부분 성공 | PR #62 |
| 9 | 09 | [사이드 프로젝트에 모니터링 스택을 붙인 이유](blog-09-monitoring-stack.md) | Prometheus, Grafana, Loki, Zipkin, Alertmanager | PR #63~#67 |
| 10 | 10 | [k6로 채팅 앱 부하 테스트하기 — 그리고 성능 최적화](blog-10-k6-load-testing.md) | k6, WebSocket 부하, 커넥션 풀, 성능 튜닝 | PR #99, #100 |

---

## 백엔드 — 업그레이드와 CI 수정

| 순서 | # | 제목 | 핵심 키워드 | 이슈/PR |
|------|---|------|------------|---------|
| 11 | 11 | [Java 25를 쓰려면 Gradle 9가 필수다](blog-11-gradle-9-java25-upgrade.md) | Java 25, Gradle 9, Virtual Threads, JaCoCo, ArchUnit | #126, PR #125 |
| 12 | 12 | [로컬 올 그린, CI 올 레드 — 통합 테스트 20건 수정기](blog-12-ci-integration-test-fix.md) | Flyway+H2, SecurityFilterChain, TestConfiguration, Redis 레이스 컨디션 | PR #127, #130, #132 |
| 13 | 13 | [로컬 올 그린, CI 올 레드 — WebSocket 통합 테스트 수정기 2탄](blog-13-ci-integration-test-fix-2.md) | Testcontainers Redis, WebSocket 테스트, 최종 해결 | PR #137 |

---

## Flutter 프론트엔드

| 순서 | # | 제목 | 핵심 키워드 |
|------|---|------|------------|
| 14 | 14 | [Flutter에서도 Clean Architecture가 필요할까](blog-14-flutter-clean-architecture.md) | 4레이어, GetIt+Injectable DI, GoRouter 인증 가드, Cubit vs BLoC |
| 15 | 15 | [Flutter 실시간 채팅 — WebSocket Facade 패턴과 Optimistic UI](blog-15-flutter-realtime-websocket.md) | STOMP, Facade + 4 Manager, Exponential Backoff, Event Dedup, Optimistic UI |
| 16 | 16 | [Flutter UX 깊이 파기 — 테마, 생체인증, 오프라인 캐시](blog-16-flutter-ux-features.md) | Material 3 테마, 생체인증 Grace Period, Drift FTS5, copyWith 버그 |

---

## 배포 자동화

| 순서 | # | 제목 | 핵심 키워드 |
|------|---|------|------------|
| 17 | 17 | [Fastlane으로 iOS·macOS·Android 한 번에 배포하기](blog-17-fastlane-multiplatform.md) | 통합 Fastfile, ASC API Key, macOS 재서명, Google Play 트랙 |
| 18 | 18 | [Flutter CI/CD 설계하기 — GitHub Actions로 테스트부터 스토어 배포까지](blog-18-flutter-cicd-github-actions.md) | 재사용 워크플로우, 병렬 빌드, 시크릿 관리, 빌드 캐시, 비용 |

---

## 인프라 회고

| 순서 | # | 제목 | 핵심 키워드 |
|------|---|------|------------|
| 19 | 19 | [카나리아 3인스턴스에서 Blue-Green 단일 운영으로 — NAS CPU가 항복했다](blog-19-nas-bluegreen-rollback.md) | Celeron J4125, CPU 경합, profiles 조건부 기동, Blue-Green 회귀 |

---

## 읽기 가이드

### 처음부터 끝까지 따라가고 싶다면

위 순서(00 → 01 → 02 → ... → 18)대로 읽으면 된다. 실제 개발 순서 그대로다.

### 관심 분야만 골라 읽고 싶다면

| 관심사 | 추천 글 |
|--------|---------|
| **아키텍처 설계** | 01 → 02 → 14 |
| **실시간 채팅 구현** | 01 → 08 → 15 |
| **보안** | 04 → 05 |
| **캐시와 성능** | 06 → 10 |
| **인프라와 배포** | 04 → 07 → 09 → 17 → 18 → 19 |
| **Flutter 앱 개발** | 14 → 15 → 16 |
| **CI/CD 전체** | 07 → 17 → 18 |
| **트러블슈팅** | 03 → 11 → 12 → 13 |

---

## 기술 스택 요약

| 영역 | 기술 |
|------|------|
| **백엔드** | Java 25, Spring Boot 3.5.6, PostgreSQL 16, Redis 7, MinIO |
| **프론트엔드** | Flutter 3.8+, BLoC/Cubit, Drift(SQLite), STOMP WebSocket |
| **인프라** | Docker Compose, Nginx, Blue-Green 배포 (NAS), 카나리아 롤링 (확장 시) |
| **모니터링** | Prometheus, Grafana, Loki, Zipkin, Alertmanager |
| **CI/CD** | GitHub Actions, Fastlane, GHCR |
| **배포 대상** | NAS(백엔드), App Store(iOS/macOS), Google Play(Android) |

---

모든 코드는 [GitHub 저장소](https://github.com/with-co-talk/co-talk)에서 확인할 수 있다.
