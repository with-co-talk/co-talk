package com.cotalk.domain.exception;

/**
 * 도메인 예외에 대한 HTTP 상태 코드 힌트.
 * 인프라 레이어의 GlobalExceptionHandler에서 적절한 HTTP 상태를 결정하는 데 사용한다.
 *
 * @author seunggu.lee
 */
public enum HttpStatusHint {
    BAD_REQUEST(400),
    UNAUTHORIZED(401),
    FORBIDDEN(403),
    NOT_FOUND(404),
    CONFLICT(409),
    TOO_MANY_REQUESTS(429),
    INTERNAL_ERROR(500),
    SERVICE_UNAVAILABLE(503);

    private final int statusCode;

    HttpStatusHint(int statusCode) {
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
