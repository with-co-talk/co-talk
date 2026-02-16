import { SharedArray } from 'k6/data';

/**
 * Co-Talk k6 Load Test - Shared Configuration
 *
 * 환경변수로 설정 가능:
 *   BASE_URL    - API 서버 주소 (기본: http://localhost:8080)
 *   WS_URL      - WebSocket 서버 주소 (기본: ws://localhost:8080)
 *   TEST_EMAIL  - 테스트 계정 이메일 prefix (기본: loadtest)
 *   TEST_PASS   - 테스트 계정 비밀번호 (기본: Test1234!@)
 *   VUS         - 가상 사용자 수 (기본: 10)
 *   DURATION    - 테스트 지속 시간 (기본: 1m)
 *
 * 사전 준비:
 *   ./k6/seed.sh <BASE_URL> <유저수>
 *   → k6/data/users.json 에 토큰 저장됨
 */

export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// WS_URL 자동 추론: https → wss, http → ws
function deriveWsUrl(baseUrl) {
  return baseUrl.replace(/^https:/, 'wss:').replace(/^http:/, 'ws:');
}
export const WS_URL = __ENV.WS_URL || deriveWsUrl(BASE_URL);
export const API_PREFIX = '/api/v1';

export const TEST_EMAIL_PREFIX = __ENV.TEST_EMAIL || 'loadtest';
export const TEST_PASSWORD = __ENV.TEST_PASS || 'Test1234!@';
export const TEST_DOMAIN = 'test.cotalk.com';

export const VUS = parseInt(__ENV.VUS || '10');
export const DURATION = __ENV.DURATION || '1m';

// Thresholds
export const THRESHOLDS = {
  http_req_duration: ['p(95)<500', 'p(99)<1000'],
  http_req_failed: ['rate<0.05'],
  ws_connecting: ['p(95)<1000'],
};

/**
 * Snowflake ID 정밀도 보존 JSON 파서.
 * JavaScript Number는 2^53까지만 정확하므로, 16자리 이상 숫자를 문자열로 변환 후 파싱.
 * 예: {"roomId": 281840969769287680} → {"roomId": "281840969769287680"}
 */
export function safeParseBigInts(jsonStr) {
  return JSON.parse(jsonStr.replace(/(:\s*)(\d{16,})/g, '$1"$2"'));
}

// seed.sh로 미리 생성된 사용자 로드 (SharedArray: VU간 메모리 공유)
let _seededUsers = null;
try {
  _seededUsers = new SharedArray('seeded-users', function () {
    return safeParseBigInts(open('./data/users.json'));
  });
} catch {
  _seededUsers = null;
}

/**
 * 시딩된 사용자 배열 반환.
 * seed.sh 미실행 시 null 반환.
 */
export function getSeededUsers() {
  return _seededUsers;
}

/**
 * VU에 할당된 시딩 사용자 반환.
 * 없으면 null.
 */
export function getSeededUser(vuIndex) {
  if (!_seededUsers || _seededUsers.length === 0) return null;
  return _seededUsers[vuIndex % _seededUsers.length];
}

// Generate test user credentials
export function testUser(vuId) {
  return {
    email: `${TEST_EMAIL_PREFIX}+${vuId}@${TEST_DOMAIN}`,
    password: TEST_PASSWORD,
    nickname: `${TEST_EMAIL_PREFIX}-user-${vuId}`,
  };
}

// k6 rate limit bypass token (nginx + app 양쪽 우회)
const K6_TOKEN = __ENV.K6_TOKEN || '';

// Common HTTP headers
export function authHeaders(token) {
  const h = {
    'Content-Type': 'application/json',
    Authorization: `Bearer ${token}`,
  };
  if (K6_TOKEN) h['X-K6-Token'] = K6_TOKEN;
  return { headers: h };
}

export function jsonHeaders() {
  const h = { 'Content-Type': 'application/json' };
  if (K6_TOKEN) h['X-K6-Token'] = K6_TOKEN;
  return { headers: h };
}
