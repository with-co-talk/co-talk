import ws from 'k6/ws';
import { check } from 'k6';
import { Counter, Trend } from 'k6/metrics';

// Custom metrics for WebSocket
export const wsMessages = new Counter('ws_messages_sent');
export const wsMessagesReceived = new Counter('ws_messages_received');
export const wsMessageLatency = new Trend('ws_message_latency', true);

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
 * STOMP SEND 프레임 생성
 */
export function stompSend(destination, body) {
  const jsonBody = JSON.stringify(body);
  return (
    'SEND\n' +
    `destination:${destination}\n` +
    'content-type:application/json\n' +
    `content-length:${jsonBody.length}\n` +
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
  const lines = data.split('\n');
  const command = lines[0];

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
 */
export function connectStomp(wsUrl, token, onConnected, timeoutMs = 30000) {
  const url = `${wsUrl}/ws/websocket`;

  const res = ws.connect(url, {}, function (socket) {
    socket.on('open', function () {
      socket.send(stompConnect(token));
    });

    socket.on('message', function (msg) {
      const frame = parseStompFrame(msg);

      if (frame.command === 'CONNECTED') {
        if (onConnected) {
          onConnected(socket, frame);
        }
      }

      if (frame.command === 'MESSAGE') {
        wsMessagesReceived.add(1);
      }

      // STOMP heartbeat (empty line)
      if (msg.trim() === '') {
        socket.send('\n');
      }
    });

    socket.on('error', function (e) {
      console.error(`WebSocket error: ${e}`);
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
