import { check, sleep } from 'k6';
import http from 'k6/http';
import { Counter, Rate, Trend } from 'k6/metrics';
import {
  WS_URL,
  BASE_URL,
  API_PREFIX,
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
 * Co-Talk WebSocket 한계점(Breakpoint) 테스트
 *
 * VU를 단계적으로 올리며 서버가 버티는 한계를 측정합니다.
 * 10 → 50 → 100 → 150 → 200 (각 단계 40초 유지)
 *
 * 실행:
 *   K6_TOKEN=cotalk-k6-bypass-2026 k6 run \
 *     --env BASE_URL=https://co-talk.sgyj-dev.synology.me \
 *     --env K6_TOKEN=cotalk-k6-bypass-2026 \
 *     k6/scenarios/breakpoint.js
 */

const MAX_VUS = parseInt(__ENV.MAX_VUS || '200');
const SESSION_MS = 20000; // 20초 (빠른 회전)

// 연결 실패 추적
const wsConnectFailed = new Counter('ws_connect_failed');
const wsConnectSuccess = new Counter('ws_connect_success');
const wsErrorRate = new Rate('ws_error_rate');

export const options = {
  scenarios: {
    breakpoint: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        // 워밍업
        { duration: '10s', target: 10 },
        { duration: '40s', target: 10 },
        // 단계별 증가
        { duration: '10s', target: 50 },
        { duration: '40s', target: 50 },
        { duration: '10s', target: 100 },
        { duration: '40s', target: 100 },
        { duration: '10s', target: 150 },
        { duration: '40s', target: 150 },
        { duration: '10s', target: MAX_VUS },
        { duration: '40s', target: MAX_VUS },
        // 쿨다운
        { duration: '10s', target: 0 },
      ],
      gracefulRampDown: '10s',
      gracefulStop: '25s',
    },
  },
  // 한계점 테스트이므로 threshold를 느슨하게 설정 (깨지는 지점을 관찰)
  thresholds: {
    ws_rtt: [{ threshold: 'avg<5000', abortOnFail: false }],
    ws_error_rate: [{ threshold: 'rate<0.5', abortOnFail: true }],
    ws_connect_success: ['count>0'],
  },
};

function extractRoomId(obj) {
  return obj.roomId || obj.id || obj.chatRoomId;
}

export function setup() {
  const users = getSeededUsers();
  if (!users || users.length === 0) {
    console.error('시딩된 사용자가 없습니다. 먼저 실행: ./k6/seed.sh <BASE_URL> <유저수>');
    return { users: [], rooms: [] };
  }
  console.log(`시딩된 사용자 ${users.length}명 로드 완료`);

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
      } catch { /* skip */ }
    }

    if (!roomId) {
      const res = http.post(
        `${BASE_URL}${API_PREFIX}/chat/rooms`,
        `{"userId2":${user2.userId}}`,
        params
      );
      if (res.status === 200 || res.status === 201) {
        try {
          const regex = /"roomId"\s*:\s*(\d+)/;
          const m = res.body.match(regex);
          if (m) roomId = m[1];
        } catch { /* ignore */ }
      }

      if (!roomId) {
        const retryRes = http.get(`${BASE_URL}${API_PREFIX}/chat/rooms`, params);
        if (retryRes.status === 200) {
          try {
            const parsed = safeParseBigInts(retryRes.body);
            const roomList = parsed.rooms || parsed;
            if (Array.isArray(roomList) && roomList.length > 0) {
              roomId = extractRoomId(roomList[0]);
            }
          } catch { /* skip */ }
        }
      }
    }

    if (roomId) {
      rooms.push({
        roomId,
        user1Id: user1.userId,
        user2Id: user2.userId,
      });
    }
  }

  console.log(`채팅방 ${rooms.length}개 준비 완료`);
  console.log(`\n=== 한계점 테스트 시작 ===`);
  console.log(`단계: 10 → 50 → 100 → 150 → ${MAX_VUS} VUs`);
  console.log(`세션: ${SESSION_MS / 1000}초, 메시지 간격: 3초\n`);
  return { users: arr, rooms };
}

export default function (data) {
  if (data.rooms.length === 0) {
    sleep(5);
    return;
  }

  const vuIndex = (__VU - 1) % data.users.length;
  const user = data.users[vuIndex];

  // 방 매핑 (멤버인 방 우선, 없으면 아무 방)
  let roomId = null;
  for (let r = 0; r < data.rooms.length; r++) {
    const rm = data.rooms[r];
    if (String(user.userId) === String(rm.user1Id) ||
        String(user.userId) === String(rm.user2Id)) {
      roomId = rm.roomId;
      break;
    }
  }
  if (!roomId) {
    roomId = data.rooms[vuIndex % data.rooms.length].roomId;
  }

  runSession(user, roomId);
}

function runSession(user, roomId) {
  let messageCount = 0;
  const vuMarker = `k6|${__VU}|`;

  const res = connectStomp(
    WS_URL,
    user.accessToken,
    function onConnected(socket) {
      socket.send(stompSubscribe('/user/queue/errors', 'errors'));
      socket.send(stompSubscribe(`/topic/chat/room/${roomId}`, `room-${roomId}`));

      // 3초마다 메시지 전송
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
    },
    SESSION_MS,
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
      } catch { /* ignore */ }
    }
  );

  const connected = res && res.status === 101;
  if (connected) {
    wsConnectSuccess.add(1);
    wsErrorRate.add(0);
  } else {
    wsConnectFailed.add(1);
    wsErrorRate.add(1);
    console.warn(`VU-${__VU}: 연결 실패 (status=${res ? res.status : 'null'})`);
  }

  sleep(1);
}
