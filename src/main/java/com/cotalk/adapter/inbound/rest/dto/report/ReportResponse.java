package com.cotalk.adapter.inbound.rest.dto.report;

import com.cotalk.domain.entity.Report;

import java.time.LocalDateTime;

/**
 * 신고 응답 DTO.
 *
 * @param id                신고 ID
 * @param reporterId        신고자 ID
 * @param reportedUserId    신고된 사용자 ID
 * @param reportedMessageId 신고된 메시지 ID
 * @param type              신고 유형
 * @param reason            신고 사유
 * @param description       상세 설명
 * @param status            처리 상태
 * @param createdAt         생성 일시
 * @author seunggu.lee
 */
public record ReportResponse(
        Long id,
        Long reporterId,
        Long reportedUserId,
        Long reportedMessageId,
        String type,
        String reason,
        String description,
        String status,
        LocalDateTime createdAt
) {
    /**
     * Report 엔티티로부터 DTO를 생성한다.
     *
     * @param report Report 엔티티
     * @return ReportResponse 인스턴스
     */
    public static ReportResponse from(Report report) {
        return new ReportResponse(
                report.getId(),
                report.getReporterId(),
                report.getReportedUserId(),
                report.getReportedMessageId(),
                report.getType().name(),
                report.getReason().name(),
                report.getDescription(),
                report.getStatus().name(),
                report.getCreatedAt()
        );
    }
}
