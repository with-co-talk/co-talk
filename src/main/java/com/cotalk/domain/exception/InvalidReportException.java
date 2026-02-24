package com.cotalk.domain.exception;

/**
 * 유효하지 않은 신고 요청일 때 발생하는 예외.
 *
 * @author seunggu.lee
 */
public class InvalidReportException extends DomainException {

    public InvalidReportException(String message) {
        super(message, "INVALID_REPORT", HttpStatusHint.BAD_REQUEST);
    }
}
