import ws from 'k6/ws';
import { check } from 'k6';
import { Counter, Trend } from 'k6/metrics';

// Custom metrics for WebSocket
export const wsMessages = new Counter('ws_messages_sent');
export const wsMessagesReceived = new Counter('ws_messages_received');
export const wsMessageLatency = new Trend('ws_message_latency', true);
export const wsRtt = new Trend('ws_rtt', true);

// k6 rate limit bypass token (nginx WebSocket rate limit 우회)
const K6_TOKEN = __ENV.K6_TOKEN || '';

// STOMP frame helpers
const NULL_CHAR = '\u0000';

/**
 * STOMP CONNECT 프레임 생성
 */
export function stompConnect(token) {
  return (
    'CONNECT\n' +
    'accept-version:1.2\n' +
    'heart-beat:10000,10000\n' +
    `Authorization:Bearer ${token}\n` +
    '\n' +
    NULL_CHAR
  );
}

/**
 * STOMP SUBSCRIBE 프레임 생성
 */
export function stompSubscribe(destination, id) {
  return (
    'SUBSCRIBE\n' +
    `id:sub-${id}\n` +
    `destination:${destination}\n` +
    '\n' +
    NULL_CHAR
  );
}

/**
 * STOMP SEND 프레임 생성.
 * 문자열로 보존된 Snowflake ID를 JSON 숫자로 복원하여 서버 Long 타입과 호환.
 */
export function stompSend(destination, body) {
  // JSON.stringify 후, 16자리 이상 숫자 문자열을 JSON 숫자로 복원
  // "roomId":"281840969769287680" → "roomId":281840969769287680
  const jsonBody = JSON.stringify(body).replace(/:\s*"(\d{16,})"/g, (match, num) => `:${num}`);
  return (
    'SEND\n' +
    `destination:${destination}\n` +
    'content-type:application/json\n' +
    '\n' +
    jsonBody +
    NULL_CHAR
  );
}

/**
 * STOMP DISCONNECT 프레임
 */
export function stompDisconnect() {
  return 'DISCONNECT\n' + 'receipt:disconnect-receipt\n' + '\n' + NULL_CHAR;
}

/**
 * STOMP 프레임 파싱
 */
export function parseStompFrame(data) {
  const normalized = data.replace(/\r\n/g, '\n');
  const lines = normalized.split('\n');
  const command = lines[0].trim();

  const headers = {};
  let i = 1;
  while (i < lines.length && lines[i] !== '') {
    const [key, ...valueParts] = lines[i].split(':');
    headers[key] = valueParts.join(':');
    i++;
  }

  // body is everything after the blank line, minus the null char
  const body = lines
    .slice(i + 1)
    .join('\n')
    .replace(NULL_CHAR, '');

  return { command, headers, body };
}

/**
 * WebSocket + STOMP 연결 헬퍼
 *
 * @param {string} wsUrl - WebSocket URL
 * @param {string} token - JWT access token
 * @param {function} onConnected - CONNECTED 프레임 수신 시 콜백(socket)
 * @param {number} timeoutMs - 연결 유지 시간 (기본 30초)
 * @param {function} onMessage - MESSAGE 프레임 수신 시 콜백(frame) (RTT 측정 등)
 */
export function connectStomp(wsUrl, token, onConnected, timeoutMs = 30000, onMessage = null) {
  // 순수 WebSocket 엔드포인트 사용 (NOT SockJS /ws/websocket)
  // WebSocketConfig.java: addEndpoint("/ws") (SockJS 없이)
  const url = `${wsUrl}/ws`;

  const headers = {
    Authorization: `Bearer ${token}`,
  };
  // nginx WebSocket rate limit(ws_connect zone) 우회
  if (K6_TOKEN) {
    headers['X-K6-Token'] = K6_TOKEN;
  }

  const res = ws.connect(url, { headers }, function (socket) {
    socket.on('open', function () {
      socket.send(stompConnect(token));
    });

    socket.on('message', function (msg) {
      // STOMP heartbeat (empty line) - 먼저 체크
      if (msg.trim() === '' || msg.trim() === '\u0000') {
        socket.send('\n');
        return;
      }

      const frame = parseStompFrame(msg);

      if (frame.command === 'CONNECTED') {
        if (onConnected) {
          onConnected(socket, frame);
        }
      } else if (frame.command === 'ERROR') {
        console.error(`STOMP ERROR: ${frame.headers.message || frame.body}`);
      } else if (frame.command === 'MESSAGE') {
        wsMessagesReceived.add(1);
        if (onMessage) {
          onMessage(frame);
        }
      }
    });

    socket.on('error', function (e) {
      console.error(`WebSocket error: ${e}`);
    });

    socket.on('close', function () {
      // 정상 종료 - 로깅 불필요
    });

    socket.setTimeout(function () {
      socket.send(stompDisconnect());
      socket.close();
    }, timeoutMs);
  });

  check(res, {
    'ws: status 101': (r) => r && r.status === 101,
  });

  return res;
}
