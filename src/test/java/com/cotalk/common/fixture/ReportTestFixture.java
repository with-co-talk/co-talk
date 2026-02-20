package com.cotalk.common.fixture;

import com.cotalk.domain.entity.Report;

/**
 * Report 엔티티 테스트 픽스처.
 * 테스트에서 반복적으로 사용되는 Report 객체 생성 메서드를 제공한다.
 *
 * @author seunggu.lee
 */
public class ReportTestFixture {

    private static final Long DEFAULT_ID = 1L;
    private static final Long DEFAULT_REPORTER_ID = 1L;
    private static final Long DEFAULT_REPORTED_USER_ID = 2L;

    /**
     * 기본값으로 사용자 신고(USER 타입, HARASSMENT 사유) 객체를 생성한다.
     *
     * @return PENDING 상태의 사용자 신고 엔티티
     */
    public static Report createUserReport() {
        return createUserReport(DEFAULT_ID, DEFAULT_REPORTER_ID, DEFAULT_REPORTED_USER_ID);
    }

    /**
     * 지정된 ID와 사용자 정보로 사용자 신고 객체를 생성한다.
     *
     * @param id             신고 ID
     * @param reporterId     신고자 ID
     * @param reportedUserId 피신고 사용자 ID
     * @return PENDING 상태의 사용자 신고 엔티티
     */
    public static Report createUserReport(Long id, Long reporterId, Long reportedUserId) {
        return Report.builder()
                .id(id)
                .reporterId(reporterId)
                .reportedUserId(reportedUserId)
                .type(Report.ReportType.USER)
                .reason(Report.ReportReason.HARASSMENT)
                .description("괴롭힘 신고")
                .build();
    }

    /**
     * 메시지 신고 객체를 생성한다.
     *
     * @param id                신고 ID
     * @param reporterId        신고자 ID
     * @param reportedMessageId 피신고 메시지 ID
     * @return PENDING 상태의 메시지 신고 엔티티
     */
    public static Report createMessageReport(Long id, Long reporterId, Long reportedMessageId) {
        return Report.builder()
                .id(id)
                .reporterId(reporterId)
                .reportedMessageId(reportedMessageId)
                .type(Report.ReportType.MESSAGE)
                .reason(Report.ReportReason.SPAM)
                .build();
    }

    /**
     * 채팅방 신고 객체를 생성한다.
     *
     * @param id                  신고 ID
     * @param reporterId          신고자 ID
     * @param reportedChatRoomId  피신고 채팅방 ID
     * @return PENDING 상태의 채팅방 신고 엔티티
     */
    public static Report createChatRoomReport(Long id, Long reporterId, Long reportedChatRoomId) {
        return Report.builder()
                .id(id)
                .reporterId(reporterId)
                .reportedChatRoomId(reportedChatRoomId)
                .type(Report.ReportType.CHAT_ROOM)
                .reason(Report.ReportReason.INAPPROPRIATE_CONTENT)
                .build();
    }

    /**
     * 빌더 스타일로 Report 생성을 시작한다.
     *
     * @return ReportBuilder 인스턴스
     */
    public static ReportBuilder builder() {
        return new ReportBuilder();
    }

    /**
     * Report 테스트 빌더.
     */
    public static class ReportBuilder {
        private Long id = DEFAULT_ID;
        private Long reporterId = DEFAULT_REPORTER_ID;
        private Long reportedUserId = null;
        private Long reportedMessageId = null;
        private Long reportedChatRoomId = null;
        private Report.ReportType type = Report.ReportType.USER;
        private Report.ReportReason reason = Report.ReportReason.HARASSMENT;
        private String description = null;
        private Report.ReportStatus status = Report.ReportStatus.PENDING;

        /**
         * 신고 ID를 설정한다.
         *
         * @param id 신고 ID
         * @return 빌더
         */
        public ReportBuilder id(Long id) {
            this.id = id;
            return this;
        }

        /**
         * 신고자 ID를 설정한다.
         *
         * @param reporterId 신고자 ID
         * @return 빌더
         */
        public ReportBuilder reporterId(Long reporterId) {
            this.reporterId = reporterId;
            return this;
        }

        /**
         * 피신고 사용자 ID를 설정한다.
         *
         * @param reportedUserId 피신고 사용자 ID
         * @return 빌더
         */
        public ReportBuilder reportedUserId(Long reportedUserId) {
            this.reportedUserId = reportedUserId;
            return this;
        }

        /**
         * 피신고 메시지 ID를 설정한다.
         *
         * @param reportedMessageId 피신고 메시지 ID
         * @return 빌더
         */
        public ReportBuilder reportedMessageId(Long reportedMessageId) {
            this.reportedMessageId = reportedMessageId;
            return this;
        }

        /**
         * 피신고 채팅방 ID를 설정한다.
         *
         * @param reportedChatRoomId 피신고 채팅방 ID
         * @return 빌더
         */
        public ReportBuilder reportedChatRoomId(Long reportedChatRoomId) {
            this.reportedChatRoomId = reportedChatRoomId;
            return this;
        }

        /**
         * 신고 유형을 설정한다.
         *
         * @param type 신고 유형
         * @return 빌더
         */
        public ReportBuilder type(Report.ReportType type) {
            this.type = type;
            return this;
        }

        /**
         * 신고 사유를 설정한다.
         *
         * @param reason 신고 사유
         * @return 빌더
         */
        public ReportBuilder reason(Report.ReportReason reason) {
            this.reason = reason;
            return this;
        }

        /**
         * 신고 상세 설명을 설정한다.
         *
         * @param description 상세 설명
         * @return 빌더
         */
        public ReportBuilder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * 신고 상태를 설정한다.
         *
         * @param status 신고 상태
         * @return 빌더
         */
        public ReportBuilder status(Report.ReportStatus status) {
            this.status = status;
            return this;
        }

        /**
         * Report 객체를 생성한다.
         *
         * @return 생성된 Report 엔티티
         */
        public Report build() {
            return Report.builder()
                    .id(id)
                    .reporterId(reporterId)
                    .reportedUserId(reportedUserId)
                    .reportedMessageId(reportedMessageId)
                    .reportedChatRoomId(reportedChatRoomId)
                    .type(type)
                    .reason(reason)
                    .description(description)
                    .status(status)
                    .build();
        }
    }
}
