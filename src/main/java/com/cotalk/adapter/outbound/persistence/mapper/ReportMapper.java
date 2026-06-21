package com.cotalk.adapter.outbound.persistence.mapper;

import com.cotalk.adapter.outbound.persistence.entity.ReportJpaEntity;
import com.cotalk.domain.entity.Report;
import org.springframework.stereotype.Component;

/**
 * Report 도메인과 ReportJpaEntity 간 매핑.
 *
 * @author seunggu.lee
 */
@Component
public class ReportMapper {

    /**
     * JPA 엔티티를 도메인 엔티티로 변환한다.
     *
     * @param jpa JPA 엔티티
     * @return 도메인 엔티티, jpa가 null이면 null
     */
    public Report toDomain(ReportJpaEntity jpa) {
        if (jpa == null) {
            return null;
        }
        return Report.builder()
                .id(jpa.getId())
                .reporterId(jpa.getReporterId())
                .reportedUserId(jpa.getReportedUserId())
                .reportedMessageId(jpa.getReportedMessageId())
                .reportedChatRoomId(jpa.getReportedChatRoomId())
                .type(jpa.getType())
                .reason(jpa.getReason())
                .description(jpa.getDescription())
                .status(jpa.getStatus())
                .adminNote(jpa.getAdminNote())
                .processedAt(jpa.getProcessedAt())
                .processedBy(jpa.getProcessedBy())
                .createdAt(jpa.getCreatedAt())
                .updatedAt(jpa.getUpdatedAt())
                .build();
    }

    /**
     * 도메인 엔티티를 JPA 엔티티로 변환한다.
     *
     * @param domain 도메인 엔티티
     * @return JPA 엔티티, domain이 null이면 null
     */
    public ReportJpaEntity toJpa(Report domain) {
        if (domain == null) {
            return null;
        }
        ReportJpaEntity jpa = ReportJpaEntity.builder()
                .id(domain.getId())
                .reporterId(domain.getReporterId())
                .reportedUserId(domain.getReportedUserId())
                .reportedMessageId(domain.getReportedMessageId())
                .reportedChatRoomId(domain.getReportedChatRoomId())
                .type(domain.getType())
                .reason(domain.getReason())
                .description(domain.getDescription())
                .status(domain.getStatus())
                .adminNote(domain.getAdminNote())
                .processedAt(domain.getProcessedAt())
                .processedBy(domain.getProcessedBy())
                .build();
        if (domain.getCreatedAt() != null) {
            jpa.setCreatedAt(domain.getCreatedAt());
        }
        if (domain.getUpdatedAt() != null) {
            jpa.setUpdatedAt(domain.getUpdatedAt());
        }
        return jpa;
    }
}
