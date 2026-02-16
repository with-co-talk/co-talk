import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE_URL, API_PREFIX, authHeaders, getSeededUsers } from '../config.js';

/**
 * Co-Talk 스트레스 테스트
 *
 * 사전 준비:
 *   ./k6/seed.sh <BASE_URL> <유저수>   (MAX_VUS 이상 생성 권장)
 *
 * 실행:
 *   k6 run k6/scenarios/stress.js
 *   k6 run --env BASE_URL=http://your-server:8080 --env MAX_VUS=50 k6/scenarios/stress.js
 */

const MAX_VUS = parseInt(__ENV.MAX_VUS || '100');

export const options = {
  scenarios: {
    stress: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '1m', target: 10 },
        { duration: '2m', target: 10 },
        { duration: '1m', target: 30 },
        { duration: '2m', target: 30 },
        { duration: '1m', target: 50 },
        { duration: '2m', target: 50 },
        { duration: '1m', target: MAX_VUS },
        { duration: '2m', target: MAX_VUS },
        { duration: '2m', target: 0 },
      ],
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<2000', 'p(99)<5000'],
    http_req_failed: ['rate<0.15'],
  },
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
  const params = authHeaders(user.accessToken);

  const actions = [
    () => http.get(`${BASE_URL}${API_PREFIX}/users/me`, params),
    () => http.get(`${BASE_URL}${API_PREFIX}/friends`, params),
    () => http.get(`${BASE_URL}${API_PREFIX}/chat/rooms`, params),
    () => http.get(`${BASE_URL}${API_PREFIX}/users/search?query=loadtest`, params),
  ];

  const callCount = 2 + Math.floor(Math.random() * 2);
  for (let i = 0; i < callCount; i++) {
    const action = actions[Math.floor(Math.random() * actions.length)];
    const res = action();
    check(res, {
      'stress: status < 500': (r) => r.status < 500,
    });
    sleep(0.3 + Math.random() * 0.7);
  }

  sleep(1);
}
