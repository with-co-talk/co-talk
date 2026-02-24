<!-- Parent: ../../../../AGENTS.md -->
<!-- Generated: 2026-02-23 | Updated: 2026-02-23 -->

# 테스트 코드

## 개요
JUnit 5 + Mockito 기반. 단위/슬라이스/통합/아키텍처 4레벨 테스트. JaCoCo 클래스당 60% 커버리지 강제.

## 디렉토리
| 디렉토리 | 용도 |
|-----------|------|
| `domain/` | 도메인 엔티티/예외/검증기 단위 테스트 (~15개) |
| `application/service/` | 서비스 단위 테스트 (~30개, Mockito) |
| `adapter/inbound/rest/` | REST 컨트롤러 슬라이스 테스트 (27개, @WebMvcTest) |
| `adapter/inbound/rest/dto/` | DTO 검증 테스트 |
| `adapter/outbound/persistence/` | 영속성 어댑터 슬라이스 테스트 (15개, @DataJpaTest) |
| `infrastructure/` | 인프라 컴포넌트 단위 테스트 (~40개) |
| `integration/` | 전체 흐름 통합 테스트 (@SpringBootTest) |
| `architecture/` | ArchUnit 아키텍처 규칙 검증 |
| `common/fixture/` | 테스트 픽스처 (ChatRoomTestFixture, MessageTestFixture) |
| `config/` | 테스트 전용 설정 (TestRedisConfiguration) |

## 주요 파일

### 공통 설정
| 파일 | 설명 |
|------|------|
| `config/TestRedisConfiguration.java` | Mock Redis 빈 모음 (RedissonClient, DistributedLock, RedisTemplate 등 @Primary) |
| `infrastructure/security/WithMockCustomUser.java` | 커스텀 Security 어노테이션 (`@WithMockCustomUser(userId=1L, role="USER")`) |
| `integration/IntegrationTestSecurityConfig.java` | 통합 테스트용 Security 설정 (모든 요청 허용, userId 파라미터 인증) |
| `common/fixture/ChatRoomTestFixture.java` | ChatRoom/ChatRoomMember 정적 팩토리 + 빌더 |
| `common/fixture/MessageTestFixture.java` | Message 정적 팩토리 + 빌더 (Reflection으로 createdAt 설정) |

### 테스트 프로파일
| 파일 | 용도 |
|------|------|
| `resources/application-test.yml` | 기본 프로파일. H2 인메모리, Flyway 비활성화, Redis/Firebase/MinIO 비활성화 |
| `resources/application-ratelimit-test.yml` | Rate limit 전용. 실제 Redis(localhost:6380) 연결 |

## AI 에이전트 가이드

### 테스트 작성 패턴

**서비스 단위 테스트:**
```java
@ExtendWith(MockitoExtension.class)
class XxxServiceTest {
    @Mock private SomeRepository repository;
    @InjectMocks private XxxService service;

    @Test
    void should_예상결과_when_조건() { ... }
}
```

**컨트롤러 슬라이스 테스트:**
```java
@WebMvcTest(value = XxxController.class, addFilters = false)
@Import(RateLimitTestConfiguration.class)
class XxxControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockitoBean private XxxUseCase useCase;

    @Test
    void should_예상결과_when_조건() { ... }
}
```

**영속성 슬라이스 테스트:**
```java
@DataJpaTest
@ActiveProfiles("test")
@Import({XxxRepositoryAdapter.class, XxxMapper.class, JpaAuditingConfig.class})
class XxxRepositoryAdapterTest { ... }
```

**통합 테스트:**
```java
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@Import(TestRedisConfiguration.class)
class XxxIntegrationTest { ... }
```

### 새 테스트 프로파일 추가 시 필수 설정
```yaml
spring:
  flyway:
    enabled: false
jwt:
  secret: test-secret-key-for-testing-purposes-only-1234567890
firebase:
  enabled: false
minio:
  enabled: false
app:
  encryption:
    key: dGhpc2lzYXRlc3RrZXlmb3JkZXZlbG9wbWVudG9ubHk=
    enabled: false
```

### 테스트 메서드 네이밍
`should_예상결과_when_조건` 형식 필수

<!-- MANUAL: -->
