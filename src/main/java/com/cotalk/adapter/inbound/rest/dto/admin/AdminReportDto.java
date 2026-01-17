package com.cotalk.adapter.inbound.rest.dto.admin;

import com.cotalk.domain.entity.Report;

import java.time.LocalDateTime;

/**
 * 관리자용 신고 정보 DTO.
 *
 * @param id                 신고 ID
 * @param reporterId         신고자 ID
 * @param reportedUserId     신고된 사용자 ID
 * @param reportedMessageId  신고된 메시지 ID
 * @param reportedChatRoomId 신고된 채팅방 ID
 * @param type               신고 유형
 * @param reason             신고 사유
 * @param description        상세 설명
 * @param status             처리 상태
 * @param adminNote          관리자 메모
 * @param processedBy        처리 관리자 ID
 * @param processedAt        처리 일시
 * @param createdAt          생성 일시
 * @author seunggu.lee
 */
public record AdminReportDto(
        Long id,
        Long reporterId,
        Long reportedUserId,
        Long reportedMessageId,
        Long reportedChatRoomId,
        String type,
        String reason,
        String description,
        String status,
        String adminNote,
        Long processedBy,
        LocalDateTime processedAt,
        LocalDateTime createdAt
) {
    /**
     * Report 엔티티로부터 DTO를 생성한다.
     *
     * @param report Report 엔티티
     * @return AdminReportDto 인스턴스
     */
    public static AdminReportDto from(Report report) {
        return new AdminReportDto(
                report.getId(),
                report.getReporterId(),
                report.getReportedUserId(),
                report.getReportedMessageId(),
                report.getReportedChatRoomId(),
                report.getType().name(),
                report.getReason().name(),
                report.getDescription(),
                report.getStatus().name(),
                report.getAdminNote(),
                report.getProcessedBy(),
                report.getProcessedAt(),
                report.getCreatedAt()
        );
    }
}
