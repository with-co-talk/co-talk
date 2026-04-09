# 사이드 프로젝트에 모니터링 스택을 붙인 이유

> Co-Talk 개발기 8편 — Prometheus + Grafana + Loki + Zipkin + Alertmanager

---

## "사이드 프로젝트에 모니터링까지?"

솔직히 처음엔 나도 그 생각이었다. 혼자 만드는 사이드 프로젝트에 모니터링 스택까지 붙이는 건 오버엔지니어링 아닌가 싶었다. 문제 생기면 서버에 로그인해서 `docker logs cotalk-app-1 | tail -100` 하면 되지 않나?

그런데 직접 해보니까 한계가 명확했다.

**로그인해서 `docker logs`로 보는 방식의 한계:**
- 문제가 발생한 시점의 로그를 찾으려면 직접 눈으로 스크롤해야 한다
- 3개 인스턴스를 동시에 띄워놓으면 어느 인스턴스에서 문제가 났는지도 모른다
- "메시지가 가끔 늦게 온다"는 제보를 받았는데, 재현도 안 되고 로그에도 ERROR가 없다
- WebSocket이 끊겼다는 피드백이 왔는데 언제, 얼마나 자주 끊겼는지 알 방법이 없다

실시간 채팅 앱에서 메시지 전달 실패나 WebSocket 끊김은 UX를 직접적으로 망친다. 장애가 나고 나서 사후에 분석하는 건 늦다. **배포 직후부터 정상 동작 여부를 확인할 수 있어야 한다.**

그래서 PR #63~#67에 걸쳐 모니터링 스택을 도입했다.

---

## 스택 구성

<!-- IMAGE: Grafana Co-Talk Overview 대시보드 — WebSocket 연결 수, 메시지 전송량, 5xx 에러율, JVM 힙 등 핵심 패널이 한눈에 보이는 대시보드 전체 스크린샷 (가장 임팩트 큰 이미지) -->

Co-Talk은 NAS 위에서 Docker Compose로 돌아가는 환경이다. 클라우드 managed 서비스를 쓸 수 없으니 모두 셀프호스팅이다.

```
┌─────────────────────────────────────────────────────┐
│                   모니터링 데이터 흐름                  │
│                                                     │
│  Spring Boot App (x3)                               │
│  ├── /actuator/prometheus ──────────► Prometheus    │
│  │   (메트릭 노출, 15초 주기 스크랩)       │             │
│  │                                  ▼             │
│  └── stdout/stderr ──► Promtail ──► Loki           │
│      (JSON 로그)        (Docker       (로그 저장소)   │
│                         소켓 감시)                   │
│                                                     │
│  Spring Boot App (x3)                               │
│  └── Micrometer Tracing ──────────► Zipkin          │
│      (Span 전송)                     (트레이스 저장)  │
│                                                     │
│  Prometheus ──────────────────────► Alertmanager    │
│  (alert-rules.yml 평가)               (이메일 발송)   │
│                                                     │
│  Grafana ◄──── Prometheus (메트릭)                   │
│           ◄──── Loki      (로그)                     │
│           ◄──── Zipkin    (트레이스)                  │
│           (포트 13001 → 외부 접근)                    │
└─────────────────────────────────────────────────────┘
```

| 컴포넌트 | 역할 | 포트 |
|---|---|---|
| Prometheus | 메트릭 수집 및 저장 | 내부 전용 |
| Grafana | 대시보드 시각화 | 13001 (외부 노출) |
| Loki | 로그 수집 및 검색 | 내부 전용 |
| Promtail | Docker 컨테이너 로그 수집 에이전트 | - |
| Zipkin | 분산 트레이싱 | 내부 전용 |
| Alertmanager | 알림 발송 (이메일/Slack) | 내부 전용 |

Grafana만 외부 포트를 열고, 나머지는 Docker 내부 네트워크(`cotalk-network`)로만 통신한다. Grafana가 단일 진입점이 되는 구조다.

---

## docker-compose.nas.yml 구성

핵심 서비스 설정만 발췌한다.

```yaml
# Prometheus — 메트릭 스크랩
prometheus:
  image: prom/prometheus:v2.48.0
  user: root  # NAS 볼륨 권한 문제로 root 필요 (삽질 1번)
  command:
    - '--config.file=/etc/prometheus/prometheus.yml'
    - '--storage.tsdb.path=/prometheus'
    - '--storage.tsdb.retention.time=15d'   # 15일치 보관
    - '--web.enable-lifecycle'
    - '--web.enable-remote-write-receiver'  # k6 부하테스트 메트릭도 수신
  volumes:
    - ./docker/prometheus:/etc/prometheus
    - prometheus-data:/prometheus

# Grafana — 대시보드
grafana:
  image: grafana/grafana:10.2.2
  user: root  # 마찬가지
  ports:
    - "${GRAFANA_PORT:-13001}:3000"
  environment:
    - GF_USERS_ALLOW_SIGN_UP=false
  volumes:
    - ./docker/grafana/provisioning:/etc/grafana/provisioning
    - ./docker/grafana/dashboards:/var/lib/grafana/dashboards
    - grafana-data:/var/lib/grafana

# Loki — 로그 저장소
loki:
  image: grafana/loki:2.9.2
  user: root
  command: -config.file=/etc/loki/local-config.yaml

# Promtail — Docker 로그 수집 에이전트
promtail:
  image: grafana/promtail:2.9.2
  user: root
  volumes:
    - ./docker/promtail/promtail-config.yml:/etc/promtail/config.yml
    - /var/run/docker.sock:/var/run/docker.sock:ro
    # Synology NAS의 Docker 루트 디렉토리 (환경마다 다름)
    - ${DOCKER_ROOT_DIR:-/volume1/@docker}/containers:/var/lib/docker/containers:ro

# Zipkin — 분산 트레이싱
zipkin:
  image: openzipkin/zipkin:latest
  environment:
    - STORAGE_TYPE=mem  # 사이드 프로젝트라 메모리 저장으로 충분

# Alertmanager — 알림
alertmanager:
  image: prom/alertmanager:v0.26.0
  user: root
  environment:
    - SMTP_AUTH_PASSWORD=${SMTP_AUTH_PASSWORD}
```

---

## 삽질 1: NAS Docker 볼륨 권한 문제 (PR #64)

모니터링 서비스를 처음 띄웠을 때 Prometheus, Grafana, Loki가 모두 데이터 디렉토리에 쓰기를 못해서 죽었다.

```
level=error msg="opening storage failed" err="open /prometheus/queries.active: permission denied"
```

Prometheus는 기본적으로 `nobody` 사용자(UID 65534)로 실행된다. Grafana는 UID 472. Loki는 UID 10001. 각 컨테이너마다 실행 UID가 다르고, NAS의 볼륨 마운트 경로 권한이 root:root로 잡혀 있으니 당연히 쓰기 권한이 없다.

**시도 1: 볼륨 디렉토리 권한을 직접 변경**
```bash
# NAS에서
chown -R 65534:65534 /path/to/prometheus-data
chown -R 472:472 /path/to/grafana-data
```
컨테이너마다 UID가 다르니 관리가 너무 번거롭다.

**시도 2: `user: "1000:1000"` 고정**
NAS의 관리자 계정 UID/GID를 맞춰봤지만 NAS Docker 환경마다 달라서 재현이 안 됐다.

**최종 해결: `user: root`**

```yaml
prometheus:
  user: root

grafana:
  user: root

loki:
  user: root

promtail:
  user: root

alertmanager:
  user: root
```

프로덕션 환경에서는 `user: root`로 컨테이너를 실행하는 건 보안상 비권장이다. 그런데 이건 NAS에서 돌아가는 사이드 프로젝트다. 외부에서 모니터링 포트에 직접 접근도 안 되고, 모니터링 데이터가 유출되더라도 치명적이지 않다. **실용성 > 이론적 완벽함**이 맞다고 판단했다.

---

<!-- IMAGE: Prometheus Targets 페이지 — http://localhost:9090/targets 화면에서 cotalk-app (app-1, app-2, app-3) 타겟이 모두 State=UP인 스크린샷 -->

## Prometheus 스크랩 설정

Spring Boot Actuator의 `/actuator/prometheus` 엔드포인트를 5초 간격으로 스크랩한다.

```yaml
# docker/prometheus/prometheus.yml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

alerting:
  alertmanagers:
    - static_configs:
        - targets:
            - alertmanager:9093

rule_files:
  - /etc/prometheus/alert-rules.yml

scrape_configs:
  - job_name: 'cotalk-app'
    metrics_path: '/actuator/prometheus'
    scrape_interval: 5s
    static_configs:
      - targets: ['app-1:8080', 'app-2:8080', 'app-3:8080']
    relabel_configs:
      # app-1, app-2, app-3에서 instance_id 라벨 추출
      - source_labels: [__address__]
        regex: 'app-(.+):8080'
        target_label: instance_id
        replacement: '$1'
```

3개 인스턴스를 하나의 job으로 묶되, `instance_id` 라벨로 구분한다. Grafana에서 인스턴스별로 메트릭을 비교할 수 있다.

---

## 커스텀 비즈니스 메트릭 (PR #67)

JVM 메트릭(힙 메모리, GC 시간, 스레드 수)은 기본 제공이지만, 채팅 앱에서 진짜 중요한 건 **비즈니스 메트릭**이다. "지금 몇 명이 접속해 있나", "메시지가 제대로 전달되고 있나"를 알아야 한다.

`CustomMetrics.java`는 `MetricsPort` 인터페이스를 구현해서 도메인 레이어와 완전히 분리했다. 도메인은 Micrometer에 의존하지 않는다.

```java
@Component
@Getter
public class CustomMetrics implements MetricsPort {

    // Counter: 단조 증가 카운터
    private final Counter messagesSentCounter;
    private final Counter messagesReceivedCounter;
    private final Counter loginSuccessCounter;
    private final Counter loginFailureCounter;
    private final Counter userRegistrationCounter;

    // Timer: 처리 시간 측정
    private final Timer messageProcessingTimer;

    // Gauge: 현재값 (증감 가능)
    private final AtomicLong activeWebSocketConnections = new AtomicLong(0);
    private final AtomicLong activeChatRooms = new AtomicLong(0);

    public CustomMetrics(MeterRegistry meterRegistry) {
        this.messagesSentCounter = Counter.builder("cotalk.messages.sent")
                .description("Total number of messages sent")
                .register(meterRegistry);

        this.loginSuccessCounter = Counter.builder("cotalk.auth.login.success")
                .description("Total number of successful logins")
                .register(meterRegistry);

        this.loginFailureCounter = Counter.builder("cotalk.auth.login.failure")
                .description("Total number of failed logins")
                .register(meterRegistry);

        // Gauge는 AtomicLong을 참조 — 컨테이너가 현재값을 pull해 간다
        Gauge.builder("cotalk.websocket.connections", activeWebSocketConnections, AtomicLong::get)
                .description("Current number of active WebSocket connections")
                .register(meterRegistry);

        Gauge.builder("cotalk.chatrooms.active", activeChatRooms, AtomicLong::get)
                .description("Current number of active chat rooms")
                .register(meterRegistry);

        this.messageProcessingTimer = Timer.builder("cotalk.messages.processing.time")
                .description("Time taken to process messages")
                .register(meterRegistry);
    }
}
```

**태그를 붙인 메트릭도 있다.** Redis Pub/Sub 발행과 WebSocket 전달은 성공/실패를 `result` 태그로 구분해서 기록한다.

```java
// Redis Pub/Sub 발행 결과 (type: message|reaction|event, result: success|failure)
public void recordRedisPublish(String type, boolean success) {
    meterRegistry.counter("cotalk.redis.publish",
            "type", type,
            "result", success ? "success" : "failure"
    ).increment();
}

// WebSocket 전달 결과
public void recordWebsocketDelivery(String type, boolean success) {
    meterRegistry.counter("cotalk.websocket.delivery",
            "type", type,
            "result", success ? "success" : "failure"
    ).increment();
}
```

메시지 하나가 전달되는 경로는 `App → Redis Publish → Redis Subscribe → WebSocket Delivery`다. 이 두 메트릭을 함께 보면 파이프라인 어느 단계에서 실패가 발생하는지 바로 알 수 있다.

### 수집하는 커스텀 메트릭 전체 목록

| 메트릭명 | 타입 | 의미 |
|---|---|---|
| `cotalk.messages.sent` | Counter | 전송된 메시지 수 |
| `cotalk.messages.received` | Counter | 수신된 메시지 수 |
| `cotalk.messages.processing.time` | Timer | 메시지 처리 소요 시간 |
| `cotalk.auth.login.success` | Counter | 로그인 성공 수 |
| `cotalk.auth.login.failure` | Counter | 로그인 실패 수 |
| `cotalk.users.registered` | Counter | 신규 가입자 수 |
| `cotalk.websocket.connections` | Gauge | 현재 활성 WebSocket 연결 수 |
| `cotalk.chatrooms.active` | Gauge | 현재 활성 채팅방 수 |
| `cotalk.redis.publish{type,result}` | Counter | Redis Pub/Sub 발행 성공/실패 |
| `cotalk.websocket.delivery{type,result}` | Counter | WebSocket 전달 성공/실패 |

---

## Loki + Promtail: 로그 수집

Promtail이 Docker 소켓을 통해 모든 컨테이너의 stdout/stderr를 감시하고 Loki로 보낸다.

```yaml
# docker/promtail/promtail-config.yml
scrape_configs:
  - job_name: cotalk-containers
    docker_sd_configs:
      - host: unix:///var/run/docker.sock
        refresh_interval: 5s
        filters:
          - name: label
            values: ["com.docker.compose.project"]
    relabel_configs:
      - source_labels: ['__meta_docker_container_name']
        regex: '/(.*)'
        target_label: 'container'
      # app-1, app-2, app-3을 job=cotalk-app으로 통일
      - source_labels: ['__meta_docker_container_label_com_docker_compose_service']
        regex: 'app-(1|2|3)'
        target_label: 'job'
        replacement: 'cotalk-app'
```

**로그 포맷은 두 가지 경로로 처리된다.**

`logback-spring.xml`을 보면 프로파일에 따라 다르게 동작한다.

```xml
<!-- docker,prod 프로파일: JSON 포맷으로 파일 기록 (Promtail이 수집) -->
<springProfile name="docker &amp; prod">
    <root level="WARN">
        <appender-ref ref="CONSOLE"/>  <!-- Promtail이 stdout 감시 -->
        <appender-ref ref="FILE"/>
        <appender-ref ref="JSON"/>     <!-- logstash-logback-encoder로 JSON 출력 -->
    </root>
</springProfile>

<!-- docker-monitoring 프로파일: Loki에 직접 Push -->
<springProfile name="docker-monitoring">
    <appender name="LOKI" class="com.github.loki4j.logback.Loki4jAppender">
        <http>
            <url>${LOKI_URL:-http://loki:3100/loki/api/v1/push}</url>
        </http>
        <format>
            <label>
                <pattern>app=${appName},host=${HOSTNAME},level=%level</pattern>
            </label>
            <!-- traceId, spanId를 로그에 포함 → Zipkin과 연계 가능 -->
            <message>
                <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%thread] [%X{traceId:-},%X{spanId:-}] %logger{36} - %msg%n</pattern>
            </message>
        </format>
    </appender>
</springProfile>
```

프로덕션(`docker,prod`)에서는 Promtail이 Docker stdout을 수집해서 Loki로 보낸다. `docker-monitoring` 프로파일은 앱이 직접 Loki에 푸시하는 방식인데, 개발 환경에서 Promtail 없이 테스트할 때 사용한다.

로그 패턴에 `%X{traceId:-},%X{spanId:-}`를 넣어두면 Zipkin trace ID가 로그에 찍힌다. Grafana에서 특정 trace ID로 로그를 검색하면 그 요청의 전체 흐름을 추적할 수 있다.

---

## 삽질 2: Alertmanager 환경변수 치환 (PR #65)

처음에 `alertmanager.yml`에 이메일 비밀번호를 환경변수로 넣으려 했다.

```yaml
# 이렇게 쓰면 동작 안 한다
global:
  smtp_auth_password: ${SMTP_AUTH_PASSWORD}
```

Alertmanager는 설정 파일에서 환경변수 치환을 지원하지 않는다. Prometheus나 다른 컴포넌트들은 `${}` 치환을 지원하는데, Alertmanager만 지원 안 한다. 공식 이슈도 오래전부터 열려 있었다.

몇 가지 방법을 시도했다.

**시도 1: `envsubst` 사용**
```yaml
# docker-compose에서 envsubst로 치환
command: /bin/sh -c "envsubst < /etc/alertmanager/alertmanager.yml | alertmanager --config.file=/dev/stdin"
```
`/dev/stdin`으로 설정을 전달하는 방식인데, Alertmanager가 설정 파일 경로를 기대하는 곳에 stdin을 넣으면 reload 기능이 동작하지 않는다.

**최종 해결: 런타임에 파일 복사 후 sed 치환**

```yaml
alertmanager:
  environment:
    - SMTP_AUTH_PASSWORD=${SMTP_AUTH_PASSWORD}
  entrypoint: ["/bin/sh", "-c"]
  command:
    - |
      cp /etc/alertmanager/alertmanager.yml /tmp/alertmanager.yml
      sed -i "s|__SMTP_AUTH_PASSWORD__|$$SMTP_AUTH_PASSWORD|g" /tmp/alertmanager.yml
      exec /bin/alertmanager --config.file=/tmp/alertmanager.yml --storage.path=/alertmanager
```

설정 파일에는 `__SMTP_AUTH_PASSWORD__` 플레이스홀더를 쓰고, 컨테이너 시작 시 sed로 실제 값으로 치환한 임시 파일을 만들어서 그걸 사용한다. 우아하지는 않지만 동작은 확실하다.

설정 파일에는 플레이스홀더가 남아 있고 주석으로 설명을 달아놨다.

```yaml
# alertmanager.yml
# Alertmanager는 환경변수 치환을 지원하지 않습니다.
# docker-compose에서 sed로 __SMTP_AUTH_PASSWORD__를 자동 치환합니다.
global:
  smtp_auth_password: '__SMTP_AUTH_PASSWORD__'
```

---

## Alert Rules: 무엇을 감지하나

`alert-rules.yml`에 정의한 알림 규칙들이다. severity 3단계로 구분한다.

**critical — 즉시 대응 필요**

```yaml
# 3인스턴스 중 2개 이상 다운
- alert: MultipleInstancesDown
  expr: count(up{job="cotalk-app"} == 0) >= 2
  for: 30s

# HikariCP 커넥션 풀 90% 소진
- alert: DatabaseConnectionPoolExhausted
  expr: hikaricp_connections_active / hikaricp_connections_max > 0.9
  for: 2m

# 메시지 전달 파이프라인 실패율 10% 초과
- alert: MessageDeliveryPipelineDown
  expr: |
    sum(rate(cotalk_redis_publish_total{result="failure"}[5m]))
    / sum(rate(cotalk_redis_publish_total[5m])) > 0.1
    or
    sum(rate(cotalk_websocket_delivery_total{result="failure"}[5m]))
    / sum(rate(cotalk_websocket_delivery_total[5m])) > 0.1
  for: 1m
```

**warning — 주의 필요, 2시간 내 대응**

```yaml
# 5xx 에러율 5% 초과
- alert: HighErrorRate
  expr: |
    sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m]))
    / sum(rate(http_server_requests_seconds_count[5m])) > 0.05

# JVM 힙 메모리 85% 초과
- alert: HighMemoryUsage
  expr: jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} > 0.85

# 인스턴스 간 메시지 전달량 50% 이상 불균형
# → 특정 인스턴스에서 단방향 전달 실패 가능성
- alert: MessageDeliveryImbalance
  expr: |
    abs(
      sum by (instance) (rate(cotalk_websocket_delivery_total{type="message",result="success"}[5m]))
      - ignoring(instance) group_left
      avg(sum by (instance) (rate(cotalk_websocket_delivery_total{type="message",result="success"}[5m])))
    )
    / avg(...) > 0.5
```

`MessageDeliveryImbalance` 규칙이 특히 유용하다. 멀티 인스턴스 환경에서 Redis Pub/Sub이 한 방향만 실패하는 상황(특정 인스턴스만 구독 실패)을 감지할 수 있다.

---

<!-- IMAGE: Zipkin 트레이스 화면 — 메시지 전송 요청의 trace 상세 화면. MessageService.send(), Redis PUBLISH, DB INSERT span이 계층 구조로 보이는 화면 -->

## Zipkin: Redis Pub/Sub 경로 추적

`docker-compose.nas.yml`에서 앱 인스턴스에 `TRACING_ENABLED=true`와 `ZIPKIN_ENDPOINT`를 환경변수로 주입한다.

```yaml
app-1:
  environment:
    - TRACING_ENABLED=true
    - ZIPKIN_ENDPOINT=http://zipkin:9411/api/v2/spans
```

Spring Boot 3의 Micrometer Tracing이 자동으로 HTTP 요청마다 traceId를 생성하고 Zipkin에 전송한다.

채팅 메시지 하나가 처리되는 흐름을 Zipkin에서 보면:

```
[POST /api/v1/messages] → 47ms
  ├── MessageService.send()         → 12ms
  ├── Redis PUBLISH (chat:room:123) → 8ms
  └── DB INSERT (messages)          → 27ms
```

Redis Pub/Sub 구독 후 WebSocket 전달 부분은 비동기라 별도 trace로 잡힌다. traceId를 Loki 로그 검색에 넣으면 두 trace를 연결해서 볼 수 있다.

---

<!-- IMAGE: Alertmanager 알림 예시 — HighErrorRate 또는 MultipleInstancesDown 알림이 이메일/Slack으로 수신된 스크린샷 -->

## 전체적인 소감

모니터링 스택을 붙이면서 얻은 교훈 세 가지.

**1. 모니터링은 장애 전에 붙여야 한다**

장애가 난 다음에 "아, 모니터링 있었으면 바로 알았을 텐데"라고 후회하는 건 이미 늦다. 첫 배포 전에 붙여두는 게 맞다. 배포하고 나서 Grafana 대시보드에서 WebSocket 연결 수가 올라가는 걸 실시간으로 보는 건 나름 뿌듯하다.

**2. 비즈니스 메트릭이 JVM 메트릭보다 유용하다**

힙 메모리 사용률이나 GC 시간은 중요하지만, 내가 실제로 자주 보는 건 `cotalk.messages.sent`와 `cotalk.websocket.connections`다. "지금 몇 명이 채팅 중인가", "메시지가 정상적으로 전달되고 있는가"가 훨씬 직접적인 지표다. **비즈니스 메트릭을 먼저 설계하고 JVM 메트릭은 참고 지표로 활용하는 게 맞다.**

**3. Prometheus + Grafana 조합의 가성비**

설정 파일 몇 개 작성하고 `docker-compose up`하면 프로덕션급 모니터링이 올라온다. 메모리도 각 컨테이너 256MB 제한으로 충분히 돌아간다. 사이드 프로젝트에서도 이 정도 투자는 충분히 가치가 있다.

---

## 다음 편

9편에서는 k6를 이용한 부하 테스트와 Nginx 리버스 프록시 앞에서 실시간 채팅 앱이 어떻게 동작하는지를 다룰 예정이다. Prometheus의 `--web.enable-remote-write-receiver` 옵션도 여기서 활약한다.

---

*PR #63~#67 | 관련 파일: `docker-compose.nas.yml`, `docker/prometheus/`, `docker/alertmanager/`, `docker/loki/`, `docker/promtail/`, `src/main/java/com/cotalk/infrastructure/metrics/CustomMetrics.java`, `src/main/resources/logback-spring.xml`*
