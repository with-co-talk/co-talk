package com.cotalk.domain.exception;

/**
 * Rate Limit 초과 시 발생하는 예외.
 *
 * @author seunggu.lee
 */
public class RateLimitExceededException extends DomainException {

    private final long retryAfterSeconds;

    public RateLimitExceededException(String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    public static RateLimitExceededException tooManyRequests(long retryAfterSeconds) {
        return new RateLimitExceededException(
                "요청 한도를 초과했습니다. " + retryAfterSeconds + "초 후 다시 시도해주세요.",
                retryAfterSeconds
        );
    }
}
