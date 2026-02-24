# Cloudflare Tunnel 적용 계획서

**작성 일자:** 2026-02-24
**상태:** 구현 전 계획 단계
**프로젝트:** Co-Talk 백엔드 인프라

---

## 1. 개요

### 1.1 현황 분석

#### 현재 아키텍처의 문제점
- **IP 노출**: Synology DDNS 도메인(`co-talk.sgyj-dev.synology.me`)이 직접 NAS IP를 노출
- **보안 위험**: DDoS 공격에 취약, 서버 위치 파악 용이
- **포트 노출**: NAT 포트 매핑을 통한 직접 접속으로 추적 가능

#### 해결 방안
Cloudflare Tunnel을 도입하여 다음을 달성:
- NAS 서버 IP 완전 은닉
- Cloudflare Edge를 통한 DDoS 보호 및 웹 애플리케이션 방화벽(WAF)
- 아웃바운드 연결만 필요 (인바운드 포트 개방 불필요)

### 1.2 트래픽 흐름 변경

#### 현재 구조
```
클라이언트 → Synology DDNS → NAT 포트매핑 → NAS:18080 → Nginx → Spring Apps
                (IP 직접 노출)
```

#### 변경 후 구조
```
클라이언트 → Cloudflare Edge → cloudflared (outbound 터널) → Nginx → Spring Apps
           (글로벌 캐시)     (로컬 NAS에서 발신)
```

### 1.3 기대 효과

| 항목 | 효과 |
|------|------|
| **보안** | IP 은닉 + DDoS 방어 + WAF 적용 |
| **성능** | Cloudflare 글로벌 엣지 네트워크 캐싱 |
| **인프라** | 인바운드 포트 개방 불필요 → 방화벽 단순화 |
| **모니터링** | Cloudflare 대시보드에서 트래픽 분석 |

---

## 2. 구현 범위

### 2.1 변경 대상 파일 목록

| 번호 | 파일 경로 | 유형 | 변경 내용 |
|------|---------|------|----------|
| 1 | `docker-compose.nas.yml` | Docker | cloudflared 서비스 추가 |
| 2 | `docker/nginx/nginx.conf` | Nginx | CF 헤더 처리, 실제 IP 추출 |
| 3 | `src/main/java/com/cotalk/infrastructure/web/ClientIpResolver.java` | Java | 신규 생성 - IP 추출 통합 |
| 4 | `src/main/java/com/cotalk/infrastructure/ratelimit/RateLimitInterceptor.java` | Java | ClientIpResolver 적용 |
| 5 | `src/main/java/com/cotalk/adapter/inbound/rest/TermsController.java` | Java | ClientIpResolver 적용 |
| 6 | `src/main/java/com/cotalk/infrastructure/websocket/WebSocketConfig.java` | Java | Heartbeat 추가 |
| 7 | `.env.example` | Config | Cloudflare 설정 변수 추가 |
| 8 | `scripts/deploy.sh` | Script | cloudflared 헬스 체크 추가 |
| 9 | `src/test/java/com/cotalk/infrastructure/web/ClientIpResolverTest.java` | Test | 신규 생성 - 단위 테스트 |

### 2.2 비인프라 작업 (Cloudflare 대시보드)

이 계획서의 범위 외. 배포 전 수동 준비 필요:

| 순서 | 작업 | 담당 |
|------|------|------|
| 1 | 도메인을 Cloudflare DNS에 추가 | DevOps |
| 2 | 네임서버를 Cloudflare로 변경 | DevOps |
| 3 | Zero Trust > Tunnels에서 새 터널 생성 | DevOps |
| 4 | 터널 토큰 발급 및 `.env`에 저장 | DevOps |
| 5 | Public Hostname 설정: 새도메인 → `http://nginx:80` | DevOps |
| 6 | SSL/TLS 모드: "Full" 또는 "Full (strict)" | DevOps |

---

## 3. 상세 구현 계획

### 3.1 Step 1: Docker Compose 업데이트

**파일:** `/Users/nhn/Desktop/DEV/cursor-workspace/with-co-talk/co-talk/docker-compose.nas.yml`

#### 변경 내용

##### 1) `cloudflared` 서비스 추가

`nginx` 서비스 다음에 아래 내용 삽입:

```yaml
  # ===========================================
  # Cloudflare Tunnel (Secure Outbound Tunnel)
  # ===========================================
  cloudflared:
    image: cloudflare/cloudflared:latest
    container_name: cotalk-cloudflared
    restart: unless-stopped
    environment:
      - TUNNEL_TOKEN=${CLOUDFLARE_TUNNEL_TOKEN}
    command: tunnel run
    depends_on:
      - nginx
    networks:
      - cotalk-network
    healthcheck:
      test: ["CMD-SHELL", "curl -f http://localhost:3000/ready || exit 1"]
      interval: 10s
      timeout: 5s
      retries: 3
      start_period: 30s
```

##### 2) Nginx 포트 변경

**기존:**
```yaml
  nginx:
    ...
    ports:
      - "${APP_PORT:-18080}:80"
    ...
```

**변경:**
```yaml
  nginx:
    ...
    # cloudflared 사용 시 아래 주석 처리
    # ports:
    #   - "${APP_PORT:-18080}:80"
    # 로컬 테스트용 (롤백 시 복구)
    expose:
      - "80"
    ...
```

**이유:**
- Cloudflare Tunnel이 활성화되면 외부 포트 노출 불필요
- 롤백 시 주석 해제하면 즉시 복구 가능
- 로컬 테스트는 `docker exec cotalk-nginx curl http://localhost/` 등으로 가능

---

### 3.2 Step 2: Nginx 설정 업데이트

**파일:** `/Users/nhn/Desktop/DEV/cursor-workspace/with-co-talk/co-talk/docker/nginx/nginx.conf`

#### 변경 내용

##### 1) Cloudflare 실제 IP 매핑 추가

**위치:** `http {}` 블록 최상단 (서버 블록 전)

```nginx
http {
    # ========================================
    # Cloudflare 실제 클라이언트 IP 추출
    # ========================================
    map $http_cf_connecting_ip $real_client_ip {
        ""      $remote_addr;      # CF 헤더 없으면 remote_addr 사용
        default $http_cf_connecting_ip;  # Cloudflare가 설정한 실제 IP 우선
    }

    # 기존 설정들...
```

##### 2) Rate Limiting Zone 수정

**기존:**
```nginx
limit_req_zone $binary_remote_addr zone=api_limit:10m rate=10r/s;
limit_req_zone $binary_remote_addr zone=auth_limit:10m rate=5r/s;
limit_req_zone $binary_remote_addr zone=ws_limit:10m rate=3r/s;
```

**변경:**
```nginx
limit_req_zone $real_client_ip zone=api_limit:10m rate=10r/s;
limit_req_zone $real_client_ip zone=auth_limit:10m rate=5r/s;
limit_req_zone $real_client_ip zone=ws_limit:10m rate=3r/s;
```

**이유:** Nginx가 이제 `$remote_addr`(cloudflared의 로컬 주소)이 아닌 실제 클라이언트 IP로 rate limit 적용

##### 3) 프록시 헤더 추가

**위치:** 각 `upstream cotalk_*` 블록의 `proxy_set_header` 섹션

**기존:**
```nginx
proxy_set_header X-Real-IP $remote_addr;
```

**변경:**
```nginx
proxy_set_header X-Real-IP $real_client_ip;
proxy_set_header X-Forwarded-For $real_client_ip;
proxy_set_header X-Forwarded-Proto https;
```

**이유:**
- Spring Boot 애플리케이션이 실제 클라이언트 IP를 받을 수 있도록
- `X-Forwarded-Proto https` 추가 → HTTPS가 강제되었음을 알림 (리다이렉트 루프 방지)

##### 4) MinIO 프록시 추가 (선택 사항)

**위치:** `server {}` 블록 내 `/api/v1/` location 후

```nginx
    # ========================================
    # MinIO 파일 저장소 프록시
    # ========================================
    location /files/ {
        proxy_pass http://minio:9000/;
        proxy_set_header Host $host;
        proxy_set_header Authorization $http_authorization;
        proxy_buffering off;
        proxy_request_buffering off;
    }
```

**이유:** MinIO 포트(9000)를 외부에 노출하지 않고 프록시를 통해 접속

---

### 3.3 Step 3: ClientIpResolver 신규 생성

**파일:** `/Users/nhn/Desktop/DEV/cursor-workspace/with-co-talk/co-talk/src/main/java/com/cotalk/infrastructure/web/ClientIpResolver.java`

#### 파일 구조

현재 상황:
- `RateLimitInterceptor`: `X-Real-IP` 헤더 사용 (262-271줄)
- `TermsController`: `X-Forwarded-For` 헤더 사용 (127-133줄)
- **문제:** 두 곳의 IP 추출 로직이 서로 다름 + 일관성 부족

#### 해결책: 통합 IP 추출 유틸리티

```java
package com.cotalk.infrastructure.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

/**
 * HTTP 요청에서 클라이언트 IP 주소를 추출하는 유틸리티 클래스.
 *
 * Cloudflare Tunnel, 리버스 프록시(Nginx), 직접 연결 등 다양한 환경을 지원.
 *
 * IP 추출 우선순위:
 * 1. CF-Connecting-IP (Cloudflare Tunnel에서 설정)
 * 2. X-Real-IP (Nginx에서 설정)
 * 3. X-Forwarded-For (기타 프록시에서 설정)
 * 4. Remote Address (직접 연결)
 *
 * @since 1.0.0
 */
public final class ClientIpResolver {

    private ClientIpResolver() {
        // 정적 메서드만 제공하는 유틸리티 클래스
    }

    /**
     * HTTP 요청에서 클라이언트의 실제 IP 주소를 추출합니다.
     *
     * @param request 클라이언트 요청
     * @return 클라이언트 IP 주소 (null이 아님)
     */
    public static String resolve(HttpServletRequest request) {
        // 1순위: Cloudflare Tunnel
        String ip = getHeaderValue(request, "CF-Connecting-IP");
        if (ip != null) {
            return ip;
        }

        // 2순위: Nginx 리버스 프록시
        ip = getHeaderValue(request, "X-Real-IP");
        if (ip != null) {
            return ip;
        }

        // 3순위: 기타 프록시 (X-Forwarded-For는 쉼표 분리된 여러 IP 가능)
        ip = getHeaderValue(request, "X-Forwarded-For");
        if (ip != null) {
            // 첫 번째 IP만 추출 (클라이언트 측)
            return ip.split(",")[0].trim();
        }

        // 4순위: 직접 연결
        return request.getRemoteAddr();
    }

    /**
     * 요청 헤더에서 값을 추출하고, 빈 문자열이면 null 반환.
     *
     * @param request HTTP 요청
     * @param headerName 헤더 이름
     * @return 헤더 값 (없거나 빈 문자열이면 null)
     */
    private static String getHeaderValue(HttpServletRequest request, String headerName) {
        String value = request.getHeader(headerName);
        return StringUtils.hasText(value) ? value : null;
    }
}
```

#### 설계 특징

| 특징 | 설명 |
|------|------|
| **Utility 패턴** | 생성 불가능한 정적 클래스 (객체 생성 불필요) |
| **우선순위 명확** | CF-Connecting-IP → X-Real-IP → X-Forwarded-For → remoteAddr |
| **안전성** | null 체크 + trim 포함, X-Forwarded-For 파싱 안전 |
| **JavaDoc** | 사용법과 우선순위 명확 기술 |
| **테스트 용이** | 순수 함수 (의존성 없음) |

---

### 3.4 Step 4: ClientIpResolver 테스트

**파일:** `/Users/nhn/Desktop/DEV/cursor-workspace/with-co-talk/co-talk/src/test/java/com/cotalk/infrastructure/web/ClientIpResolverTest.java`

```java
package com.cotalk.infrastructure.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ClientIpResolver 테스트")
class ClientIpResolverTest {

    @Test
    @DisplayName("CF-Connecting-IP 헤더가 있으면 해당 값을 반환")
    void should_return_cf_connecting_ip_when_present() {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("CF-Connecting-IP", "203.0.113.1");
        request.addHeader("X-Real-IP", "192.0.2.1");
        request.setRemoteAddr("127.0.0.1");

        // When
        String result = ClientIpResolver.resolve(request);

        // Then
        assertThat(result).isEqualTo("203.0.113.1");
    }

    @Test
    @DisplayName("CF-Connecting-IP가 없으면 X-Real-IP를 반환")
    void should_return_x_real_ip_when_cf_absent() {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Real-IP", "192.0.2.1");
        request.addHeader("X-Forwarded-For", "198.51.100.1");
        request.setRemoteAddr("127.0.0.1");

        // When
        String result = ClientIpResolver.resolve(request);

        // Then
        assertThat(result).isEqualTo("192.0.2.1");
    }

    @Test
    @DisplayName("X-Real-IP가 없으면 X-Forwarded-For를 반환 (첫 번째 IP)")
    void should_return_first_ip_from_x_forwarded_for_when_real_ip_absent() {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "198.51.100.1, 198.51.100.2, 198.51.100.3");
        request.setRemoteAddr("127.0.0.1");

        // When
        String result = ClientIpResolver.resolve(request);

        // Then
        assertThat(result).isEqualTo("198.51.100.1");
    }

    @Test
    @DisplayName("모든 프록시 헤더가 없으면 remoteAddr을 반환")
    void should_return_remote_addr_when_all_headers_absent() {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.1");

        // When
        String result = ClientIpResolver.resolve(request);

        // Then
        assertThat(result).isEqualTo("203.0.113.1");
    }

    @Test
    @DisplayName("CF-Connecting-IP가 빈 문자열이면 X-Real-IP를 반환")
    void should_ignore_blank_cf_connecting_ip() {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("CF-Connecting-IP", "   ");
        request.addHeader("X-Real-IP", "192.0.2.1");
        request.setRemoteAddr("127.0.0.1");

        // When
        String result = ClientIpResolver.resolve(request);

        // Then
        assertThat(result).isEqualTo("192.0.2.1");
    }

    @Test
    @DisplayName("X-Forwarded-For의 공백을 정확히 처리")
    void should_trim_whitespace_in_x_forwarded_for() {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "  198.51.100.1  ,  198.51.100.2  ");
        request.setRemoteAddr("127.0.0.1");

        // When
        String result = ClientIpResolver.resolve(request);

        // Then
        assertThat(result).isEqualTo("198.51.100.1");
    }
}
```

#### 테스트 커버리지

| 시나리오 | 검증 내용 | 예상 결과 |
|---------|---------|---------|
| CF-Connecting-IP 존재 | 우선순위 1 적용 | CF IP 반환 |
| CF 없고 X-Real-IP 존재 | 우선순위 2 적용 | X-Real-IP 반환 |
| 둘 다 없고 X-Forwarded-For 존재 | 첫 번째 IP 추출 | 첫 번째 IP 반환 |
| 모두 없음 | 우선순위 4 적용 | remoteAddr 반환 |
| CF가 빈 문자열 | 무시 후 다음 우선순위 | X-Real-IP 반환 |
| X-Forwarded-For 공백 | trim 처리 | 정확한 IP 반환 |

---

### 3.5 Step 5: RateLimitInterceptor 업데이트

**파일:** `/Users/nhn/Desktop/DEV/cursor-workspace/with-co-talk/co-talk/src/main/java/com/cotalk/infrastructure/ratelimit/RateLimitInterceptor.java`

#### 변경 내용

**현재 구조 (272줄):**
```java
private String getClientIpAddress(HttpServletRequest request) {
    // 262-271줄
    String ip = request.getHeader("X-Real-IP");
    if (ip != null && !ip.isBlank()) {
        return ip;
    }
    return request.getRemoteAddr();
}
```

**변경:**

1) **메서드 삭제:** `getClientIpAddress()` 메서드 완전 제거 (262-271줄)

2) **호출부 수정:** 모든 `this.getClientIpAddress(request)` → `ClientIpResolver.resolve(request)`

3) **Import 추가:**
```java
import com.cotalk.infrastructure.web.ClientIpResolver;
```

**예시:**
```java
// 변경 전
String clientIp = this.getClientIpAddress(request);

// 변경 후
String clientIp = ClientIpResolver.resolve(request);
```

**영향도:**
- 기존 테스트 (`RateLimitInterceptorTest`) 4개 통과 확인 필요
- IP 추출 로직이 더 정확해짐 (Cloudflare 헤더 지원)

---

### 3.6 Step 6: TermsController 업데이트

**파일:** `/Users/nhn/Desktop/DEV/cursor-workspace/with-co-talk/co-talk/src/main/java/com/cotalk/adapter/inbound/rest/TermsController.java`

#### 변경 내용

**현재 구조 (134줄):**
```java
private String getClientIpAddress(HttpServletRequest request) {
    // 127-133줄
    String ip = request.getHeader("X-Forwarded-For");
    if (ip != null && !ip.isBlank()) {
        return ip.split(",")[0];
    }
    return request.getRemoteAddr();
}
```

**변경:**

1) **메서드 삭제:** `getClientIpAddress()` 메서드 완전 제거 (127-133줄)

2) **호출부 수정:** 모든 `getClientIpAddress(request)` → `ClientIpResolver.resolve(request)`

3) **Import 추가:**
```java
import com.cotalk.infrastructure.web.ClientIpResolver;
```

**예시:**
```java
// 변경 전
String ip = getClientIpAddress(request);

// 변경 후
String ip = ClientIpResolver.resolve(request);
```

**영향도:**
- 기존 테스트 (`TermsControllerTest`) 18개 통과 확인 필요
- IP 추출 로직이 더 견고해짐 (Cloudflare + X-Real-IP 지원)

---

### 3.7 Step 7: WebSocketConfig Heartbeat 설정

**파일:** `/Users/nhn/Desktop/DEV/cursor-workspace/with-co-talk/co-talk/src/main/java/com/cotalk/infrastructure/websocket/WebSocketConfig.java`

#### 문제 상황

Cloudflare Tunnel의 기본 idle timeout: **100초**

현재 WebSocket 설정에 heartbeat이 없으면:
- 100초 유휴 연결 → Cloudflare가 자동 종료
- 사용자가 메시지를 받지 못함

#### 해결책: Heartbeat 추가

**변경 위치:** `configureMessageBroker()` 메서드

**현재:**
```java
@Override
public void configureMessageBroker(MessageBrokerRegistry registry) {
    registry.enableSimpleBroker("/topic", "/queue");
    registry.setApplicationDestinationPrefixes("/app");
}
```

**변경:**
```java
@Override
public void configureMessageBroker(MessageBrokerRegistry registry) {
    registry.enableSimpleBroker("/topic", "/queue")
        .setHeartbeatValue(new long[]{25000, 25000})  // 25초 주기 heartbeat
        .setTaskScheduler(heartbeatScheduler());     // 스케줄러 지정
    registry.setApplicationDestinationPrefixes("/app");
}
```

**Heartbeat 설정:**
```java
/**
 * WebSocket heartbeat용 Task Scheduler.
 *
 * @return ThreadPoolTaskScheduler 설정된 스케줄러
 */
@Bean
public TaskScheduler heartbeatScheduler() {
    ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
    scheduler.setPoolSize(1);
    scheduler.setThreadNamePrefix("ws-heartbeat-");
    scheduler.setAwaitTerminationSeconds(30);
    scheduler.setWaitForTasksToCompleteOnShutdown(true);
    scheduler.initialize();
    return scheduler;
}
```

**Import 추가:**
```java
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
```

#### Heartbeat 동작 원리

| 항목 | 값 | 의미 |
|------|-----|------|
| **클라이언트 heartbeat** | 25초 | 클라이언트에서 25초마다 ping 전송 |
| **서버 heartbeat** | 25초 | 서버에서 25초마다 pong 응답 |
| **Cloudflare timeout** | 100초 | 유휴 상태가 100초 지나야 연결 종료 |
| **안전 마진** | 50초 | 25초 heartbeat × 2회 + 안전 마진 |

**결과:** 최소 50초의 안전 마진으로 연결 유지

**영향도:**
- 기존 테스트 (`WebSocketConfigTest`) 3개 통과 확인 필요
- CPU/메모리 오버헤드: 무시할 수준 (1개 스레드, 1초에 1회)

---

### 3.8 Step 8: .env.example 업데이트

**파일:** `/Users/nhn/Desktop/DEV/cursor-workspace/with-co-talk/co-talk/.env.example`

#### 변경 내용

**추가 위치:** 파일 말미 (기존 Redis, MinIO 설정 후)

```bash
# ========================================
# Cloudflare Tunnel (Optional)
# ========================================
# Cloudflare Zero Trust에서 생성한 터널 토큰
# https://one.dash.cloudflare.com/
# 세팅 > 터널 > 새 터널 생성 후 토큰 복사
CLOUDFLARE_TUNNEL_TOKEN=eyJhIjoiYTAxMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAiLCJ0IjoiMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMCIsInMiOiJUMWpLV3h...}

# 도메인 (Cloudflare에 등록한 도메인)
# Cloudflare Public Hostname 설정: 새도메인 → http://nginx:80
APP_DOMAIN=example.cloudflare.dev

# Cloudflare 사용 여부 (활성화 시 Nginx에서 포트 노출 제거)
USE_CLOUDFLARE_TUNNEL=true
```

#### 주석 추가 (기존 설정)

**기존:**
```bash
# NAS Synology DDNS (Cloudflare 사용 전)
APP_DOMAIN=co-talk.sgyj-dev.synology.me
APP_PORT=18080
```

**변경:**
```bash
# ========================================
# 도메인 및 포트 설정
# ========================================
# Cloudflare 사용 시: APP_DOMAIN만 필요 (APP_PORT는 내부용)
# Cloudflare 미사용 시: NAS DDNS 사용 (아래 주석 해제)

# [Cloudflare 사용]
APP_DOMAIN=co-talk-prod.cloudflare.dev

# [Cloudflare 미사용 - NAS DDNS 사용]
# APP_DOMAIN=co-talk.sgyj-dev.synology.me
# APP_PORT=18080

# 로컬 Nginx 포트 (docker-compose 내부)
APP_PORT=18080
```

---

### 3.9 Step 9: deploy.sh 스크립트 업데이트

**파일:** `/Users/nhn/Desktop/DEV/cursor-workspace/with-co-talk/co-talk/scripts/deploy.sh`

#### 변경 내용

**추가 위치:** 인프라 서비스 헬스 체크 섹션 (배포 시작 전)

```bash
# ========================================
# 인프라 서비스 사전 체크
# ========================================
echo "Checking infrastructure services..."

# 기존 체크들...
check_service "PostgreSQL" "postgres" 5432
check_service "Redis" "redis" 6379
check_service "MinIO" "minio" 9000

# Cloudflare Tunnel 체크 추가
if [[ "${USE_CLOUDFLARE_TUNNEL:-false}" == "true" ]]; then
    echo "Checking Cloudflare Tunnel..."
    docker ps | grep -q cotalk-cloudflared || {
        echo "WARNING: cloudflared container not found. Tunnel may not be configured."
    }
fi
```

**배포 완료 후 상태 확인:**

```bash
# ========================================
# 배포 완료 후 상태 확인
# ========================================
echo "Verifying deployment..."

# Nginx 헬스 체크
docker exec cotalk-nginx wget -q --spider http://localhost/nginx-health \
    && echo "✓ Nginx is healthy" \
    || echo "✗ Nginx health check failed"

# Cloudflared 헬스 체크 추가
if [[ "${USE_CLOUDFLARE_TUNNEL:-false}" == "true" ]]; then
    docker logs cotalk-cloudflared 2>&1 | grep -q "Connection.*registered" \
        && echo "✓ Cloudflare Tunnel connected" \
        || echo "⚠ Cloudflare Tunnel status unclear - check logs"
fi
```

#### 로그 확인 명령어 추가 (문서)

배포 후 troubleshooting 명령어:

```bash
# Cloudflare Tunnel 연결 상태 확인
docker logs cotalk-cloudflared | tail -20

# Nginx 프록시 로그 확인
docker logs cotalk-nginx | grep -E "CF-Connecting-IP|X-Real-IP"

# Rate limit이 올바르게 적용되었는지 확인
docker exec cotalk-nginx grep "limit_req_zone \$real_client_ip" /etc/nginx/nginx.conf
```

---

## 4. 테스트 전략

### 4.1 자동 테스트 (단위 + 통합)

#### 신규 테스트

| 테스트 | 클래스 | 케이스 수 | 검증 항목 |
|--------|--------|---------|---------|
| `ClientIpResolverTest` | `ClientIpResolverTest.java` | 6개 | CF/X-Real-IP/X-Forwarded-For 우선순위 |

#### 기존 테스트 (통과 확인)

| 테스트 | 클래스 | 케이스 수 | 영향 |
|--------|--------|---------|------|
| `RateLimitInterceptorTest` | 기존 | 4개 | IP 추출 메서드 변경 |
| `TermsControllerTest` | 기존 | 18개 | IP 추출 메서드 변경 |
| `WebSocketConfigTest` | 기존 | 3개 | Heartbeat 설정 추가 |

### 4.2 실행 방법

```bash
# 1. 전체 테스트 실행
./gradlew test

# 2. 특정 테스트만 실행
./gradlew test --tests ClientIpResolverTest
./gradlew test --tests RateLimitInterceptorTest
./gradlew test --tests TermsControllerTest
./gradlew test --tests WebSocketConfigTest

# 3. 테스트 + 커버리지
./gradlew test jacocoTestReport
```

### 4.3 예상 테스트 결과

```
ClientIpResolverTest ...................... 6/6 PASSED
RateLimitInterceptorTest .................. 4/4 PASSED
TermsControllerTest ....................... 18/18 PASSED
WebSocketConfigTest ....................... 3/3 PASSED
────────────────────────────────────────────────
Total: 31 tests, 31 passed, 0 failed
Coverage: ≥ 60% (JaCoCo 강제)
```

### 4.4 최종 검증 (배포 전)

```bash
# 모든 테스트 통과
./gradlew test --stacktrace

# 빌드 성공
./gradlew bootJar

# 특정 클래스 컴파일 확인
./gradlew compileJava
```

---

## 5. 사전 준비 (비코드)

### 5.1 Cloudflare 대시보드 작업

다음 작업은 이 계획서 범위 외. 배포 전 DevOps 팀에서 수행:

#### 1단계: 도메인 Cloudflare 등록

1. Cloudflare 대시보드 로그인 (https://dash.cloudflare.com)
2. "도메인 추가" 클릭
3. 도메인명 입력: `example.dev`
4. 무료 플랜 선택
5. 네임서버를 Cloudflare로 변경 (DNS 레지스트라 설정)
   ```
   noah.ns.cloudflare.com
   sara.ns.cloudflare.com
   ```

#### 2단계: Tunnel 생성

1. Zero Trust (https://one.dash.cloudflare.com) 로그인
2. 좌측 메뉴: "네트워크" → "Tunnels"
3. "Tunnel 만들기" 클릭
4. 이름: `co-talk-prod`
5. 커넥터 환경: `Docker` 선택
6. 토큰 표시 후 복사 → `.env`에 `CLOUDFLARE_TUNNEL_TOKEN` 저장

#### 3단계: Public Hostname 설정

1. Tunnel 상세 페이지
2. "Public Hostnames" 탭
3. "호스트명 추가" 클릭
4. 설정:
   - **Subdomain:** `co-talk-prod`
   - **Domain:** `example.dev` (선택)
   - **Type:** HTTP
   - **URL:** `http://nginx:80` (Docker 네트워크 내 nginx)
5. 저장

#### 4단계: SSL/TLS 설정

1. "SSL/TLS" 탭 (도메인 수준)
2. "Overview" 섹션
3. "Your SSL/TLS encryption mode": "Full" 또는 "Full (strict)" 선택
   - **Full:** Nginx ← → Cloudflare (모두 암호화)
   - **Full (strict):** Nginx 인증서 필수 (선택적)

---

### 5.2 환경 변수 설정

```bash
# 1. .env 파일 생성
cp .env.example .env

# 2. CLOUDFLARE_TUNNEL_TOKEN 설정
echo "CLOUDFLARE_TUNNEL_TOKEN=eyJhIjoiYTAxMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAiLCJ0IjoiMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMCIsInMiOiJUMWpLV3h...}" >> .env

# 3. 도메인 설정
sed -i 's/co-talk.sgyj-dev.synology.me/co-talk-prod.example.dev/' .env

# 4. Cloudflare Tunnel 활성화
echo "USE_CLOUDFLARE_TUNNEL=true" >> .env

# 5. 검증
grep CLOUDFLARE_TUNNEL_TOKEN .env  # 토큰 확인
grep APP_DOMAIN .env                # 도메인 확인
```

---

## 6. 배포 절차

### 6.1 배포 전 체크리스트

- [ ] Cloudflare Tunnel 생성 완료 (토큰 발급)
- [ ] Public Hostname 설정 완료 (nginx:80 연동)
- [ ] SSL/TLS 모드 "Full" 이상 설정
- [ ] `.env`에 `CLOUDFLARE_TUNNEL_TOKEN` 저장
- [ ] 모든 자동 테스트 통과 (`./gradlew test`)
- [ ] 코드 리뷰 완료
- [ ] 롤백 계획 검토

### 6.2 배포 단계

```bash
# 1. 전체 테스트 실행 (필수)
./gradlew test

# 2. 코드 빌드
./gradlew bootJar

# 3. Docker 이미지 빌드
docker build -t cotalk:cloudflare .

# 4. 기존 컨테이너 중지
docker-compose -f docker-compose.nas.yml down

# 5. 새 버전 시작
docker-compose -f docker-compose.nas.yml up -d

# 6. 헬스 체크 대기 (30초)
sleep 30

# 7. 배포 검증
./scripts/deploy.sh verify

# 8. 로그 확인
docker logs cotalk-cloudflared | tail -20
docker logs cotalk-nginx | tail -20
```

### 6.3 배포 후 검증

#### 트래픽 확인

```bash
# Cloudflare를 통한 요청 확인
curl -H "User-Agent: curl" https://co-talk-prod.example.dev/api/v1/health

# 응답 헤더에 Cloudflare 서명 확인
curl -I https://co-talk-prod.example.dev/api/v1/health | grep -i "cf-"
```

#### 로그 확인

```bash
# Nginx 실제 IP 처리 확인
docker logs cotalk-nginx | grep "real_client_ip"

# Rate limit 동작 확인 (X-Real-IP 기반)
docker exec cotalk-app-1 grep "RateLimitInterceptor" logs/spring.log

# Cloudflare 연결 상태
docker logs cotalk-cloudflared | grep "Connected"
```

#### Cloudflare 대시보드 확인

1. Zero Trust > Tunnels > 터널명
2. "Status" 확인: "Connected" 표시
3. "Activity" 탭: 최근 요청 로그 확인
4. Security > WAF: 규칙 적용 확인

---

## 7. 롤백 계획

### 7.1 롤백 조건

다음 중 하나 발생 시 롤백 실행:

- Cloudflare Tunnel 연결 실패 (60초 이상 지속)
- Nginx rate limiting 오작동
- WebSocket 연결 끊김 (25초 heartbeat 미동작)
- 실제 클라이언트 IP 추출 실패

### 7.2 롤백 절차

```bash
# 1. Nginx 포트 노출 복구
# docker-compose.nas.yml에서 주석 처리된 ports 복구
# 변경: expose: ["80"] → ports: ["${APP_PORT:-18080}:80"]

# 2. Cloudflared 컨테이너 중지
docker stop cotalk-cloudflared

# 3. 컨테이너 다시 시작
docker-compose -f docker-compose.nas.yml up -d

# 4. 헬스 체크 (60초)
sleep 60
docker logs cotalk-nginx

# 5. DNS 변경 대기 (TTL 기본 300초)
# .env에서 APP_DOMAIN을 Synology DDNS로 복구
# co-talk.sgyj-dev.synology.me
```

### 7.3 롤백 검증

```bash
# NAS 직접 접속 가능 확인
curl -H "User-Agent: curl" http://co-talk.sgyj-dev.synology.me:18080/api/v1/health

# IP 추출이 remoteAddr로 동작 (Cloudflare 헤더 없음)
docker logs cotalk-nginx | grep "remote_addr"
```

### 7.4 롤백 후 복구

Cloudflare 관련 코드는 그대로 유지 (하위 호환성):
- `ClientIpResolver`: CF 헤더 없으면 remoteAddr 자동 사용
- Nginx: `$real_client_ip` 매핑에서 CF 없으면 `$remote_addr` 사용
- WebSocket heartbeat: 추가되지만 미사용 (부작용 없음)

---

## 8. 타임라인 및 리소스

### 8.1 구현 일정

| 단계 | 작업 | 소요 시간 | 담당 |
|------|------|----------|------|
| 1 | Docker Compose 업데이트 | 30분 | Backend |
| 2 | Nginx 설정 업데이트 | 1시간 | Infra |
| 3 | ClientIpResolver 개발 | 1시간 | Backend |
| 4 | ClientIpResolver 테스트 | 45분 | Backend |
| 5 | RateLimitInterceptor 업데이트 | 30분 | Backend |
| 6 | TermsController 업데이트 | 30분 | Backend |
| 7 | WebSocketConfig 업데이트 | 1시간 | Backend |
| 8 | 환경 변수 설정 | 15분 | DevOps |
| 9 | 배포 스크립트 업데이트 | 30분 | DevOps |
| 10 | 통합 테스트 & 검증 | 1시간 | QA |
| 11 | 문서화 및 리뷰 | 1시간 | Tech Lead |
| **총합** | | **약 8시간** | |

### 8.2 필요 인원

| 역할 | 책임 | 일정 |
|------|------|------|
| **Backend Developer** | 코드 구현 (3-5) | 4시간 |
| **DevOps/Infra** | Docker, Nginx, 배포 스크립트 | 2시간 |
| **QA 테스터** | 테스트 실행 및 검증 | 1시간 |
| **Tech Lead** | 코드 리뷰 및 승인 | 1시간 |
| **DBA** | Cloudflare 대시보드 준비 (사전) | 1시간 |

---

## 9. 위험 요소 및 대응

### 9.1 주요 위험 요소

| 위험 | 심각도 | 확률 | 대응 |
|------|--------|------|------|
| Cloudflare 연결 실패 | **높음** | 중간 | 로그 확인, 토큰 재발급 |
| Nginx IP 추출 오류 | **높음** | 낮음 | 단위 테스트 충분 |
| WebSocket heartbeat 미동작 | **중간** | 낮음 | 모니터링, 로그 검사 |
| DNS 전파 지연 | **중간** | 중간 | TTL 낮춤 (1시간), 사전 설정 |
| Rate limit 오작동 | **높음** | 낮음 | 통합 테스트, 부하 테스트 |

### 9.2 대응 방안

#### Cloudflare 연결 실패

**증상:** `docker logs cotalk-cloudflared`에 "Connection failed" 표시

**대응:**
1. 토큰 유효성 확인 (Cloudflare 대시보드)
2. 토큰 재발급: Zero Trust > Tunnels > 터널명 > Connectors > 토큰 복사
3. `.env` 업데이트 및 컨테이너 재시작
4. 롤백 (토큰 복구 불가 시)

#### IP 추출 오류

**증상:** Rate limit이 실제 IP가 아닌 proxy IP로 적용

**대응:**
1. 로그 확인: `docker logs cotalk-nginx | grep "real_client_ip"`
2. 테스트 실행: `./gradlew test --tests ClientIpResolverTest`
3. Nginx 설정 검증: `docker exec cotalk-nginx nginx -t`
4. 스프링 애플리케이션 로그: `docker logs cotalk-app-1 | grep "ClientIpResolver"`

#### WebSocket 연결 끊김

**증상:** 클라이언트의 WebSocket 연결이 100초 후 자동 종료

**대응:**
1. Heartbeat 설정 확인: `WebSocketConfig.java` heartbeat value
2. 로그 확인: 클라이언트 ping/pong 메시지 추적
3. Cloudflare 타임아웃 설정 확인 (대시보드)
4. 필요시 heartbeat 주기 단축 (15초)

---

## 10. 모니터링 및 메트릭

### 10.1 모니터링 항목

| 항목 | 목표 | 측정 방법 | 빈도 |
|------|------|---------|------|
| **Tunnel 연결** | "Connected" 상태 유지 | `docker logs cloudflared` | 실시간 |
| **IP 추출 정확도** | 100% Cloudflare IP 감지 | Rate limit 로그 분석 | 매일 |
| **WebSocket 생존 시간** | 30분 이상 | 클라이언트 세션 통계 | 매주 |
| **Rate limit 적중률** | 정상 적용 | 공격 테스트 | 변경 후 |
| **응답 시간** | < 100ms 증가 없음 | APM 대시보드 | 매일 |

### 10.2 알림 설정

```bash
# Cloudflare Tunnel 다운 알림 (cron)
*/5 * * * * docker logs cotalk-cloudflared | grep -q "Connected" || send_alert "Tunnel down"

# Rate limit 비정상 알림
*/1 * * * * tail -100 /var/log/nginx/access.log | grep "limit_req" | wc -l | awk '$1 > 10 {system("send_alert Rate_limit_high"")}'
```

---

## 11. 참고 자료

### 11.1 공식 문서

- [Cloudflare Tunnel 문서](https://developers.cloudflare.com/cloudflare-one/connections/connect-applications/)
- [cloudflared Docker 이미지](https://hub.docker.com/r/cloudflare/cloudflared)
- [Nginx Real IP Module](http://nginx.org/en/docs/http/ngx_http_realip_module.html)
- [Spring Security with X-Forwarded Headers](https://spring.io/guides/topical/spring-security-architecture/)

### 11.2 프로젝트 문서

- `/Users/nhn/Desktop/DEV/cursor-workspace/with-co-talk/co-talk/CLAUDE.md` - 프로젝트 아키텍처
- `/Users/nhn/Desktop/DEV/cursor-workspace/with-co-talk/co-talk/docker-compose.nas.yml` - 현재 Docker Compose
- `/Users/nhn/Desktop/DEV/cursor-workspace/with-co-talk/co-talk/docker/nginx/nginx.conf` - Nginx 설정

### 11.3 관련 PR/Issues

- **관련 이슈:** Security - IP 노출 위험 (예상)
- **차단 이슈:** 없음 (독립적 구현 가능)

---

## 12. 부록

### 12.1 Cloudflare 용어집

| 용어 | 설명 |
|------|------|
| **Tunnel** | 로컬 애플리케이션과 Cloudflare 간의 아웃바운드 QUIC 연결 |
| **Public Hostname** | 터널을 통해 공개되는 도메인/서브도메인 |
| **cloudflared** | Cloudflare Tunnel을 관리하는 데몬 (Docker 컨테이너로 실행) |
| **CF-Connecting-IP** | Cloudflare가 추가하는 클라이언트 실제 IP 헤더 |
| **Zero Trust** | Cloudflare의 ID 및 접근 관리 플랫폼 |

### 12.2 테스트 실행 명령어

```bash
# 모든 테스트 실행
./gradlew test

# ClientIpResolver 테스트만 실행
./gradlew test --tests ClientIpResolverTest -v

# 특정 테스트 메서드만 실행
./gradlew test --tests ClientIpResolverTest.should_return_cf_connecting_ip_when_present

# 테스트 + 커버리지 리포트
./gradlew test jacocoTestReport

# 테스트 캐시 초기화 후 실행
./gradlew clean test
```

### 12.3 Docker 명령어

```bash
# Cloudflare Tunnel 로그 확인 (실시간)
docker logs -f cotalk-cloudflared

# Nginx 로그 확인
docker logs -f cotalk-nginx

# 특정 로그만 필터링
docker logs cotalk-cloudflared 2>&1 | grep "Connection\|error\|failed"

# 컨테이너 상태 확인
docker ps -a | grep -E "cloudflared|nginx"

# Cloudflare Tunnel 헬스 체크
docker exec cotalk-cloudflared curl -s http://localhost:3000/ready
```

---

## 13. 승인 및 서명

### 13.1 검토자

| 역할 | 이름 | 서명 | 날짜 |
|------|------|------|------|
| **Tech Lead** | (검토 필요) | [ ] | |
| **DevOps Manager** | (검토 필요) | [ ] | |
| **Backend Team Lead** | (검토 필요) | [ ] | |

### 13.2 구현 담당

| 항목 | 담당자 | 예상 완료 |
|------|--------|----------|
| 코드 구현 | Backend Team | |
| 테스트 검증 | QA Team | |
| 배포 | DevOps Team | |

---

**문서 버전:** 1.0
**마지막 수정:** 2026-02-24
**상태:** 구현 전 계획 (Ready for Review)
