import http from 'k6/http';
import { check, fail } from 'k6';
import { BASE_URL, API_PREFIX, jsonHeaders, testUser } from '../config.js';

/**
 * 테스트 사용자 회원가입
 * 이미 존재하면 무시 (409 허용)
 */
export function signup(vuId) {
  const user = testUser(vuId);
  const res = http.post(
    `${BASE_URL}${API_PREFIX}/auth/signup`,
    JSON.stringify({
      email: user.email,
      password: user.password,
      nickname: user.nickname,
    }),
    jsonHeaders()
  );

  // 201 Created or 409 Conflict (already exists) both OK
  check(res, {
    'signup: status 201 or 409': (r) => r.status === 201 || r.status === 409,
  });

  return res.status === 201;
}

/**
 * 로그인 후 JWT 토큰 반환
 */
export function login(vuId) {
  const user = testUser(vuId);
  const res = http.post(
    `${BASE_URL}${API_PREFIX}/auth/login`,
    JSON.stringify({
      email: user.email,
      password: user.password,
    }),
    jsonHeaders()
  );

  const success = check(res, {
    'login: status 200': (r) => r.status === 200,
    'login: has accessToken': (r) => {
      try {
        return JSON.parse(r.body).accessToken !== undefined;
      } catch {
        return false;
      }
    },
  });

  if (!success) {
    fail(`Login failed for VU ${vuId}: ${res.status} ${res.body}`);
  }

  const body = JSON.parse(res.body);
  return {
    accessToken: body.accessToken,
    refreshToken: body.refreshToken,
    userId: body.userId,
  };
}

/**
 * setup 단계에서 사용자 등록 + 로그인 (VU별)
 */
export function setupUser(vuId) {
  signup(vuId);
  return login(vuId);
}

/**
 * 토큰 갱신
 */
export function refreshToken(token) {
  const res = http.post(
    `${BASE_URL}${API_PREFIX}/auth/refresh`,
    JSON.stringify({ refreshToken: token }),
    jsonHeaders()
  );

  check(res, {
    'refresh: status 200': (r) => r.status === 200,
  });

  if (res.status === 200) {
    return JSON.parse(res.body).accessToken;
  }
  return null;
}
