import http from 'k6/http';
import { check, group, sleep } from 'k6';
import {
  BASE_URL,
  WS_URL,
  API_PREFIX,
  VUS,
  DURATION,
  THRESHOLDS,
  authHeaders,
  getSeededUsers,
  getSeededUser,
} from '../config.js';
import {
  connectStomp,
  stompSubscribe,
  stompSend,
  wsMessages,
} from '../helpers/websocket.js';

/**
 * Co-Talk 통합 부하 테스트 (Full User Journey)
 *
 * 사전 준비:
 *   ./k6/seed.sh <BASE_URL> <유저수>
 *
 * 실행:
 *   k6 run k6/scenarios/full-flow.js
 *   k6 run --env BASE_URL=http://your-server:8080 --env VUS=10 k6/scenarios/full-flow.js
 */

export const options = {
  scenarios: {
    user_journey: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: Math.ceil(VUS / 2) },
        { duration: DURATION, target: VUS },
        { duration: '30s', target: 0 },
      ],
    },
  },
  thresholds: {
    ...THRESHOLDS,
    'group_duration{group:::01. Browse}': ['p(95)<2000'],
    'group_duration{group:::03. WebSocket Chat}': ['p(95)<60000'],
  },
};

export function setup() {
  const users = getSeededUsers();
  if (!users || users.length === 0) {
    console.error('시딩된 사용자가 없습니다. 먼저 실행: ./k6/seed.sh <BASE_URL> <유저수>');
    return { users: [] };
  }
  console.log(`시딩된 사용자 ${users.length}명 로드 완료`);
  // SharedArray를 일반 배열로 변환하여 setup data로 전달
  const arr = [];
  for (let i = 0; i < users.length; i++) {
    arr.push(users[i]);
  }
  return { users: arr };
}

export default function (data) {
  if (!data.users || data.users.length === 0) {
    sleep(5);
    return;
  }

  const vuIndex = (__VU - 1) % data.users.length;
  const user = data.users[vuIndex];
  const token = user.accessToken;
  const params = authHeaders(token);
  let chatRoomId = null;

  // ─── 01. 프로필 + 친구목록 + 채팅방 조회 ───
  group('01. Browse', function () {
    const profileRes = http.get(`${BASE_URL}${API_PREFIX}/users/me`, params);
    check(profileRes, {
      'profile: 200': (r) => r.status === 200,
    });

    const friendsRes = http.get(`${BASE_URL}${API_PREFIX}/friends`, params);
    check(friendsRes, {
      'friends: 200': (r) => r.status === 200,
    });

    const roomsRes = http.get(`${BASE_URL}${API_PREFIX}/chat/rooms`, params);
    check(roomsRes, {
      'rooms: 200': (r) => r.status === 200,
    });

    if (roomsRes.status === 200) {
      try {
        const rooms = JSON.parse(roomsRes.body);
        if (Array.isArray(rooms) && rooms.length > 0) {
          chatRoomId = rooms[0].id || rooms[0].chatRoomId;
        }
      } catch {
        // ignore
      }
    }
  });

  sleep(1);

  // ─── 02. 채팅방 생성 (없으면) ───
  group('02. Create Room', function () {
    if (chatRoomId) return;

    const otherIndex = (vuIndex + 1) % data.users.length;
    const otherUser = data.users[otherIndex];

    if (otherUser.userId) {
      const createRes = http.post(
        `${BASE_URL}${API_PREFIX}/chat/rooms`,
        JSON.stringify({ userId2: otherUser.userId }),
        params
      );

      if (createRes.status === 200 || createRes.status === 201) {
        try {
          const room = JSON.parse(createRes.body);
          chatRoomId = room.id || room.chatRoomId;
        } catch {
          // ignore
        }
      }

      check(createRes, {
        'create room: success or conflict': (r) =>
          r.status === 200 || r.status === 201 || r.status === 409,
      });
    }
  });

  sleep(1);

  // ─── 03. WebSocket 채팅 ───
  group('03. WebSocket Chat', function () {
    if (!chatRoomId) return;

    const wsSessionMs = 30000;
    let msgCount = 0;

    connectStomp(
      WS_URL,
      token,
      function onConnected(socket) {
        socket.send(stompSubscribe(`/topic/chat/${chatRoomId}`, `room-${chatRoomId}`));
        socket.send(stompSubscribe('/user/queue/notifications', 'notif'));

        socket.send(
          stompSend('/app/chat/presence', { roomId: chatRoomId })
        );

        const interval = setInterval(function () {
          msgCount++;
          socket.send(
            stompSend('/app/chat/message', {
              roomId: chatRoomId,
              content: `Full flow msg #${msgCount} from VU-${__VU}`,
            })
          );
          wsMessages.add(1);
        }, 5000);

        socket.on('close', function () {
          clearInterval(interval);
        });
      },
      wsSessionMs
    );
  });

  sleep(2);

  // ─── 04. 정리 (메시지 조회) ───
  group('04. Cleanup', function () {
    if (!chatRoomId) return;

    const res = http.get(
      `${BASE_URL}${API_PREFIX}/chat/rooms/${chatRoomId}/messages?page=0&size=10`,
      params
    );
    check(res, {
      'read messages: 200': (r) => r.status === 200,
    });
  });

  sleep(1);
}
