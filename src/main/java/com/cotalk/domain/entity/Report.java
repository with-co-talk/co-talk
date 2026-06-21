package com.cotalk.domain.entity;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * 신고 도메인 엔티티.
 * 사용자, 메시지, 채팅방에 대한 신고 정보를 나타낸다.
 * 순수 도메인 모델이며 JPA 어노테이션은 persistence 계층에만 존재한다.
 *
 * @author seunggu.lee
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@SuperBuilder
public class Report extends DomainBaseEntity {

    private Long id;

    private Long reporterId;

    private Long reportedUserId;

    private Long reportedMessageId;

    private Long reportedChatRoomId;

    private ReportType type;

    private ReportReason reason;

    private String description;

    @Builder.Default
    private ReportStatus status = ReportStatus.PENDING;

    private String adminNote;

    private LocalDateTime processedAt;

    private Long processedBy;

    /**
     * 신고 유형을 나타내는 열거형.
     *
     * @author seunggu.lee
     */
    public enum ReportType {
        /** 사용자 신고 */
        USER,
        /** 메시지 신고 */
        MESSAGE,
        /** 채팅방 신고 */
        CHAT_ROOM
    }

    /**
     * 신고 사유를 나타내는 열거형.
     *
     * @author seunggu.lee
     */
    public enum ReportReason {
        /** 스팸 */
        SPAM,
        /** 괴롭힘 */
        HARASSMENT,
        /** 부적절한 콘텐츠 */
        INAPPROPRIATE_CONTENT,
        /** 가짜 프로필 */
        FAKE_PROFILE,
        /** 사기 */
        SCAM,
        /** 혐오 발언 */
        HATE_SPEECH,
        /** 폭력 */
        VIOLENCE,
        /** 기타 */
        OTHER
    }

    /**
     * 신고 처리 상태를 나타내는 열거형.
     *
     * @author seunggu.lee
     */
    public enum ReportStatus {
        /** 대기 중 */
        PENDING,
        /** 검토 중 */
        REVIEWING,
        /** 처리 완료 */
        RESOLVED,
        /** 기각됨 */
        DISMISSED
    }

    /**
     * 신고를 처리한다.
     *
     * @param newStatus 새 처리 상태
     * @param adminNote 관리자 메모
     * @param adminId 처리한 관리자 ID
     * @param now 현재 시간
     */
    public void process(ReportStatus newStatus, String adminNote, Long adminId, LocalDateTime now) {
        this.status = newStatus;
        this.adminNote = adminNote;
        this.processedBy = adminId;
        this.processedAt = now;
    }

    /**
     * 신고가 대기 상태인지 확인한다.
     *
     * @return 대기 상태이면 true, 그렇지 않으면 false
     */
    public boolean isPending() {
        return this.status == ReportStatus.PENDING;
    }

    /**
     * 신고가 처리 완료 상태인지 확인한다.
     *
     * @return 처리 완료 상태이면 true, 그렇지 않으면 false
     */
    public boolean isResolved() {
        return this.status == ReportStatus.RESOLVED;
    }
}
