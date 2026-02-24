package com.cotalk.domain.exception;

/**
 * 신고 정보를 찾을 수 없을 때 발생하는 예외.
 *
 * @author seunggu.lee
 */
public class ReportNotFoundException extends DomainException {

    public ReportNotFoundException(Long reportId) {
        super("신고를 찾을 수 없습니다. ID: " + reportId, "REPORT_NOT_FOUND", HttpStatusHint.NOT_FOUND);
    }
}
