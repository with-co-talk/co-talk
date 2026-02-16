import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE_URL, API_PREFIX, authHeaders, getSeededUsers } from '../config.js';

/**
 * Co-Talk 스파이크 테스트
 *
 * 사전 준비:
 *   ./k6/seed.sh <BASE_URL> 100
 *
 * 실행:
 *   k6 run k6/scenarios/spike.js
 */

export const options = {
  scenarios: {
    spike: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 5 },
        { duration: '1m', target: 5 },
        { duration: '10s', target: 80 },
        { duration: '1m', target: 80 },
        { duration: '10s', target: 5 },
        { duration: '1m', target: 5 },
        { duration: '10s', target: 0 },
      ],
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<3000'],
    http_req_failed: ['rate<0.10'],
  },
};

export function setup() {
  const users = getSeededUsers();
  if (!users || users.length === 0) {
    console.error('시딩된 사용자가 없습니다. 먼저 실행: ./k6/seed.sh <BASE_URL> 100');
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
  const params = authHeaders(user.accessToken);

  const profileRes = http.get(`${BASE_URL}${API_PREFIX}/users/me`, params);
  check(profileRes, { 'spike: profile 200': (r) => r.status === 200 });

  sleep(0.2);

  const roomsRes = http.get(`${BASE_URL}${API_PREFIX}/chat/rooms`, params);
  check(roomsRes, { 'spike: rooms 200': (r) => r.status === 200 });

  sleep(0.5 + Math.random());
}
