# k6로 채팅 앱 부하 테스트하기 — 그리고 성능 최적화

> Co-Talk 개발기 시리즈 9편. PR #99(부하 테스트 시나리오 구성), PR #100(성능 최적화) 내용을 다룬다.

---

## 왜 부하 테스트인가

기능이 동작하는 것과 동시에 100명이 써도 동작하는 것은 완전히 다른 문제다.

채팅 앱의 특성상 순간적으로 트래픽이 집중된다. 누군가 그룹채팅에 메시지를 보내면 멤버 전원에게 WebSocket 푸시가 동시에 나가고, FCM 알림 조회가 동시에 발생하고, Redis에 presence 상태가 동시에 쓰인다. 1명일 때 p95 응답시간이 50ms여도 100명이 동시에 메시지를 보내는 순간 DB 커넥션 풀이 고갈되거나 Redis 연결이 몰릴 수 있다.

단위 테스트와 통합 테스트로는 이 부분을 절대 검증할 수 없다. 부하 테스트만이 알려준다.

---

## k6를 선택한 이유

부하 테스트 도구 선택지는 여럿이다. JMeter, Gatling, Locust, k6.

k6를 고른 이유는 단순하다.

**JavaScript로 시나리오를 작성한다.** JMeter의 XML 설정 파일이나 Gatling의 Scala DSL 대신, 개발자에게 익숙한 JS 코드로 테스트 로직을 작성할 수 있다. 조건 분기, 반복, 함수 추출 모두 자유롭게 된다.

**CLI 기반이라 CI에 붙이기 쉽다.** `k6 run scenario.js` 한 줄이면 된다.

**WebSocket을 first-class로 지원한다.** `k6/ws` 모듈이 내장되어 있어 STOMP 위에서 실시간 채팅 시나리오를 직접 구현할 수 있다.

**무료 오픈소스다.** Grafana Cloud와 통합하면 실시간 대시보드도 볼 수 있지만, 로컬에서 CLI 출력만으로도 충분히 유용하다.

---

## 디렉토리 구조

```
k6/
├── config.js             # 공통 설정, 임계값, 헬퍼 함수
├── seed.sh               # 부하 테스트용 사용자 사전 생성 스크립트
├── run.sh                # 시나리오 실행 래퍼
├── data/
│   └── users.json        # seed.sh 실행 결과 (JWT 토큰 저장)
├── helpers/
│   ├── auth.js           # 인증 헬퍼
│   └── websocket.js      # STOMP 프레임 생성/파싱 헬퍼
└── scenarios/
    ├── rest-api.js        # REST API 전반 부하 테스트
    ├── websocket-chat.js  # WebSocket 실시간 채팅 부하 테스트
    ├── spike.js           # 스파이크 테스트
    ├── stress.js          # 점진적 부하 증가 테스트
    ├── breakpoint.js      # 한계점 테스트
    └── full-flow.js       # 전체 사용자 여정
```

---

<!-- IMAGE: k6 실행 터미널 출력 — `k6 run scenarios/rest-api.js` 완료 후 출력되는 결과 요약. http_req_duration p(95)/p(99), http_req_failed rate, iterations 항목이 보이는 터미널 캡처 -->

## 시나리오 설계

### rest-api.js — REST API 전반

실제 사용자 행동 패턴을 모사한다. 프로필 조회 → 친구 목록 → 채팅방 목록 → 메시지 조회 → 사용자 검색까지 순서대로 호출한다. 채팅방 생성은 10% 확률, 메시지 전송은 20% 확률로만 실행해서 읽기 위주의 현실적인 트래픽 비율을 만든다.

```javascript
stages: [
  { duration: '30s', target: VUS },     // 워밍업
  { duration: DURATION, target: VUS },   // 유지
  { duration: '15s', target: VUS * 2 }, // 피크
  { duration: '30s', target: VUS },      // 복구
  { duration: '15s', target: 0 },
],
thresholds: {
  http_req_duration: ['p(95)<500', 'p(99)<1000'],
  http_req_failed: ['rate<0.05'],
}
```

p95 500ms, 에러율 5% 미만을 통과 기준으로 잡았다.

### websocket-chat.js — WebSocket 실시간 채팅

이것이 핵심 시나리오다. 실제 사용자처럼 STOMP 연결 → 채팅방 구독 → 메시지 전송 → RTT 측정 흐름을 구현했다.

```javascript
// 3초마다 메시지 전송 (RTT 측정용 timestamp 삽입)
socket.setInterval(function () {
  messageCount++;
  socket.send(
    stompSend('/app/chat/message', {
      roomId: roomId,
      content: `${vuMarker}${Date.now()}|msg#${messageCount}`,
    })
  );
  wsMessages.add(1);
}, 3000);
```

메시지 내용에 `k6|{VU번호}|{전송시각}|` 형태의 마커를 삽입한다. 서버가 이 메시지를 브로드캐스트해서 돌아오면 수신 핸들러에서 마커를 파싱해 RTT(Round Trip Time)를 계산한다.

```javascript
function onMessage(frame) {
  const body = frame.body;
  const idx = body.indexOf(vuMarker);
  if (idx < 0) return; // 자신이 보낸 메시지가 아님
  const afterMarker = body.substring(idx + vuMarker.length);
  const pipeIdx = afterMarker.indexOf('|');
  const sendTime = parseInt(afterMarker.substring(0, pipeIdx));
  wsRtt.add(Date.now() - sendTime);
}
```

채팅 임계값은 `ws_rtt: ['p(95)<2000', 'avg<1000']`로 잡았다. 평균 RTT 1초 미만, p95 2초 미만.

3초마다 메시지 외에도 10초마다 타이핑 상태, 15초마다 presence 핑을 전송해서 실제 사용자 행동에 가깝게 만들었다.

### spike.js — 스파이크 테스트

갑작스러운 트래픽 급증을 테스트한다. 5 VU로 1분 안정적으로 실행하다가 10초 안에 80 VU로 급증시킨다.

```javascript
stages: [
  { duration: '30s', target: 5 },
  { duration: '1m', target: 5 },
  { duration: '10s', target: 80 },  // 갑작스러운 급증
  { duration: '1m', target: 80 },
  { duration: '10s', target: 5 },   // 복구
  { duration: '1m', target: 5 },
  { duration: '10s', target: 0 },
]
```

스파이크 테스트에서는 임계값을 느슨하게 잡는다. p95 3초, 에러율 10%. 목적은 서버가 스파이크에서 회복하는지 확인하는 것이지, 스파이크 중 성능을 확인하는 게 아니다.

### stress.js — 점진적 부하 증가

10 → 30 → 50 → 100 VU로 단계적으로 올리면서 각 단계를 2분씩 유지한다. 부하가 증가해도 에러율이 임계값(15%) 아래를 유지하는지 확인한다. 랜덤으로 2~3개 API를 호출해서 다양한 트래픽 패턴을 만든다.

### breakpoint.js — 한계점 테스트

서버가 버티는 한계를 찾는 테스트다. 10 → 50 → 100 → 150 → 200 VU를 각 40초씩 유지하면서 어느 단계에서 에러율이 50%를 넘는지 관찰한다.

```javascript
stages: [
  { duration: '10s', target: 10 },
  { duration: '40s', target: 10 },
  { duration: '10s', target: 50 },
  { duration: '40s', target: 50 },
  { duration: '10s', target: 100 },
  { duration: '40s', target: 100 },
  { duration: '10s', target: 150 },
  { duration: '40s', target: 150 },
  { duration: '10s', target: 200 },
  { duration: '40s', target: 200 },
  { duration: '10s', target: 0 },
],
thresholds: {
  ws_error_rate: [{ threshold: 'rate<0.5', abortOnFail: true }], // 50% 초과 시 중단
}
```

`abortOnFail: true`로 설정해서 에러율 50%를 넘는 순간 테스트를 자동 중단한다. 그 시점의 VU 수가 현재 서버 한계점이다.

### full-flow.js — 전체 사용자 여정

"프로필 조회 → 친구 목록 → 채팅방 조회 → WebSocket 채팅 30초 → 메시지 이력 조회" 순서로 실제 사용자가 앱을 여는 순간부터 채팅을 하고 나가는 전체 흐름을 검증한다.

---

## 시딩 — 부하 테스트 전 사용자 준비

부하 테스트를 실행하기 전에 테스트 사용자를 미리 만들어야 한다. 매 테스트 실행 때마다 회원가입/로그인을 하면 두 가지 문제가 생긴다. 첫째로 Rate Limit에 걸린다. 둘째로 테스트 시간의 상당 부분을 준비 작업이 차지한다.

`seed.sh`가 이 문제를 해결한다.

```bash
# 기본 사용 (로컬, 20명)
./k6/seed.sh

# 원격 서버, 50명
./k6/seed.sh https://co-talk.example.com 50

# Rate Limit 우회 토큰 사용 (빠른 시딩)
K6_TOKEN=<token> ./k6/seed.sh https://co-talk.example.com 100
```

스크립트 실행 흐름은 3단계다.

**Phase 1 — 회원가입**: 사용자를 생성한다. `K6_TOKEN`이 있으면 nginx Rate Limit을 우회해서 빠르게 진행하고, 없으면 분당 5건 Rate Limit을 맞춰 13초씩 대기한다. 409(이미 존재)는 건너뛴다.

**Phase 1.5 — 이메일 인증 활성화**: Co-Talk은 이메일 인증이 필요하다. NAS SSH 접근이 가능하면 DB를 직접 업데이트해서 자동 처리하고, 없으면 수동으로 진행하도록 안내한다.

```bash
docker exec cotalk-postgres psql -U cotalk -d cotalk \
  -c "UPDATE users SET email_verified = true WHERE email LIKE 'loadtest%@test.cotalk.com'"
```

**Phase 2 — 로그인 + 프로필 조회**: 각 사용자로 로그인해서 JWT 토큰과 userId를 수집한다. 결과는 `k6/data/users.json`에 저장된다.

```json
[
  { "vuId": 1, "accessToken": "eyJ...", "userId": "281840969769287680", "email": "loadtest+1@test.cotalk.com" },
  { "vuId": 2, "accessToken": "eyJ...", "userId": "281840969769287681", "email": "loadtest+2@test.cotalk.com" }
]
```

k6 시나리오들은 실행 시 이 파일을 `SharedArray`로 로드해서 VU에 할당한다. `SharedArray`는 모든 VU가 같은 메모리를 공유하므로 메모리 효율적이다.

---

## Snowflake ID 정밀도 문제

처음 부하 테스트를 돌렸을 때 이상한 현상이 있었다. 채팅방 생성 후 roomId로 WebSocket을 구독하려 하면 서버가 "채팅방 멤버가 아닙니다" 에러를 돌려보냈다. 분명 방금 만든 방인데.

원인은 JavaScript의 정수 표현 한계였다.

JavaScript `Number` 타입은 IEEE 754 64비트 부동소수점이다. 정수를 정확히 표현할 수 있는 범위는 `Number.MAX_SAFE_INTEGER = 2^53 - 1 = 9,007,199,254,740,991`까지다. Co-Talk의 Snowflake ID는 64비트 정수라 이 범위를 훌쩍 넘는다.

예를 들어 `281840969769287680`이라는 ID가 있다면, `JSON.parse()`로 파싱하는 순간 `281840969769287680`이 되어야 할 값이 `281840969769287680`이 아닌 가장 가까운 표현 가능한 수로 바뀐다. 그 결과로 `281840969769287680`이 `281840969769287700` 같은 값이 되고, 서버가 존재하지 않는 방 ID로 인식한다.

해결책으로 `safeParseBigInts` 함수를 만들었다.

```javascript
export function safeParseBigInts(jsonStr) {
  // JSON 값 위치의 16자리 이상 숫자를 문자열로 변환 후 파싱
  // {"roomId": 281840969769287680} → {"roomId": "281840969769287680"}
  return JSON.parse(
    jsonStr.replace(/("(?:[^"\\]|\\.)*")|(\b\d{16,}\b)/g, (match, str, num) => {
      if (str) return str; // 문자열 내부 숫자는 건드리지 않음
      return `"${num}"`;   // JSON 값 위치의 큰 숫자만 문자열화
    })
  );
}
```

정규식이 하는 일은 두 가지다. 따옴표 안에 있는 숫자(`"timestamp": "1234567890123456"` 같은 경우)는 이미 문자열이니 건드리지 않는다. 따옴표 밖에 있는 16자리 이상 숫자만 따옴표로 감싼다.

STOMP SEND 프레임을 만들 때는 반대로 해야 한다. 서버의 `@RequestBody` 역직렬화가 문자열이 아닌 JSON 숫자를 기대하기 때문이다.

```javascript
export function stompSend(destination, body) {
  // JSON.stringify 후, 문자열로 보존된 Snowflake ID를 JSON 숫자로 복원
  // "roomId":"281840969769287680" → "roomId":281840969769287680
  const jsonBody = JSON.stringify(body).replace(/:\s*"(\d{16,})"/g, (match, num) => `:${num}`);
  // ...
}
```

파싱 시에는 숫자를 문자열로 변환하고, 전송 시에는 문자열을 다시 숫자로 복원하는 것이다.

---

<!-- IMAGE: Grafana k6 대시보드 — 부하 테스트 실행 중 실시간 VU 수, 요청 속도, 응답시간 그래프가 그려지는 대시보드 화면 -->

## 부하 테스트에서 발견한 성능 병목

테스트를 돌리기 전에는 문제가 없어 보였던 코드들이 부하 테스트 결과를 보고 나서 달라 보이기 시작했다.

### 병목 1: 트랜잭션 범위가 너무 넓었다

원래 `SendMessageService`의 `sendMessage()`는 `@Transactional`로 전체를 감싸고 있었다. 구조는 대략 이랬다.

```
@Transactional
sendMessage() {
  // 1. DB: 멤버십 확인
  // 2. DB: 메시지 저장
  // 3. Redis: Presence 조회 (Active 사용자 필터링)
  // 4. FCM: 푸시 알림 전송
}
```

문제는 3번과 4번이다. Redis 호출과 FCM 호출이 트랜잭션 안에 있으면, DB 커넥션을 잡고 있는 동안 Redis와 외부 HTTP 요청이 완료되기를 기다린다. FCM 응답이 느린 날이면 DB 커넥션이 그 시간 동안 계속 점유된다.

100명이 동시에 메시지를 보내면? 커넥션 풀(보통 10~20개)이 순식간에 고갈된다. 이후 요청들은 커넥션을 기다리다 타임아웃이 난다.

`TransactionTemplate`으로 범위를 최소화했다.

```java
private SendResult doSendMessage(Long chatRoomId, Long senderId, Message message, String notificationContent) {
    // DB 작업만 트랜잭션으로 래핑 (커넥션 점유 최소화)
    SendResult result = transactionTemplate.execute(status -> {
        List<ChatRoomMember> members = chatRoomMemberRepository.findByChatRoomId(chatRoomId);
        User sender = userRepository.findById(senderId).orElse(null);

        boolean isMember = members.stream().anyMatch(m -> m.getUserId().equals(senderId));
        if (!isMember) throw new ChatRoomAccessDeniedException(chatRoomId, senderId);

        Message savedMessage = messageRepository.save(message);
        // ... lastReadMessageId 업데이트 ...

        return new SendResult(savedMessage, senderNickname, senderAvatarUrl, members);
    }); // ← 여기서 커넥션 반환

    // 트랜잭션 밖: Redis/FCM 호출 (DB 커넥션 미점유)
    sendPushNotificationsToOtherMembers(...);

    return result;
}
```

`@Transactional` 어노테이션을 제거하고 `TransactionTemplate.execute()` 블록으로 DB 작업만 감쌌다. 블록이 끝나는 순간 커넥션이 반환된다. Redis와 FCM은 그 이후에 처리된다.

### 병목 2: Presence 조회가 N번 Redis 왕복이었다

메시지를 보낼 때 "채팅방 안에 있는 사용자들(이미 읽고 있는 사람들)에게는 푸시를 보내지 않는다"는 로직이 있다. 이를 위해 각 멤버의 presence를 Redis에서 확인한다.

원래 코드는 멤버 수만큼 Redis 호출을 했다. 2명이면 2번, 10명이면 10번.

```java
// 개선 전 (의사코드)
for (Long memberId : memberIds) {
    boolean isActive = redisTemplate.hasKey("presence:" + chatRoomId + ":" + memberId);
    // ...
}
```

Redis Pipeline을 써서 한 번의 왕복으로 배치 처리했다.

```java
// ChatRoomPresenceTracker.getActiveUserIds() 구현
Set<Long> getActiveUserIds(Long chatRoomId, List<Long> userIds) {
    // Redis Pipeline: 모든 키를 한 번에 조회
    // N번 왕복 → 2회 왕복 (PIPELINE + EXEC)
}
```

`getActiveUserIds()`를 명시적으로 호출해서 한 번의 배치 조회로 전체 presence 정보를 가져온 뒤 필터링한다.

```java
// 배치 presence 조회 (Redis pipeline: 2N → 2회)
Set<Long> activeUserIds = chatRoomPresenceTracker.getActiveUserIds(chatRoomId, otherMemberIds);

List<Long> receiverUserIds = otherMemberIds.stream()
        .filter(userId -> !activeUserIds.contains(userId))
        .toList();
```

채팅방 멤버가 많을수록 이 최적화 효과가 커진다.

<!-- IMAGE: 성능 최적화 전/후 비교 그래프 — TransactionTemplate 적용 전후 메시지 전송 p95 응답시간 비교 차트. Grafana 또는 k6 결과 터미널 출력으로 before/after 나란히 캡처 -->

### 병목 3: 채팅 목록 업데이트가 메시지 RTT에 포함됐다

메시지를 저장할 때 "채팅 목록의 마지막 메시지 미리보기"를 업데이트하는 작업이 있었다. 이게 동기로 실행되어 메시지 전송 응답 시간에 포함됐다.

`@Async`로 비동기화해서 메시지 저장 완료 후 별도 스레드에서 처리하도록 분리했다. 사용자 입장에서 채팅 목록 미리보기가 100ms 늦게 업데이트되는 건 거의 느끼지 못하는 수준이지만, 메시지 전송 RTT는 그만큼 줄어든다.

---

## 실행 방법

```bash
# 1. 테스트 사용자 시딩
K6_TOKEN=<your-k6-token> ./k6/seed.sh http://localhost:8080 20

# 2. 기본 REST API 부하 테스트
k6 run k6/scenarios/rest-api.js

# 3. WebSocket 채팅 부하 테스트
k6 run --env VUS=20 --env DURATION=2m k6/scenarios/websocket-chat.js

# 4. 한계점 테스트 (200 VU까지)
k6 run --env BASE_URL=https://your-server k6/scenarios/breakpoint.js

# 5. 전체 흐름 테스트
k6 run --env VUS=10 k6/scenarios/full-flow.js
```

환경변수로 서버 주소, VU 수, 테스트 시간을 조절할 수 있다. 로컬 개발 환경에서는 VU 10~20 정도면 충분하고, 실제 서버 성능 측정 시에는 더 올린다.

---

## 교훈

**부하 테스트 없이는 성능 문제를 발견할 수 없다.** 트랜잭션 범위 문제, Redis N+1 문제 모두 단위 테스트에서는 멀쩡하게 통과한다. 실제로 동시 요청이 몰려야 드러난다.

**트랜잭션 범위 = DB 커넥션 점유 시간.** `@Transactional`이 편하지만, 트랜잭션 안에 Redis 호출이나 외부 HTTP 호출이 들어가는 순간 부하 시 커넥션 풀 고갈 위험이 생긴다. `TransactionTemplate`으로 정확히 DB 작업만 감싸는 것이 맞다.

**JavaScript에서 큰 정수를 다룰 때는 항상 조심해야 한다.** `JSON.parse()` 결과를 그대로 믿으면 안 된다. Snowflake ID처럼 `2^53`을 넘는 정수는 파싱 즉시 정밀도가 손실된다. 서버에서 ID를 문자열로 내려주거나, 클라이언트에서 `safeParseBigInts` 같은 전처리를 해야 한다.

**시딩은 테스트 자산이다.** `seed.sh`를 한 번 잘 만들어두면 이후 모든 부하 테스트의 준비 시간이 0에 가까워진다. 특히 Rate Limit이 있는 서비스에서는 사전 시딩이 필수다.

---

다음 편에서는 Co-Talk의 메시지 암호화(AES-256) 구현을 다룰 예정이다. 클라이언트-서버 간 메시지를 어떻게 암호화하고, 키 관리는 어떻게 하는지 이야기한다.
