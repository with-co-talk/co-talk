import { check, sleep } from 'k6';
import http from 'k6/http';
import {
  WS_URL,
  BASE_URL,
  API_PREFIX,
  VUS,
  DURATION,
  THRESHOLDS,
  authHeaders,
  getSeededUsers,
  safeParseBigInts,
} from '../config.js';
import {
  connectStomp,
  stompSubscribe,
  stompSend,
  wsMessages,
  wsRtt,
} from '../helpers/websocket.js';

/**
 * Co-Talk WebSocket/STOMP 부하 테스트
 *
 * 사전 준비:
 *   ./k6/seed.sh <BASE_URL> <유저수>
 *
 * 실행:
 *   k6 run k6/scenarios/websocket-chat.js
 *   k6 run --env BASE_URL=http://your-server:8080 k6/scenarios/websocket-chat.js
 */

// 세션 유지 시간 (환경변수 또는 기본 30초)
const SESSION_MS = parseInt(__ENV.SESSION_MS || '30000');

export const options = {
  scenarios: {
    websocket_chat: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '10s', target: VUS },
        { duration: DURATION, target: VUS },
        { duration: '10s', target: 0 },
      ],
      gracefulRampDown: '15s',
      gracefulStop: '35s',
    },
  },
  thresholds: {
    ...THRESHOLDS,
    ws_messages_sent: ['count>0'],
    ws_messages_received: ['count>0'],
    ws_rtt: ['p(95)<2000', 'avg<1000'],
  },
};

/**
 * safeParseBigInts로 파싱된 객체에서 채팅방 ID 추출.
 * ID는 문자열로 보존됨 (Snowflake ID 정밀도 유지).
 */
function extractRoomId(obj) {
  return obj.roomId || obj.id || obj.chatRoomId;
}

/**
 * 원본 JSON 문자열에서 특정 필드의 숫자 값을 문자열로 추출.
 * JSON.parse 없이 정확한 Snowflake ID를 보존.
 */
function extractIdFromBody(rawBody, fieldName) {
  const regex = new RegExp('"' + fieldName + '"\\s*:\\s*(\\d+)');
  const m = rawBody.match(regex);
  return m ? m[1] : null;
}

export function setup() {
  const users = getSeededUsers();
  if (!users || users.length === 0) {
    console.error('시딩된 사용자가 없습니다. 먼저 실행: ./k6/seed.sh <BASE_URL> <유저수>');
    return { users: [], rooms: [] };
  }
  console.log(`시딩된 사용자 ${users.length}명 로드 완료`);

  // SharedArray를 일반 배열로 변환
  const arr = [];
  for (let i = 0; i < users.length; i++) {
    arr.push(users[i]);
  }

  // 페어링: 인접한 사용자끼리 채팅방 생성
  const rooms = [];
  for (let i = 0; i < arr.length - 1; i += 2) {
    const user1 = arr[i];
    const user2 = arr[i + 1];
    const params = authHeaders(user1.accessToken);

    // 1) 먼저 기존 채팅방 목록에서 상대방과의 방 검색
    // safeParseBigInts: Snowflake ID(>2^53)를 문자열로 보존하여 정밀도 손실 방지
    let roomId = null;
    const listRes = http.get(`${BASE_URL}${API_PREFIX}/chat/rooms`, params);
    if (listRes.status === 200) {
      try {
        const parsed = safeParseBigInts(listRes.body);
        const roomList = parsed.rooms || parsed;
        if (Array.isArray(roomList)) {
          for (let r = 0; r < roomList.length; r++) {
            const room = roomList[r];
            if (String(room.otherUserId) === String(user2.userId)) {
              roomId = extractRoomId(room);
              break;
            }
          }
        }
      } catch {
        // skip
      }
    }

    // 2) 기존 방이 없으면 생성
    // userId2를 문자열 그대로 JSON에 숫자로 삽입 (정밀도 보존)
    if (!roomId) {
      const res = http.post(
        `${BASE_URL}${API_PREFIX}/chat/rooms`,
        `{"userId2":${user2.userId}}`,
        params
      );

      if (res.status === 200 || res.status === 201) {
        try {
          roomId = extractIdFromBody(res.body, 'roomId');
        } catch {
          // ignore
        }
      }

      // 409 (이미 존재) → 목록 재검색
      if (!roomId) {
        const retryRes = http.get(`${BASE_URL}${API_PREFIX}/chat/rooms`, params);
        if (retryRes.status === 200) {
          try {
            const parsed = safeParseBigInts(retryRes.body);
            const roomList = parsed.rooms || parsed;
            if (Array.isArray(roomList) && roomList.length > 0) {
              for (let r = 0; r < roomList.length; r++) {
                const room = roomList[r];
                if (String(room.otherUserId) === String(user2.userId)) {
                  roomId = extractRoomId(room);
                  break;
                }
              }
              if (!roomId) {
                roomId = extractRoomId(roomList[0]);
              }
            }
          } catch {
            // skip
          }
        }
      }
    }

    if (roomId) {
      rooms.push({
        roomId,
        user1Index: i,
        user2Index: i + 1,
        user1Id: user1.userId,
        user2Id: user2.userId,
      });
      console.log(`  방[${rooms.length - 1}]: roomId=${roomId}, user1(idx=${i},id=${user1.userId}), user2(idx=${i + 1},id=${user2.userId})`);
    } else {
      console.warn(`채팅방 생성/조회 실패: user pair (${i}, ${i + 1})`);
    }
  }

  console.log(`채팅방 ${rooms.length}개 준비 완료`);
  return { users: arr, rooms };
}

export default function (data) {
  if (data.rooms.length === 0) {
    console.warn('No chat rooms available for WebSocket test');
    sleep(5);
    return;
  }

  const vuIndex = (__VU - 1) % data.users.length;
  const user = data.users[vuIndex];

  // 페어링 기반 방 매핑: users[0,1]→room[0], users[2,3]→room[1], ...
  const roomInfo = data.rooms[Math.floor(vuIndex / 2) % data.rooms.length];
  const roomId = roomInfo.roomId;

  // 이 VU가 방의 실제 멤버인지 검증 (user1 또는 user2)
  const isMember =
    user.userId === roomInfo.user1Id ||
    user.userId === roomInfo.user2Id ||
    String(user.userId) === String(roomInfo.user1Id) ||
    String(user.userId) === String(roomInfo.user2Id);

  if (!isMember) {
    // 멤버가 아닌 방에 구독하면 서버가 IllegalArgumentException → 연결 종료
    // 안전하게 자신이 멤버인 방을 찾아서 사용
    let fallbackRoom = null;
    for (let r = 0; r < data.rooms.length; r++) {
      const rm = data.rooms[r];
      if (user.userId === rm.user1Id || user.userId === rm.user2Id ||
          String(user.userId) === String(rm.user1Id) ||
          String(user.userId) === String(rm.user2Id)) {
        fallbackRoom = rm;
        break;
      }
    }
    if (!fallbackRoom) {
      console.warn(`VU-${__VU}: 멤버인 방을 찾을 수 없음 (userId=${user.userId})`);
      sleep(5);
      return;
    }
    // fallbackRoom 사용
    return runWebSocketSession(user, fallbackRoom.roomId);
  }

  runWebSocketSession(user, roomId);
}

function runWebSocketSession(user, roomId) {
  let messageCount = 0;
  const vuMarker = `k6|${__VU}|`;

  connectStomp(
    WS_URL,
    user.accessToken,
    function onConnected(socket) {
      // 1. 사용자별 큐 먼저 구독 (인증 검사 없음 - /user prefix)
      socket.send(stompSubscribe('/user/queue/notifications', 'notifications'));
      socket.send(stompSubscribe('/user/queue/chat-list', 'chat-list'));
      // 서버 에러 수신용 (WebSocketExceptionHandler → /user/queue/errors)
      socket.send(stompSubscribe('/user/queue/errors', 'errors'));

      // 2. 채팅방 구독 (서버가 멤버십 검증: WebSocketAuthInterceptor)
      socket.send(stompSubscribe(`/topic/chat/room/${roomId}`, `room-${roomId}`));

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

      // 10초마다 타이핑 상태 전송
      socket.setInterval(function () {
        socket.send(
          stompSend('/app/chat/typing', {
            roomId: roomId,
            isTyping: true,
          })
        );

        socket.setTimeout(function () {
          socket.send(
            stompSend('/app/chat/typing', {
              roomId: roomId,
              isTyping: false,
            })
          );
        }, 2000);
      }, 10000);

      // 15초마다 프레즌스 핑
      socket.setInterval(function () {
        socket.send(
          stompSend('/app/chat/presence', {
            roomId: roomId,
          })
        );
      }, 15000);
    },
    SESSION_MS,
    // onMessage: 수신 메시지에서 RTT 계산 (자기가 보낸 메시지만)
    function onMessage(frame) {
      try {
        const body = frame.body;
        const idx = body.indexOf(vuMarker);
        if (idx < 0) return;
        const afterMarker = body.substring(idx + vuMarker.length);
        const pipeIdx = afterMarker.indexOf('|');
        if (pipeIdx < 0) return;
        const sendTime = parseInt(afterMarker.substring(0, pipeIdx));
        if (sendTime > 0) {
          wsRtt.add(Date.now() - sendTime);
        }
      } catch {
        // 파싱 실패 무시
      }
    }
  );

  sleep(1);
}
