package com.cotalk.domain.port.inbound.report;

import com.cotalk.domain.entity.Report;

/**
 * 신고 생성 유스케이스.
 * 사용자 또는 메시지에 대한 신고를 생성한다.
 *
 * @author seunggu.lee
 */
public interface CreateReportUseCase {

    /**
     * 사용자를 신고한다.
     *
     * @param reporterId 신고자 ID
     * @param reportedUserId 신고 대상 사용자 ID
     * @param reason 신고 사유
     * @param description 상세 설명
     * @return 생성된 신고
     */
    Report reportUser(Long reporterId, Long reportedUserId, Report.ReportReason reason, String description);

    /**
     * 메시지를 신고한다.
     *
     * @param reporterId 신고자 ID
     * @param reportedMessageId 신고 대상 메시지 ID
     * @param reason 신고 사유
     * @param description 상세 설명
     * @return 생성된 신고
     */
    Report reportMessage(Long reporterId, Long reportedMessageId, Report.ReportReason reason, String description);
}
