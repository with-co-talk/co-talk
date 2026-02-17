import http from 'k6/http';
import { check, group, sleep } from 'k6';
import {
  BASE_URL,
  API_PREFIX,
  VUS,
  DURATION,
  THRESHOLDS,
  authHeaders,
  getSeededUsers,
  safeParseBigInts,
} from '../config.js';

/**
 * Co-Talk REST API 부하 테스트
 *
 * 사전 준비:
 *   ./k6/seed.sh <BASE_URL> <유저수>
 *
 * 실행:
 *   k6 run k6/scenarios/rest-api.js
 *   k6 run --env BASE_URL=http://your-server:8080 --env VUS=50 k6/scenarios/rest-api.js
 */

export const options = {
  scenarios: {
    rest_api_load: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: VUS },
        { duration: DURATION, target: VUS },
        { duration: '15s', target: VUS * 2 },
        { duration: '30s', target: VUS },
        { duration: '15s', target: 0 },
      ],
    },
  },
  thresholds: THRESHOLDS,
};

export function setup() {
  const users = getSeededUsers();
  if (!users || users.length === 0) {
    console.error('시딩된 사용자가 없습니다. 먼저 실행: ./k6/seed.sh <BASE_URL> <유저수>');
    return { users: [] };
  }
  console.log(`시딩된 사용자 ${users.length}명 로드 완료`);
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

  // 1. 내 프로필 조회
  group('GET /users/me', function () {
    const res = http.get(`${BASE_URL}${API_PREFIX}/users/me`, params);
    check(res, {
      'profile: status 200': (r) => r.status === 200,
    });
  });

  sleep(1);

  // 2. 친구 목록 조회
  group('GET /friends', function () {
    const res = http.get(`${BASE_URL}${API_PREFIX}/friends`, params);
    check(res, {
      'friends: status 200': (r) => r.status === 200,
    });
  });

  sleep(0.5);

  // 3. 채팅방 목록 조회
  let chatRooms = [];
  group('GET /chat/rooms', function () {
    const res = http.get(`${BASE_URL}${API_PREFIX}/chat/rooms`, params);
    check(res, {
      'rooms: status 200': (r) => r.status === 200,
    });
    if (res.status === 200) {
      try {
        chatRooms = safeParseBigInts(res.body);
      } catch {
        chatRooms = [];
      }
    }
  });

  sleep(0.5);

  // 4. 채팅방 메시지 조회 (첫 번째 채팅방)
  if (Array.isArray(chatRooms) && chatRooms.length > 0) {
    group('GET /chat/rooms/{id}/messages', function () {
      const roomId = chatRooms[0].id || chatRooms[0].chatRoomId;
      const res = http.get(
        `${BASE_URL}${API_PREFIX}/chat/rooms/${roomId}/messages?page=0&size=20`,
        params
      );
      check(res, {
        'messages: status 200': (r) => r.status === 200,
      });
    });
  }

  sleep(0.5);

  // 5. 사용자 검색
  group('GET /users/search', function () {
    const keyword = `loadtest-user-${Math.floor(Math.random() * 10) + 1}`;
    const res = http.get(
      `${BASE_URL}${API_PREFIX}/users/search?query=${encodeURIComponent(keyword)}`,
      params
    );
    check(res, {
      'search: status 200': (r) => r.status === 200,
    });
  });

  sleep(1);

  // 6. 채팅방 생성 (10% 확률)
  if (Math.random() < 0.1) {
    group('POST /chat/rooms', function () {
      const otherIndex = (vuIndex + Math.floor(Math.random() * (data.users.length - 1)) + 1) % data.users.length;
      const otherUser = data.users[otherIndex];

      const res = http.post(
        `${BASE_URL}${API_PREFIX}/chat/rooms`,
        JSON.stringify({ userId2: otherUser.userId }),
        params
      );
      check(res, {
        'create room: status 200 or 201 or 409': (r) =>
          r.status === 200 || r.status === 201 || r.status === 409,
      });
    });
  }

  sleep(0.5);

  // 7. REST 메시지 전송 (채팅방이 있을 때만, 20% 확률)
  if (Array.isArray(chatRooms) && chatRooms.length > 0 && Math.random() < 0.2) {
    group('POST /chat/messages', function () {
      const roomId = chatRooms[0].id || chatRooms[0].chatRoomId;
      const res = http.post(
        `${BASE_URL}${API_PREFIX}/chat/messages`,
        JSON.stringify({
          chatRoomId: roomId,
          content: `k6 load test message at ${new Date().toISOString()}`,
        }),
        params
      );
      check(res, {
        'send message: status 200 or 201': (r) => r.status === 200 || r.status === 201,
      });
    });
  }

  sleep(1);
}
