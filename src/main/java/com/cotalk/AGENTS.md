<!-- Parent: ../../../../AGENTS.md -->
<!-- Generated: 2026-02-23 | Updated: 2026-02-23 -->

# com.cotalk - 메인 소스

## 개요
Co-Talk 애플리케이션의 메인 소스 코드. 헥사고날 아키텍처(Ports & Adapters) 기반으로 4개 계층으로 분리.

## 계층 구조

```
[클라이언트] ─── HTTP/WebSocket ───→ [adapter/inbound]
                                          │
                                    [domain/port/inbound] (UseCase 인터페이스)
                                          │
                                    [application/service] (UseCase 구현체)
                                          │
                                    [domain/port/outbound] (Repository/서비스 포트)
                                          │
              ┌───────────────────────────┼───────────────────────────┐
        [adapter/outbound]         [infrastructure]             [infrastructure]
        (JPA 어댑터 → DB)      (Redis/FCM/MinIO 등)         (Security/Config)
```

## 디렉토리
| 디렉토리 | 용도 |
|-----------|------|
| `adapter/` | 외부 인터페이스 어댑터 (REST, WebSocket, JPA) (`adapter/AGENTS.md` 참조) |
| `application/` | 유스케이스 구현 서비스 (~65개) (`application/AGENTS.md` 참조) |
| `domain/` | 도메인 엔티티, 포트, 예외, 검증기 (`domain/AGENTS.md` 참조) |
| `infrastructure/` | 횡단 관심사 (보안, 메시징, 암호화 등) (`infrastructure/AGENTS.md` 참조) |

## 주요 파일
| 파일 | 설명 |
|------|------|
| `CoTalkApplication.java` | Spring Boot 메인 클래스 |

## AI 에이전트 가이드

### 의존 방향 (위반 시 ArchUnit 테스트 실패)
- `domain` → 어떤 외부 패키지도 의존 금지 (순수 Java + Jakarta Validation만)
- `application` → `domain`만 의존 가능
- `adapter` → `domain` + `application` 의존 가능
- `infrastructure` → 모든 패키지 의존 가능 (횡단 관심사)

### 엔티티 이중 계층 (분리 완료)
- 모든 도메인 엔티티가 **완전 분리**되었다: `<Name>`(순수 POJO, `DomainBaseEntity` 상속) + `<Name>JpaEntity`(persistence 전용, `BaseJpaEntity` 상속) + `<Name>Mapper`
- 도메인은 `jakarta.persistence`/`org.springframework` 프레임워크에 의존하지 않으며 ArchUnit으로 강제된다.

### 코드 컨벤션
- Java 25, Effective Java 준수
- JavaDoc 필수 (public 클래스, 메서드, 필드)
- DTO는 Java record 사용
- 의존성 주입은 생성자 주입

<!-- MANUAL: -->
