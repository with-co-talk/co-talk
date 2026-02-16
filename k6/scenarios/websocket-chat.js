import { check, sleep } from 'k6';
import {
  WS_URL,
  BASE_URL,
  API_PREFIX,
  VUS,
  DURATION,
  THRESHOLDS,
  authHeaders,
} from '../config.js';
import { signup, login } from '../helpers/auth.js';
import {
  connectStomp,
  stompSubscribe,
  stompSend,
  wsMessages,
  wsMessageLatency,
} from '../helpers/websocket.js';
import http from 'k6/http';

/**
 * Co-Talk WebSocket/STOMP 부하 테스트
 *
 * 시나리오:
 *   1. 로그인 (REST)
 *   2. WebSocket STOMP 연결
 *   3. 채팅방 구독
 *   4. 주기적으로 메시지 전송
 *   5. 타이핑 상태 전송
 *   6. 프레즌스 핑
 *
 * 실행:
 *   k6 run k6/scenarios/websocket-chat.js
 *   k6 run --env BASE_URL=http://your-server:8080 --env WS_URL=ws://your-server:8080 --env VUS=20 k6/scenarios/websocket-chat.js
 */

export const options = {
  scenarios: {
    websocket_chat: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '20s', target: VUS },
        { duration: DURATION, target: VUS },
        { duration: '10s', target: 0 },
      ],
    },
  },
  thresholds: {
    ...THRESHOLDS,
    ws_messages_sent: ['count>0'],
    ws_messages_received: ['count>0'],
    ws_message_latency: ['p(95)<200'],
  },
};

// setup: 사용자 생성 + 채팅방 준비
export function setup() {
  const users = [];

  // 사용자 생성 및 로그인
  for (let i = 1; i <= VUS; i++) {
    signup(i);
    const auth = login(i);
    users.push({ vuId: i, ...auth });
  }

  // 페어링: 인접한 사용자끼리 채팅방 생성
  const rooms = [];
  for (let i = 0; i < users.length - 1; i += 2) {
    const user1 = users[i];
    const user2 = users[i + 1];
    const params = authHeaders(user1.accessToken);

    const res = http.post(
      `${BASE_URL}${API_PREFIX}/chat/rooms`,
      JSON.stringify({ userId2: user2.userId }),
      params
    );

    if (res.status === 200 || res.status === 201) {
      try {
        const room = JSON.parse(res.body);
        const roomId = room.id || room.chatRoomId;
        rooms.push({ roomId, user1Index: i, user2Index: i + 1 });
      } catch {
        // room already exists - try to get from room list
      }
    } else if (res.status === 409) {
      // Room already exists, get room list
      const listRes = http.get(`${BASE_URL}${API_PREFIX}/chat/rooms`, params);
      if (listRes.status === 200) {
        try {
          const roomList = JSON.parse(listRes.body);
          if (roomList.length > 0) {
            const roomId = roomList[0].id || roomList[0].chatRoomId;
            rooms.push({ roomId, user1Index: i, user2Index: i + 1 });
          }
        } catch {
          // skip
        }
      }
    }
  }

  return { users, rooms };
}

export default function (data) {
  if (data.rooms.length === 0) {
    console.warn('No chat rooms available for WebSocket test');
    sleep(5);
    return;
  }

  const vuIndex = (__VU - 1) % data.users.length;
  const user = data.users[vuIndex];

  // 이 VU가 참여할 채팅방 선택
  const roomInfo = data.rooms[vuIndex % data.rooms.length];
  const roomId = roomInfo.roomId;

  // WebSocket 연결 시간 (60초 유지)
  const sessionDuration = 60000;
  let messageCount = 0;

  connectStomp(
    WS_URL,
    user.accessToken,
    function onConnected(socket) {
      // 채팅방 구독
      socket.send(stompSubscribe(`/topic/chat/${roomId}`, `room-${roomId}`));

      // 사용자별 큐 구독 (알림 등)
      socket.send(stompSubscribe('/user/queue/notifications', 'notifications'));

      // 채팅방 목록 업데이트 구독
      socket.send(stompSubscribe('/user/queue/chat-list', 'chat-list'));

      // 3초마다 메시지 전송
      const msgInterval = setInterval(function () {
        messageCount++;
        const sendTime = Date.now();

        socket.send(
          stompSend('/app/chat/message', {
            roomId: roomId,
            content: `k6 msg #${messageCount} from VU-${__VU} at ${new Date().toISOString()}`,
          })
        );
        wsMessages.add(1);

        // 메시지 레이턴시 추정 (전송 시점 기록)
        wsMessageLatency.add(Date.now() - sendTime);
      }, 3000);

      // 10초마다 타이핑 상태 전송
      const typingInterval = setInterval(function () {
        socket.send(
          stompSend('/app/chat/typing', {
            roomId: roomId,
            isTyping: true,
          })
        );

        // 2초 후 타이핑 해제
        setTimeout(function () {
          socket.send(
            stompSend('/app/chat/typing', {
              roomId: roomId,
              isTyping: false,
            })
          );
        }, 2000);
      }, 10000);

      // 15초마다 프레즌스 핑
      const presenceInterval = setInterval(function () {
        socket.send(
          stompSend('/app/chat/presence', {
            roomId: roomId,
          })
        );
      }, 15000);

      // 세션 종료 시 정리
      socket.on('close', function () {
        clearInterval(msgInterval);
        clearInterval(typingInterval);
        clearInterval(presenceInterval);
      });
    },
    sessionDuration
  );

  sleep(2);
}
