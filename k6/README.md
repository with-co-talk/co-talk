# Co-Talk k6 부하 테스트 가이드

## 1. 개요

이 디렉토리는 Co-Talk 프로젝트의 **k6 부하 테스트(Load Testing)** 시나리오를 관리합니다.

### 목적

k6를 사용하여 Co-Talk 백엔드의 성능과 안정성을 검증합니다:

- **REST API 성능**: 응답 시간, 처리량 (throughput), 에러율 측정
- **WebSocket/STOMP 안정성**: 실시간 채팅 멀티 연결 테스트
- **시스템 한계점 파악**: 스트레스 및 스파이크 테스트로 최대 용량 확인
- **카나리아 배포 검증**: 새 버전 배포 시 안정성 확인

### Co-Talk 인프라 컨텍스트

```
클라이언트
    ↓
  nginx (로드밸런서)
    ↓
  [app-1 | app-2 | app-3] (3인스턴스, 각 768MB)
    ↓
PostgreSQL 16 (DB)
Redis 7 (캐시)
MinIO (S3 호환 스토리지)
```

- **환경**: NAS 기반 (8GB 메모리)
- **배포 전략**: 카나리아 롤링 배포 (app-1 → app-2 → app-3)
- **모니터링**: Prometheus + Grafana

---

## 2. 사전 준비

### 2.1 k6 설치

macOS (brew):
```bash
brew install k6
```

Linux (Ubuntu/Debian):
```bash
sudo apt-get update
sudo apt-get install -y apt-transport-https
curl https://dl.k6.io/key.pub | sudo apt-key add -
echo "deb https://dl.k6.io/deb stable main" | sudo tee /etc/apt/sources.list.d/k6-stable.list
sudo apt-get update
sudo apt-get install -y k6
```

Windows:
```bash
choco install k6
```

Docker (모든 OS):
```bash
docker run -i grafana/k6 run - < k6/scenarios/rest-api.js
```

설치 확인:
```bash
k6 version
```

### 2.2 Co-Talk 서버 실행

**로컬 테스트:**
```bash
# 프로젝트 루트에서
./gradlew bootRun
# 또는
java -jar build/libs/co-talk-*.jar
```

**NAS 테스트:**
```bash
# NAS 서버에서
docker compose up -d
```

서버 헬스체크:
```bash
# 로컬
curl -X GET http://localhost:8080/actuator/health

# NAS
curl -X GET http://<NAS_IP>:18080/actuator/health
```

### 2.3 환경변수 설정 (선택사항)

`.env` 파일 생성:
```bash
# 로컬 환경
BASE_URL=http://localhost:8080
WS_URL=ws://localhost:8080

# NAS 환경
BASE_URL=http://your-nas-ip:18080
WS_URL=ws://your-nas-ip:18080

# 테스트 계정 (기본값으로도 가능)
TEST_EMAIL=loadtest
TEST_PASS=Test1234!@

# 테스트 규모
VUS=10
DURATION=1m
```

---

## 3. 디렉토리 구조

```
k6/
├── README.md                    # 이 파일
├── config.js                    # 공유 설정 (BASE_URL, VUS, 임계치)
├── helpers/
│   ├── auth.js                  # 인증 헬퍼 (회원가입, 로그인, 토큰 갱신)
│   └── websocket.js             # WebSocket/STOMP 헬퍼 (프레임 생성, 파싱)
└── scenarios/
    ├── rest-api.js              # REST API 시나리오
    ├── websocket-chat.js        # WebSocket 채팅 시나리오
    ├── full-flow.js             # 전체 사용자 여정 (통합 테스트)
    ├── stress.js                # 스트레스 테스트
    └── spike.js                 # 스파이크 테스트 (급증 대응성)
```

### 파일 역할 상세 설명

#### config.js
- 모든 시나리오가 공유하는 설정
- `BASE_URL`, `WS_URL`, `API_PREFIX`, 테스트 사용자 생성 로직
- 기본 임계치 (`http_req_duration`, `http_req_failed`) 정의
- 환경변수로 오버라이드 가능

#### helpers/auth.js
- `signup(vuId)`: 테스트 사용자 회원가입 (이미 존재하면 409 무시)
- `login(vuId)`: 로그인 후 JWT 토큰 반환
- `setupUser(vuId)`: setup 단계에서 회원가입 + 로그인
- `refreshToken(token)`: 토큰 갱신

#### helpers/websocket.js
- STOMP 프로토콜 구현 (CONNECT, SUBSCRIBE, SEND, DISCONNECT)
- `stompConnect()`: STOMP CONNECT 프레임 생성
- `stompSubscribe()`: 채팅방, 알림 큐 구독
- `stompSend()`: 메시지 전송
- `connectStomp()`: WebSocket 연결 헬퍼 (자동 STOMP 핸드셰이크)
- Custom metrics: `ws_messages_sent`, `ws_messages_received`, `ws_message_latency`

#### scenarios/rest-api.js
- **목적**: REST API의 기본 성능 테스트
- **대상**: 프로필 조회, 친구 목록, 채팅방 목록, 메시지 조회, 사용자 검색
- **패턴**: Ramp-up → Steady → Spike → Recovery → Ramp-down

#### scenarios/websocket-chat.js
- **목적**: WebSocket 멀티 연결 및 실시간 채팅 안정성 테스트
- **특징**: STOMP 프레임으로 메시지, 타이핑 상태, 프레즌스 핑 전송
- **패턴**: 사용자 쌍(pair)으로 채팅방 생성 후 양방향 메시지 교환

#### scenarios/full-flow.js
- **목적**: 실제 사용자 여정 재현 (가장 현실적)
- **단계**: 회원가입 → 로그인 → 프로필/친구 조회 → 채팅방 생성 → WebSocket 채팅
- **특징**: REST API와 WebSocket 혼합 테스트

#### scenarios/stress.js
- **목적**: 시스템 한계점 찾기
- **VU 증가**: 10 → 30 → 50 → 100 (각 단계 2분 유지)
- **관찰**: 응답 시간/에러율이 급증하는 지점 파악

#### scenarios/spike.js
- **목적**: 갑작스러운 트래픽 급증에 대한 시스템 복원력 테스트
- **패턴**: 기본 부하 → 10초에 80 VU 급증 → 1분 유지 → 복구 관찰
- **카나리아 배포 검증**: 새 인스턴스 추가/제거 시 응답 시간 변화 확인

---

## 4. 시나리오 상세 설명

### 4.1 REST API 시나리오 (rest-api.js)

#### 목적
Co-Talk REST API의 기본 성능을 측정합니다.

#### 테스트 흐름 (순차적)

1. **로그인** (setup): VUS 수의 2배 테스트 사용자 생성 후 로그인
2. **프로필 조회**: `GET /api/v1/users/me` (1초 대기)
3. **친구 목록**: `GET /api/v1/friends` (0.5초 대기)
4. **채팅방 목록**: `GET /api/v1/chat/rooms` (0.5초 대기)
5. **메시지 조회**: `GET /api/v1/chat/rooms/{id}/messages` (0.5초 대기)
6. **사용자 검색**: `GET /api/v1/users/search?keyword=...` (1초 대기)
7. **채팅방 생성**: `POST /api/v1/chat/rooms` (10% 확률, 0.5초 대기)
8. **메시지 전송**: `POST /api/v1/chat/messages` (20% 확률, 1초 대기)

#### VU 패턴 (Ramp-up)

```
VU
100|       ╱╲        ╱╖
   |      ╱  ╲      ╱  ╖
 50|     ╱    ╲    ╱    ╖
   |    ╱      ╲  ╱      ╖
  0|───0:30   1:30  2:00  2:30

Stages:
- 0-30s:    0 → VUS (ramp-up)
- 30s-DURATION: VUS (steady)
- DURATION:     VUS → VUS*2 (spike)
- +30s:         VUS*2 → VUS (recover)
- +15s:         VUS → 0 (ramp-down)
```

#### 주요 체크 항목

```javascript
// 임계치 (thresholds)
http_req_duration: ['p(95)<500', 'p(99)<1000']
// 95 percentile 응답 시간이 500ms 이하
// 99 percentile 응답 시간이 1초 이하

http_req_failed: ['rate<0.05']
// 에러율이 5% 이하
```

#### 실행 예시

```bash
# 기본값 (VUS=10, DURATION=1m)
k6 run k6/scenarios/rest-api.js

# 커스텀 (NAS, VUS=50, DURATION=5m)
k6 run \
  --env BASE_URL=http://your-nas-ip:18080 \
  --env VUS=50 \
  --env DURATION=5m \
  k6/scenarios/rest-api.js

# 결과를 JSON으로 저장
k6 run \
  --out json=results/rest-api-$(date +%Y%m%d-%H%M%S).json \
  k6/scenarios/rest-api.js
```

---

### 4.2 WebSocket 채팅 시나리오 (websocket-chat.js)

#### 목적
WebSocket/STOMP를 통한 실시간 채팅의 멀티 연결 안정성을 테스트합니다. 특히 Redis Pub/Sub 브로드캐스트가 정상 작동하는지 확인합니다.

#### 테스트 흐름

1. **setup 단계**:
   - VUS 수만큼 테스트 사용자 생성 및 로그인
   - 인접한 사용자 쌍(pair)으로 채팅방 생성 (예: VU1↔VU2, VU3↔VU4, ...)

2. **각 VU 동작**:
   - WebSocket STOMP 연결 (60초 유지)
   - 채팅방 구독: `/topic/chat/{roomId}`
   - 알림 큐 구독: `/user/queue/notifications`
   - 채팅방 목록 업데이트 구독: `/user/queue/chat-list`

3. **메시지 주기**:
   - **3초마다**: 메시지 전송 (`/app/chat/message`)
   - **10초마다**: 타이핑 상태 전송/해제 (`/app/chat/typing`)
   - **15초마다**: 프레즌스 핑 (`/app/chat/presence`)

#### VU 패턴 (Ramp-up)

```
VU
50|    ╱─────╖
  |   ╱      │ ╖
 25|  ╱      │  ╖
  |         └┘
  0|───0:20  1:20  1:30

Stages:
- 0-20s:    0 → VUS (ramp-up)
- 20s-DURATION: VUS (steady, 각 VU는 60초 세션 유지)
- DURATION: VUS → 0 (ramp-down)
```

#### 주요 체크 항목

```javascript
// 기본 HTTP 임계치
http_req_duration: ['p(95)<500']
http_req_failed: ['rate<0.05']

// WebSocket 커스텀 메트릭
ws_messages_sent: ['count>0']
ws_messages_received: ['count>0']
ws_message_latency: ['p(95)<200']  // 메시지 전송 후 수신 시간 <200ms
```

#### 메시지 흐름 분석

```
VU1 (3초마다 메시지) ──→ Backend
                          ↓
                    Redis Pub/Sub
                    (브로드캐스트)
                          ↓
                    VU2 (구독 중)
                          ↓
                    ws_messages_received.add(1)
```

#### 실행 예시

```bash
# 로컬 테스트
k6 run k6/scenarios/websocket-chat.js

# NAS 테스트 (VUS=20)
k6 run \
  --env BASE_URL=http://your-nas-ip:18080 \
  --env WS_URL=ws://your-nas-ip:18080 \
  --env VUS=20 \
  k6/scenarios/websocket-chat.js

# 출력 상세 보기
k6 run -v k6/scenarios/websocket-chat.js
```

---

### 4.3 전체 사용자 여정 시나리오 (full-flow.js)

#### 목적
실제 사용자가 앱을 사용하는 과정을 시뮬레이션합니다. 가장 현실적이고 통합적인 테스트입니다.

#### 테스트 흐름 (단계별)

```
01. Signup (회원가입)
    └─ POST /auth/signup
       (이미 존재하면 409 무시)

    sleep 0.5s

02. Login (로그인)
    └─ POST /auth/login
       → accessToken, userId 획득

    sleep 1s

03. Browse (브라우징)
    ├─ GET /users/me (프로필 조회)
    ├─ GET /friends (친구 목록)
    └─ GET /chat/rooms (채팅방 목록)

    sleep 1s

04. Create Room (채팅방 생성)
    ├─ GET /users/search (상대방 검색)
    └─ POST /chat/rooms (채팅방 생성)

    sleep 1s

05. WebSocket Chat (WebSocket 채팅)
    ├─ STOMP CONNECT
    ├─ SUBSCRIBE /topic/chat/{roomId}
    ├─ 5초마다 메시지 전송
    ├─ 30초 유지
    └─ DISCONNECT

    sleep 2s

06. Cleanup (정리)
    └─ GET /chat/rooms/{id}/messages (메시지 조회)

    sleep 1s
```

#### VU 패턴 (점진적 증가)

```
VU
100|      ╱─────────╖
  |     ╱           │ ╖
 50|   ╱            │  ╖
  |                 └┘
  0|──0:30  DURATION  +30s

Stages:
- 0-30s:          0 → VUS/2
- 30s-DURATION:   VUS/2 → VUS (점진적 증가)
- DURATION-+30s:  VUS → 0 (ramp-down)
```

전체 소요 시간: ~100-120초 (1 VU 기준)

#### 주요 체크 항목

그룹별 응답 시간 임계치:
```javascript
'group_duration{group:::01. Signup}': ['p(95)<2000']
'group_duration{group:::02. Login}': ['p(95)<1000']
'group_duration{group:::05. WebSocket Chat}': ['p(95)<60000']
```

#### 실행 예시

```bash
# 기본 실행 (VUS=10, DURATION=3m)
k6 run k6/scenarios/full-flow.js

# NAS 테스트 (카나리아 배포 후 검증)
k6 run \
  --env BASE_URL=http://your-nas-ip:18080 \
  --env WS_URL=ws://your-nas-ip:18080 \
  --env VUS=10 \
  --env DURATION=3m \
  k6/scenarios/full-flow.js

# 결과 저장 + 상세 로그
k6 run \
  --out json=results/full-flow-$(date +%Y%m%d-%H%M%S).json \
  -v \
  k6/scenarios/full-flow.js
```

---

### 4.4 스트레스 테스트 (stress.js)

#### 목적
VU를 단계적으로 증가시키며 시스템의 한계점(breaking point)을 찾습니다.

#### VU 단계별 목표

```
단계 1: VUS=10 (2분)     → Baseline 수립
        응답시간, CPU, 메모리 기준선

단계 2: VUS=30 (2분)     → 중간 부하
        성능 저하 시작점 확인?

단계 3: VUS=50 (2분)     → 높은 부하
        에러율 증가 시작?

단계 4: VUS=100 (2분)    → 스트레스 수준
        시스템 한계 확인

회복 단계 (2분)          → 부하 제거 후 복구 시간 관찰
```

#### 예상 그래프

```
응답 시간
 1000ms|                    ╱╲
       |                  ╱    ╲
  500ms|        ╱╲       ╱      ╲
       |      ╱    ╲   ╱        ╲
    0ms|────0     30  50  100      0 (VU)

에러율
  15% |                    ╱╲
      |                  ╱    ╲
   5% |      ─────────╱      ╲
      |                        ╲────
   0% |────────────────────────────0
```

#### 임계치

```javascript
http_req_duration: ['p(95)<2000', 'p(99)<5000']
// 95th percentile < 2초 (rest-api.js의 500ms보다 느슨함)

http_req_failed: ['rate<0.15']
// 에러율 15% 이하 (rest-api.js의 5%보다 높음)
```

#### 실행 예시

```bash
# 기본 스트레스 테스트 (MAX_VUS=100)
k6 run k6/scenarios/stress.js

# 더 높은 부하 (MAX_VUS=200)
k6 run --env MAX_VUS=200 k6/scenarios/stress.js

# NAS에서 실행
k6 run \
  --env BASE_URL=http://your-nas-ip:18080 \
  --env MAX_VUS=50 \
  k6/scenarios/stress.js
```

#### 분석 포인트

- **어느 VU에서 에러가 나타나는가?**: Rate limit 때문인지, 시스템 부하 때문인지?
- **응답 시간이 급증하는 시점**: 캐시 부족? DB 병목? 메모리 부족?
- **회복 패턴**: 부하 제거 후 몇 초 만에 정상화되는가?

---

### 4.5 스파이크 테스트 (spike.js)

#### 목적
갑작스런 트래픽 급증에 시스템이 얼마나 빠르게 대응하는지 테스트합니다. 카나리아 배포나 인스턴스 추가 시에 특히 중요합니다.

#### VU 패턴 (극단적 변화)

```
VU
100|          ╱──────╖
  |         ╱        │ ╖
 50|    ───           │  ───
  |   │                       │
  0|───────────────────────────
     warm  base SPIKE sustain drop recover ramp
     30s  60s  10s  60s   10s   60s   down
```

#### 단계 설명

1. **Warm-up** (30s, 5 VU): 시스템 워밍업
2. **Baseline** (1m, 5 VU): 기본 부하 시점 기록
3. **SPIKE** (10s, 5→80 VU): 급증! 16배 VU 증가
4. **Sustain** (1m, 80 VU): 스파이크 지속 관찰
5. **Drop** (10s, 80→5 VU): 급감
6. **Recovery** (1m, 5 VU): 복구 상태 관찰
7. **Ramp-down** (10s, 5→0 VU)

#### 모니터링 포인트

```
Spike 직후:
┌─────────────────────────┐
│ 응답 시간 얼마나 증가? │ ← CPU, 메모리 스파이크?
│ 에러율 얼마나 발생?     │ ← 429(rate limit)? 500 error?
│ 큐(queue) 쌓이나?      │ ← 스레드 풀 고갈?
└─────────────────────────┘

Drop 직후:
┌─────────────────────────┐
│ 응답 시간 얼마나 빨라? │ ← Graceful shutdown 동작?
│ 에러율 사라지나?        │ ← 깔끔한 복구?
│ 리소스 해제되나?        │ ← 메모리 누수 없나?
└─────────────────────────┘
```

#### 임계치

```javascript
http_req_duration: ['p(95)<3000']
// spike 중 최대 3초까지 허용 (느슨함)

http_req_failed: ['rate<0.10']
// 에러율 10% 이하
```

#### 실행 예시

```bash
# 기본 스파이크 테스트
k6 run k6/scenarios/spike.js

# NAS 카나리아 배포 후 검증
# (app-1 배포 후 실행)
k6 run \
  --env BASE_URL=http://your-nas-ip:18080 \
  --env VUS=5 \
  k6/scenarios/spike.js

# 결과 분석 (JSON 저장)
k6 run \
  --out json=results/spike-$(date +%Y%m%d-%H%M%S).json \
  k6/scenarios/spike.js
```

#### 예상 결과 분석

✅ **좋은 스파이크 응답**:
- Spike 동안 응답 시간이 2배 정도 증가
- 에러율 0-5% 미만
- Drop 후 1-2초 내 정상화

⚠️ **경고 신호**:
- Spike 동안 응답 시간이 10배 이상 증가
- 에러율 10% 이상
- Drop 후에도 응답 시간이 높게 유지 (메모리 누수?)

---

## 5. 실행 방법

### 5.1 로컬 테스트 (개발 환경)

```bash
# 1. Co-Talk 서버 시작
./gradlew bootRun

# 2. 다른 터미널에서 k6 테스트 실행
cd k6

# REST API 테스트 (권장: 먼저 실행)
k6 run scenarios/rest-api.js

# WebSocket 테스트
k6 run scenarios/websocket-chat.js

# 전체 여정 테스트
k6 run scenarios/full-flow.js

# 스트레스 테스트
k6 run scenarios/stress.js

# 스파이크 테스트
k6 run scenarios/spike.js
```

### 5.2 NAS 테스트 (배포 환경)

```bash
# NAS 서버에서
docker compose up -d

# 로컬 머신 또는 테스트 머신에서
k6 run \
  --env BASE_URL=http://<NAS_IP>:18080 \
  --env WS_URL=ws://<NAS_IP>:18080 \
  scenarios/rest-api.js

# 결과 저장
k6 run \
  --env BASE_URL=http://<NAS_IP>:18080 \
  --env VUS=20 \
  --env DURATION=5m \
  --out json=results/rest-api-nas-$(date +%Y%m%d-%H%M%S).json \
  scenarios/rest-api.js
```

### 5.3 Docker로 실행

```bash
# 이미지 빌드 (선택사항)
docker build -t k6-cotalk .

# 테스트 실행 (로컬 k6/scenarios 디렉토리 마운트)
docker run \
  -v $(pwd)/k6:/scripts \
  grafana/k6 run /scripts/scenarios/rest-api.js

# 환경변수 전달
docker run \
  -v $(pwd)/k6:/scripts \
  -e BASE_URL=http://host.docker.internal:8080 \
  grafana/k6 run /scripts/scenarios/rest-api.js
```

### 5.4 병렬 실행 (여러 시나리오 동시 테스트)

```bash
# 터미널 4개에서 동시 실행
k6 run scenarios/rest-api.js &
k6 run scenarios/websocket-chat.js &
k6 run scenarios/full-flow.js &
k6 run scenarios/stress.js &

wait
```

---

## 6. 환경변수 표

| 변수명 | 기본값 | 설명 | 예시 |
|--------|--------|------|------|
| `BASE_URL` | `http://localhost:8080` | REST API 서버 주소 | `http://your-nas:18080` |
| `WS_URL` | `ws://localhost:8080` | WebSocket 서버 주소 | `ws://your-nas:18080` |
| `TEST_EMAIL` | `loadtest` | 테스트 계정 이메일 prefix | `perftest` |
| `TEST_PASS` | `Test1234!@` | 테스트 계정 비밀번호 | `MyPassword123!` |
| `VUS` | `10` | 가상 사용자 수 | `50`, `100` |
| `DURATION` | `1m` | 테스트 지속 시간 | `5m`, `10m` |
| `MAX_VUS` | `100` | 스트레스 테스트의 최대 VU | `200` |

#### 실행 예시

```bash
# 환경변수 전달 (--env 사용)
k6 run \
  --env BASE_URL=http://10.0.0.100:18080 \
  --env WS_URL=ws://10.0.0.100:18080 \
  --env VUS=30 \
  --env DURATION=5m \
  scenarios/rest-api.js

# .env 파일 사용 (k6는 기본 미지원, shell sourcing)
source .env
k6 run \
  --env BASE_URL=$BASE_URL \
  --env WS_URL=$WS_URL \
  scenarios/rest-api.js
```

---

## 7. 결과 분석 방법

### 7.1 k6 기본 출력 이해하기

k6 테스트 실행 후 콘솔 출력:

```
     data_received..................: 45 MB   1.2 MB/s
     data_sent.......................: 8.2 MB 220 KB/s
     http_req_blocked...............: avg=2.4ms  min=1.2ms max=145ms p(90)=3.2ms p(95)=4.1ms
     http_req_connecting............: avg=0.8ms  min=0.1ms max=42ms  p(90)=1.1ms p(95)=1.4ms
     http_req_duration..............: avg=142ms  min=8ms   max=1.3s  p(90)=234ms p(95)=356ms p(99)=847ms
     http_req_failed................: 2.3%    ⚠️
     http_req_receiving.............: avg=1.2ms  min=0.1ms max=12ms  p(90)=1.4ms p(95)=1.8ms
     http_req_sending...............: avg=0.5ms  min=0.1ms max=8ms   p(90)=0.6ms p(95)=0.8ms
     http_req_tls_handshaking.......: avg=0ms    min=0ms   max=0ms   p(90)=0ms   p(95)=0ms
     http_req_waiting...............: avg=140ms  min=6ms   max=1.3s  p(90)=231ms p(95)=353ms p(99)=845ms
     http_reqs........................: 2450    65.9 req/s
     iteration_duration.............: avg=5.2s   min=4.1s  max=8.9s
     iterations.......................: 490     13.2 iter/s
     vus..............................: 10      min=0     max=10
     vus_max...........................: 10      min=10    max=10

running (00m37s) ✓ 10/10 VU
```

### 7.2 핵심 지표 설명

#### 요청 성능

| 지표 | 설명 | 목표값 |
|------|------|--------|
| `http_req_duration` (avg) | 평균 응답 시간 | < 200ms |
| `http_req_duration` (p(95)) | 95% 요청이 이 시간 이하 | < 500ms |
| `http_req_duration` (p(99)) | 99% 요청이 이 시간 이하 | < 1000ms |
| `http_req_failed` | 실패한 요청 비율 | < 5% |
| `http_reqs` | 총 요청 수 | 높을수록 좋음 |

#### 처리량 (Throughput)

| 지표 | 계산식 | 의미 |
|------|--------|------|
| `http_reqs` (총) | 모든 요청 합계 | 시스템이 처리한 요청 |
| 초당 요청 수 | `http_reqs / test_duration` | 시스템의 처리 능력 (req/s) |

예: 100초에 2450개 요청 = **24.5 req/s**

#### 사용자 활동

| 지표 | 설명 |
|------|------|
| `vus` | 현재 활성 VU 수 |
| `vus_max` | 최대 VU 수 |
| `iterations` | 완료한 반복 수 |

### 7.3 실시간 모니터링 (--summary-export)

결과를 JSON으로 저장:

```bash
k6 run \
  --out json=results/test-$(date +%Y%m%d-%H%M%S).json \
  scenarios/rest-api.js
```

저장된 JSON 분석:

```bash
# jq로 HTTP 요청 실패만 추출
cat results/test-*.json | jq '.data.samples[] | select(.type=="http" and .data.error_code != null)'

# 평균 응답 시간 추출
cat results/test-*.json | jq '.metrics.http_req_duration'
```

### 7.4 성공/실패 판정

k6 결과 요약 후 임계치 확인:

```bash
# 통과
✓ http_req_duration: p(95)<500ms
✓ http_req_failed: rate<0.05

# 실패
✗ http_req_duration: p(95)=652ms (p(95)<500ms threshold exceeded)
✗ http_req_failed: rate=0.12 (rate<0.05 threshold exceeded)
```

---

## 8. Grafana 연동

### 8.1 k6 메트릭을 Prometheus에 전송

k6는 Prometheus Remote Write를 지원합니다.

#### 설정 (로컬 테스트)

```bash
# 1. Co-Talk prometheus 확인
curl http://localhost:9090/api/v1/status/config

# 2. k6 테스트 실행 + Prometheus Remote Write
k6 run \
  --out experimental-prometheus-rw \
  scenarios/rest-api.js
```

#### Prometheus 설정 (`prometheus.yml`)

```yaml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'co-talk-app'
    static_configs:
      - targets: ['localhost:8080']

# k6 메트릭 수신 설정 (옵션)
remote_write:
  - url: http://prometheus:9090/api/v1/write
```

### 8.2 Grafana 대시보드 설정

1. **Prometheus 데이터소스 추가**:
   - Grafana → Configuration → Data Sources
   - Add → Prometheus
   - URL: `http://prometheus:9090`

2. **k6 대시보드 생성**:

```
대시보드 패널 쿼리 예시:

패널 1: HTTP 요청 성공률
Query: rate(http_reqs_total{status=~"2.."}[1m]) / rate(http_reqs_total[1m])

패널 2: 평균 응답 시간 (p95)
Query: histogram_quantile(0.95, http_req_duration_seconds)

패널 3: 에러율
Query: rate(http_reqs_total{status=~"5.."}[1m]) / rate(http_reqs_total[1m])

패널 4: 처리량 (req/s)
Query: rate(http_reqs_total[1m])
```

### 8.3 기존 Grafana 모니터링과 함께 보기

Co-Talk 기존 대시보드:
- APP 지표: CPU, Memory, JVM Heap
- DB 지표: Query 성능, Connection Pool
- 비즈니스 지표: 요청 수, 에러 수

k6와 동시 실행 시:
```bash
# 터미널 1: Grafana 열기 (http://localhost:3000)
# 터미널 2: k6 테스트 시작
k6 run --out experimental-prometheus-rw scenarios/rest-api.js

# 동안 Grafana에서 APP과 k6 메트릭 동시 감시
```

---

## 9. 테스트 사용자 관리

### 9.1 자동 생성되는 계정

k6 테스트 실행 시 다음 패턴의 계정이 자동 생성됩니다:

```
VU 1:    loadtest+1@test.cotalk.com / Test1234!@
VU 2:    loadtest+2@test.cotalk.com / Test1234!@
VU 3:    loadtest+3@test.cotalk.com / Test1234!@
...
VU N:    loadtest+N@test.cotalk.com / Test1234!@

닉네임:  loadtest-user-1, loadtest-user-2, ...
```

### 9.2 테스트 계정 수량 예측

각 시나리오별 생성 계정 수:

| 시나리오 | 계정 수 | 설명 |
|---------|--------|------|
| rest-api.js | VUS × 2 | setup에서 미리 생성 |
| websocket-chat.js | VUS | 각 VU마다 1개 |
| full-flow.js | VUS | 각 VU가 독립적으로 생성 |
| stress.js | MAX_VUS + 10 | setup에서 한 번에 생성 |
| spike.js | 100 | setup에서 고정 생성 |

### 9.3 테스트 계정 정리

#### 수동 정리 (권장: 테스트 후)

```bash
# DB 접속
psql -U postgres -h localhost -d co_talk

# 테스트 계정 삭제 (이메일 패턴 기준)
DELETE FROM "user" WHERE email LIKE 'loadtest+%@test.cotalk.com';
DELETE FROM "user" WHERE email LIKE 'perftest+%@test.cotalk.com';

# 확인
SELECT COUNT(*) FROM "user" WHERE email LIKE 'loadtest%';
```

#### 자동 정리 스크립트 (선택사항)

파일 생성: `k6/cleanup.sh`

```bash
#!/bin/bash
# k6 테스트 후 테스트 계정 자동 정리

DB_USER="postgres"
DB_HOST="localhost"
DB_NAME="co_talk"

# 이메일 패턴 (config.js의 TEST_EMAIL_PREFIX와 일치)
EMAIL_PREFIX="${1:-loadtest}"

echo "Deleting test users with prefix: $EMAIL_PREFIX"

psql -U $DB_USER -h $DB_HOST -d $DB_NAME -c \
  "DELETE FROM \"user\" WHERE email LIKE '${EMAIL_PREFIX}+%@test.cotalk.com';"

# 정리된 계정 수 확인
COUNT=$(psql -U $DB_USER -h $DB_HOST -d $DB_NAME -t -c \
  "SELECT COUNT(*) FROM \"user\" WHERE email LIKE '${EMAIL_PREFIX}+%@test.cotalk.com';")

echo "Remaining test users: $COUNT"
```

사용:
```bash
chmod +x k6/cleanup.sh
./k6/cleanup.sh loadtest
./k6/cleanup.sh perftest
```

### 9.4 데이터 고립 전략

테스트 실행 사이에 데이터 충돌을 피하기:

```bash
# 1. 테스트 전: 기존 테스트 계정 삭제
./k6/cleanup.sh loadtest

# 2. k6 테스트 실행
k6 run scenarios/rest-api.js

# 3. 테스트 후: 다시 정리
./k6/cleanup.sh loadtest
```

또는 각 테스트마다 다른 prefix 사용:

```bash
# 첫 번째 테스트
k6 run --env TEST_EMAIL=test1 scenarios/rest-api.js

# 두 번째 테스트 (독립적)
k6 run --env TEST_EMAIL=test2 scenarios/rest-api.js

# 정리 시
./k6/cleanup.sh test1
./k6/cleanup.sh test2
```

---

## 10. 권장 테스트 순서와 기대 결과

### 10.1 개발 환경 (로컬) 테스트 시퀀스

**총 소요 시간: ~30분**

```bash
# ─── 1단계: 기본 기능 확인 (5분) ───
# 목표: 테스트 환경 정상 작동 확인

echo "=== REST API 기본 테스트 (VUS=5, DURATION=1m) ==="
k6 run \
  --env VUS=5 \
  --env DURATION=1m \
  scenarios/rest-api.js

# 기대 결과:
# ✓ http_req_failed < 5%
# ✓ http_req_duration p(95) < 500ms
# ✓ 모든 엔드포인트 200/201 응답


# ─── 2단계: WebSocket 연결 확인 (5분) ───
# 목표: WebSocket/STOMP 정상 작동

echo "=== WebSocket 기본 테스트 (VUS=5) ==="
k6 run \
  --env VUS=5 \
  scenarios/websocket-chat.js

# 기대 결과:
# ✓ ws_messages_sent count > 0
# ✓ ws_messages_received count > 0
# ✓ ws_message_latency p(95) < 200ms
# ✓ Redis Pub/Sub 브로드캐스트 작동


# ─── 3단계: 전체 여정 (5분) ───
# 목표: 실제 사용 패턴 테스트

echo "=== 전체 사용자 여정 (VUS=10, DURATION=3m) ==="
k6 run scenarios/full-flow.js

# 기대 결과:
# ✓ Signup 성공률 100% (또는 409 중복 무시)
# ✓ WebSocket 세션 60초 유지
# ✓ 전체 여정 완료율 90% 이상


# ─── 4단계: 중간 부하 테스트 (8분) ───
# 목표: 부하 증가 시 성능 변화 관찰

echo "=== 중간 부하 테스트 (VUS=20) ==="
k6 run \
  --env VUS=20 \
  --env DURATION=5m \
  scenarios/rest-api.js

# 기대 결과:
# ✓ http_req_failed < 5%
# ✓ http_req_duration p(95) < 600ms (VUS=5 대비 약간 증가)
# ✓ 처리량 40+ req/s


# ─── 5단계: 스트레스 테스트 (10분) ───
# 목표: 한계점 파악

echo "=== 스트레스 테스트 (MAX_VUS=50) ==="
k6 run --env MAX_VUS=50 scenarios/stress.js

# 기대 결과:
# VUS=10: http_req_duration p(95) < 500ms
# VUS=30: http_req_duration p(95) < 800ms
# VUS=50: http_req_duration p(95) < 1200ms (error rate < 5%)
```

### 10.2 스트레스 테스트 예상 결과 해석

#### Good (안정적 시스템)

```
VUS  p(95)    Error
10   250ms    0%
30   450ms    0.5%
50   700ms    1.2%
100  1200ms   2%
     ↑ 선형적 증가, 안정적
```

→ **조치**: 없음, 배포 진행 가능

#### Warning (주의 필요)

```
VUS  p(95)    Error
10   250ms    0%
30   600ms    2%
50   1500ms   5%
100  3000ms   8%
     ↑ 비선형 증가, 한계점 VUS=50 근처
```

→ **조치**:
- Rate limit 설정 확인
- DB 인덱스 확인
- 캐시 전략 검토

#### Critical (심각한 문제)

```
VUS  p(95)    Error
10   500ms    1%
30   2000ms   10%
50   5000ms+  20%+
     ↑ 급격한 증가, 시스템 불안정
```

→ **조치**:
- 메모리 누수 검사 (jdk.jfr)
- DB 연결 풀 최적화
- WebSocket 리소스 누수 확인

### 10.3 카나리아 배포 검증 시퀀스

**배포 후 검증: ~15분**

```bash
# ─── 카나리아 배포 (app-1만 새 버전) ───
# docker compose에서 app-1 이미지 업데이트

# ─── 헬스체크 ───
curl http://your-nas:18080/actuator/health
# { "status": "UP" }

# ─── 스파이크 테스트 (1분) ───
echo "=== 스파이크 테스트 (카나리아 후) ==="
k6 run \
  --env BASE_URL=http://your-nas:18080 \
  --env VUS=5 \
  scenarios/spike.js

# 기대 결과:
# Spike 중: http_req_duration p(95) < 3000ms
# Drop 후: 1-2초 내 정상화
# 에러율: 5% 이하


# ─── REST API 부하 테스트 (3분) ───
echo "=== REST API 부하 테스트 (카나리아 후) ==="
k6 run \
  --env BASE_URL=http://your-nas:18080 \
  --env VUS=10 \
  --env DURATION=3m \
  scenarios/rest-api.js

# 기대 결과:
# 이전 버전과 비슷한 성능
# 또는 약간 향상된 성능


# ─── WebSocket 테스트 (3분) ───
echo "=== WebSocket 테스트 (카나리아 후) ==="
k6 run \
  --env BASE_URL=http://your-nas:18080 \
  --env WS_URL=ws://your-nas:18080 \
  --env VUS=10 \
  scenarios/websocket-chat.js

# 기대 결과:
# 메시지 전송/수신 정상
# 레이턴시 < 200ms


# ─── 결론 ───
if [ 모든_테스트_통과 ]; then
  echo "✓ 카나리아 배포 검증 통과"
  echo "→ app-2, app-3도 배포 진행 가능"
else
  echo "✗ 카나리아 배포 검증 실패"
  echo "→ app-1 롤백 후 원인 분석"
fi
```

---

## 11. 트러블슈팅

### 11.1 Rate Limiting 에러

#### 증상

```
http_req_failed: 15% ⚠️
에러 응답: 429 Too Many Requests
```

#### 원인

Co-Talk에 다음과 같은 Rate Limit이 적용됨:
- 로그인: 5/분
- 회원가입: 3/분
- 일반 API: 제한 없음

#### 해결책

**방법 1: VUS 감소**
```bash
k6 run --env VUS=5 scenarios/rest-api.js
```

**방법 2: 테스트 사용자 분산**
```bash
# 각 VU가 다른 계정 사용
k6 run \
  --env VUS=20 \
  --env TEST_EMAIL=loadtest \
  scenarios/rest-api.js

# 또는 prefix 변경
k6 run --env TEST_EMAIL=loadtest1 scenarios/rest-api.js &
k6 run --env TEST_EMAIL=loadtest2 scenarios/rest-api.js &
```

**방법 3: Rate Limit 정책 확인 및 조정**

`application.yml`:
```yaml
rate-limit:
  login:
    requests: 10      # 기본 5 → 10으로 증가
    window-seconds: 60
  signup:
    requests: 5       # 기본 3 → 5로 증가
    window-seconds: 60
```

### 11.2 Connection Refused

#### 증상

```
Error: connect ECONNREFUSED 127.0.0.1:8080
dial_error: 2 dial tcp 127.0.0.1:8080: connect: connection refused
```

#### 원인

- Co-Talk 서버가 실행 중이 아님
- 잘못된 BASE_URL 설정
- 방화벽이 포트 차단

#### 해결책

```bash
# 1. 서버 상태 확인
curl http://localhost:8080/actuator/health

# 2. 로그 확인
./gradlew bootRun  # 콘솔 에러 메시지 확인

# 3. 포트 사용 확인
lsof -i :8080  # 또는 netstat -an | grep 8080

# 4. BASE_URL 확인
echo "BASE_URL: $BASE_URL"  # 환경변수 확인

# 5. 재시작
pkill -f "java.*cotalk"
./gradlew bootRun
```

### 11.3 WebSocket 연결 실패

#### 증상

```
ws: status 101 false ✗
WebSocket handshake failed
```

#### 원인

- WebSocket 엔드포인트가 설정되지 않음
- JWT 토큰 만료
- STOMP 프로토콜 버전 불일치

#### 해결책

```bash
# 1. WebSocket 엔드포인트 확인
curl -I -N -H "Connection: Upgrade" \
  -H "Upgrade: websocket" \
  http://localhost:8080/ws/websocket

# 2. 로그 확인 (STOMP 에러)
grep -i "websocket\|stomp" logs/*.log

# 3. 토큰 유효성 확인
echo "accessToken: $TOKEN"  # 토큰 값 확인 (디버그)

# 4. 수동 STOMP 연결 테스트
nc -v localhost 8080
CONNECT
accept-version:1.2
Authorization:Bearer {TOKEN}

# (응답: CONNECTED)
```

### 11.4 메모리 부족 에러

#### 증상

```
Error: Cannot allocate memory
process.memoryUsage(): { rss: 2000000000 }
```

#### 원인

- VUS가 너무 많음 (각 VU가 메모리 소비)
- WebSocket 세션이 닫혀도 메모리 해제 안 됨

#### 해결책

```bash
# 1. VUS 감소
k6 run --env VUS=10 scenarios/websocket-chat.js

# 2. 세션 시간 단축 (websocket.js 수정)
// connectStomp(..., timeoutMs = 15000)  # 30s → 15s

# 3. k6 메모리 제한 (Docker)
docker run \
  -m 512m \
  -v $(pwd)/k6:/scripts \
  grafana/k6 run /scripts/scenarios/rest-api.js
```

### 11.5 STOMP 프레임 파싱 에러

#### 증상

```
ReferenceError: stompSubscribe is not defined
Cannot read property 'command' of undefined
```

#### 원인

- `helpers/websocket.js` import 누락
- STOMP 프레임 형식 오류

#### 해결책

시나리오 파일에 import 확인:

```javascript
// ✓ 정확한 import
import {
  connectStomp,
  stompSubscribe,
  stompSend,
} from '../helpers/websocket.js';

// ✗ 오류 (전체 export 객체로 가져옴)
import websocket from '../helpers/websocket.js';
// → websocket.stompSubscribe() 호출 필요
```

### 11.6 높은 에러율 (> 5%)

#### 증상

```
http_req_failed: 8% ⚠️
상태 코드 분포: 200 (1840), 500 (120), 503 (40)
```

#### 원인 분석 (우선순위)

1. **Rate Limit** (429): 위의 "Rate Limiting 에러" 참조
2. **Server Error** (500): 백엔드 버그
3. **Service Unavailable** (503): 서버 과부하 또는 메모리 부족
4. **Timeout** (ECONNRESET): DB 연결 끊김

#### 해결책

```bash
# 1. 상세 에러 로그 저장
k6 run \
  --out json=results/error-debug.json \
  scenarios/rest-api.js

# 2. JSON에서 에러 추출
cat results/error-debug.json | jq '.data.samples[] | select(.data.error_code != null) | {time: .time, method: .data.method, url: .data.url, status: .data.status, error: .data.error_code}' | head -20

# 3. 백엔드 로그 확인
tail -f logs/co-talk.log | grep -i "ERROR\|Exception"

# 4. 시스템 리소스 확인
top -b -n 1 | head -20
free -h
df -h
```

### 11.7 결과 파일이 너무 큼

#### 증상

```
results/test-20240116-143025.json: 150MB (큼!)
```

#### 원인

- `--out json` 옵션으로 모든 요청 저장
- 샘플(sample) 수가 많음

#### 해결책

```bash
# 1. 샘플링 활용 (기본값: 모든 데이터)
k6 run \
  --out json=results/test.json \
  --summary-export=results/summary.json \
  scenarios/rest-api.js

# 2. 요약 데이터만 저장 (권장)
k6 run --summary-export=results/summary.json scenarios/rest-api.js

# 3. 큰 JSON 분석
# 요약 통계만 추출
cat results/test.json | jq '.metrics'

# 그래프 데이터 추출
cat results/test.json | jq '.metrics.http_req_duration'
```

### 11.8 SSL/TLS 인증서 에러 (HTTPS 사용 시)

#### 증상

```
SSL: CERTIFICATE_VERIFY_FAILED
Unsafe legacy renegotiation disabled
```

#### 해결책

```bash
# 1. 자체 서명 인증서 무시 (개발 환경만)
k6 run \
  -k \
  scenarios/rest-api.js

# 2. 또는 환경변수
K6_INSECURE_SKIP_TLS_VERIFY=true k6 run scenarios/rest-api.js
```

---

## 12. FAQ (자주 묻는 질문)

### Q1: 테스트 중에 서버를 재시작하면?

A: k6는 자동으로 재시도하지 않습니다. 연결된 VU는 실패하고, 에러로 카운트됩니다. 테스트 결과에 spike가 나타납니다:

```
서버 재시작 시점에
http_req_duration이 갑자기 증가
http_req_failed가 급증
```

의도적으로 테스트하는 경우라면, **Graceful Shutdown** 동작 여부를 확인하세요.

### Q2: 여러 대의 머신에서 동시에 테스트할 수 있나?

A: 가능합니다. 각 머신에서 독립적으로 k6를 실행하면 됩니다:

```bash
# 머신 1
k6 run --env VUS=10 scenarios/rest-api.js &

# 머신 2
k6 run --env VUS=10 scenarios/rest-api.js &

# 머신 3
k6 run --env VUS=10 scenarios/rest-api.js &

# 총 30 VU 부하 테스트
```

### Q3: 특정 시간대에만 테스트하고 싶으면?

A: cron 설정 또는 GitHub Actions 활용:

```bash
# crontab 설정 (매일 새벽 2시에 테스트)
0 2 * * * cd /home/user/co-talk && k6 run k6/scenarios/rest-api.js --out json=results/$(date +\%Y\%m\%d).json
```

또는 GitHub Actions (`.github/workflows/k6-load-test.yml`):

```yaml
name: k6 Load Test
on:
  schedule:
    - cron: '0 2 * * *'  # 매일 2시
jobs:
  load-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - uses: grafana/k6-action@v0.3.0
        with:
          filename: k6/scenarios/rest-api.js
```

### Q4: 본 테스트가 실제 사용자 경험을 반영하나?

A: 부분적으로만 반영합니다. k6는 **프로토콜 수준** 테스트이므로:

✓ 반영됨:
- 네트워크 지연
- 서버 처리 시간
- DB 쿼리 성능

✗ 미반영:
- 브라우저 렌더링 시간
- JavaScript 실행 시간
- 이미지 로딩 시간
- 사용자의 실제 클릭 패턴

더 정확한 테스트: **Selenium**, **Lighthouse**, **WebPageTest** 등 활용

### Q5: 테스트 중 특정 요청만 분석하고 싶으면?

A: `--out` 옵션으로 JSON 저장 후 필터링:

```bash
# 특정 그룹의 요청만 추출
cat results/test.json | jq '.data.samples[] | select(.data.group | contains("GET /users/me"))'

# 특정 상태 코드 요청만 추출
cat results/test.json | jq '.data.samples[] | select(.data.status == 500)'

# 느린 요청만 추출 (응답 시간 > 1000ms)
cat results/test.json | jq '.data.samples[] | select(.data.duration > 1000)'
```

### Q6: CI/CD 파이프라인에 k6를 통합하려면?

A: 테스트 실패 시 배포를 블로킹:

```yaml
# .github/workflows/deploy.yml
- name: Run k6 Load Test
  run: |
    k6 run \
      --summary-export=results/summary.json \
      --out json=results/test.json \
      k6/scenarios/rest-api.js

- name: Check k6 Thresholds
  run: |
    # thresholds 통과 여부 확인
    if grep -q '"failed": true' results/summary.json; then
      echo "❌ k6 테스트 실패"
      exit 1
    fi
    echo "✅ k6 테스트 통과"

- name: Deploy
  if: success()
  run: ./deploy.sh
```

---

## 13. 추가 리소스

### 공식 문서
- [k6 공식 문서](https://k6.io/docs/)
- [k6 JavaScript API](https://k6.io/docs/javascript-api/)
- [STOMP 프로토콜](https://stomp.github.io/stomp-specification-1.2.html)

### 관련 설정 파일
- `docker-compose.yml`: 로컬 테스트 환경
- `docker-compose.nas.yml`: NAS 배포 환경
- `prometheus.yml`: 메트릭 수집
- `application.yml`: Rate Limit 설정

### 모니터링 대시보드
- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3000`
- k6 Cloud (선택사항): `https://cloud.k6.io`

### 문제 해결 가이드
- [k6 Troubleshooting](https://k6.io/docs/cloud/analyzing-results/result-analysis/)
- [Co-Talk 프로덕션 준비 가이드](../docs/PRODUCTION_READINESS.md)

---

## 14. 변경 로그

### v2.0 (2024-02-16)
- 포괄적인 한국어 부하 테스트 가이드 작성
- 5개 시나리오 상세 설명 추가
- 실행 방법, 트러블슈팅 전체 수정
- 3인스턴스 카나리아 배포 컨텍스트 적용
- FAQ 및 추가 리소스 확대

### v1.0 (초기)
- 기본 설치 및 실행 방법 문서화

---

**작성자**: Co-Talk DevOps Team
**최종 업데이트**: 2024-02-16
**문의**: cotalk-dev@example.com
