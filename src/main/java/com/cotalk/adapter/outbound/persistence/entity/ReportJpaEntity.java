package com.cotalk.adapter.outbound.persistence.entity;

import com.cotalk.domain.entity.Report;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 신고 JPA 엔티티.
 * persistence 계층 전용이며, 도메인 Report와 매핑된다.
 *
 * @author seunggu.lee
 */
@Entity
@Table(name = "reports")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ReportJpaEntity extends BaseJpaEntity {

    @Id
    private Long id;

    @Column(name = "reporter_id", nullable = false)
    private Long reporterId;

    @Column(name = "reported_user_id")
    private Long reportedUserId;

    @Column(name = "reported_message_id")
    private Long reportedMessageId;

    @Column(name = "reported_chat_room_id")
    private Long reportedChatRoomId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Report.ReportType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Report.ReportReason reason;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Report.ReportStatus status = Report.ReportStatus.PENDING;

    @Column(name = "admin_note", length = 1000)
    private String adminNote;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "processed_by")
    private Long processedBy;
}
