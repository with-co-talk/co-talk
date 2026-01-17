package com.cotalk.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Report 엔티티")
class ReportTest {

    @Nested
    @DisplayName("생성 시")
    class Creation {

        @Test
        @DisplayName("사용자 신고를 생성할 수 있다")
        void should_createUserReport_when_validInputsProvided() {
            // given
            Long reporterId = 1L;
            Long reportedUserId = 2L;

            // when
            Report report = Report.builder()
                    .id(1L)
                    .reporterId(reporterId)
                    .reportedUserId(reportedUserId)
                    .type(Report.ReportType.USER)
                    .reason(Report.ReportReason.HARASSMENT)
                    .description("괴롭힘 신고")
                    .build();

            // then
            assertThat(report.getReporterId()).isEqualTo(reporterId);
            assertThat(report.getReportedUserId()).isEqualTo(reportedUserId);
            assertThat(report.getType()).isEqualTo(Report.ReportType.USER);
            assertThat(report.getReason()).isEqualTo(Report.ReportReason.HARASSMENT);
            assertThat(report.getStatus()).isEqualTo(Report.ReportStatus.PENDING);
        }

        @Test
        @DisplayName("메시지 신고를 생성할 수 있다")
        void should_createMessageReport_when_validInputsProvided() {
            // given
            Long reporterId = 1L;
            Long reportedMessageId = 100L;

            // when
            Report report = Report.builder()
                    .id(2L)
                    .reporterId(reporterId)
                    .reportedMessageId(reportedMessageId)
                    .type(Report.ReportType.MESSAGE)
                    .reason(Report.ReportReason.SPAM)
                    .build();

            // then
            assertThat(report.getReportedMessageId()).isEqualTo(reportedMessageId);
            assertThat(report.getType()).isEqualTo(Report.ReportType.MESSAGE);
            assertThat(report.getReason()).isEqualTo(Report.ReportReason.SPAM);
        }

        @Test
        @DisplayName("채팅방 신고를 생성할 수 있다")
        void should_createChatRoomReport_when_validInputsProvided() {
            // given
            Long reporterId = 1L;
            Long reportedChatRoomId = 50L;

            // when
            Report report = Report.builder()
                    .id(3L)
                    .reporterId(reporterId)
                    .reportedChatRoomId(reportedChatRoomId)
                    .type(Report.ReportType.CHAT_ROOM)
                    .reason(Report.ReportReason.INAPPROPRIATE_CONTENT)
                    .build();

            // then
            assertThat(report.getReportedChatRoomId()).isEqualTo(reportedChatRoomId);
            assertThat(report.getType()).isEqualTo(Report.ReportType.CHAT_ROOM);
        }

        @Test
        @DisplayName("기본 상태는 PENDING이다")
        void should_haveDefaultStatusPending_when_created() {
            // when
            Report report = Report.builder()
                    .id(1L)
                    .reporterId(1L)
                    .reportedUserId(2L)
                    .type(Report.ReportType.USER)
                    .reason(Report.ReportReason.SPAM)
                    .build();

            // then
            assertThat(report.getStatus()).isEqualTo(Report.ReportStatus.PENDING);
        }
    }

    @Nested
    @DisplayName("process 메서드")
    class Process {

        @Test
        @DisplayName("신고를 처리 완료 상태로 변경할 수 있다")
        void should_changeStatusToResolved_when_processed() {
            // given
            Report report = Report.builder()
                    .id(1L)
                    .reporterId(1L)
                    .reportedUserId(2L)
                    .type(Report.ReportType.USER)
                    .reason(Report.ReportReason.HARASSMENT)
                    .build();

            Long adminId = 100L;
            String adminNote = "확인 결과 신고 내용이 사실로 확인됨";

            // when
            report.process(Report.ReportStatus.RESOLVED, adminNote, adminId);

            // then
            assertThat(report.getStatus()).isEqualTo(Report.ReportStatus.RESOLVED);
            assertThat(report.getAdminNote()).isEqualTo(adminNote);
            assertThat(report.getProcessedBy()).isEqualTo(adminId);
            assertThat(report.getProcessedAt()).isNotNull();
        }

        @Test
        @DisplayName("신고를 기각 상태로 변경할 수 있다")
        void should_changeStatusToDismissed_when_dismissed() {
            // given
            Report report = Report.builder()
                    .id(1L)
                    .reporterId(1L)
                    .reportedUserId(2L)
                    .type(Report.ReportType.USER)
                    .reason(Report.ReportReason.FAKE_PROFILE)
                    .build();

            Long adminId = 100L;
            String adminNote = "허위 신고로 판단됨";

            // when
            report.process(Report.ReportStatus.DISMISSED, adminNote, adminId);

            // then
            assertThat(report.getStatus()).isEqualTo(Report.ReportStatus.DISMISSED);
            assertThat(report.getAdminNote()).isEqualTo(adminNote);
        }

        @Test
        @DisplayName("신고를 검토 중 상태로 변경할 수 있다")
        void should_changeStatusToReviewing_when_underReview() {
            // given
            Report report = Report.builder()
                    .id(1L)
                    .reporterId(1L)
                    .reportedUserId(2L)
                    .type(Report.ReportType.USER)
                    .reason(Report.ReportReason.SCAM)
                    .build();

            Long adminId = 100L;
            String adminNote = "추가 조사 필요";

            // when
            report.process(Report.ReportStatus.REVIEWING, adminNote, adminId);

            // then
            assertThat(report.getStatus()).isEqualTo(Report.ReportStatus.REVIEWING);
        }

        @Test
        @DisplayName("처리 시간이 현재 시간으로 설정된다")
        void should_setProcessedAtToCurrentTime_when_processed() {
            // given
            Report report = Report.builder()
                    .id(1L)
                    .reporterId(1L)
                    .reportedUserId(2L)
                    .type(Report.ReportType.USER)
                    .reason(Report.ReportReason.HARASSMENT)
                    .build();

            LocalDateTime beforeProcess = LocalDateTime.now();

            // when
            report.process(Report.ReportStatus.RESOLVED, "처리 완료", 100L);

            // then
            assertThat(report.getProcessedAt())
                    .isAfterOrEqualTo(beforeProcess)
                    .isBeforeOrEqualTo(LocalDateTime.now());
        }
    }

    @Nested
    @DisplayName("isPending 메서드")
    class IsPending {

        @Test
        @DisplayName("상태가 PENDING이면 true를 반환한다")
        void should_returnTrue_when_statusIsPending() {
            // given
            Report report = Report.builder()
                    .id(1L)
                    .reporterId(1L)
                    .reportedUserId(2L)
                    .type(Report.ReportType.USER)
                    .reason(Report.ReportReason.SPAM)
                    .build();

            // when
            boolean result = report.isPending();

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("상태가 PENDING이 아니면 false를 반환한다")
        void should_returnFalse_when_statusIsNotPending() {
            // given
            Report report = Report.builder()
                    .id(1L)
                    .reporterId(1L)
                    .reportedUserId(2L)
                    .type(Report.ReportType.USER)
                    .reason(Report.ReportReason.SPAM)
                    .build();

            report.process(Report.ReportStatus.RESOLVED, "처리 완료", 100L);

            // when
            boolean result = report.isPending();

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("isResolved 메서드")
    class IsResolved {

        @Test
        @DisplayName("상태가 RESOLVED이면 true를 반환한다")
        void should_returnTrue_when_statusIsResolved() {
            // given
            Report report = Report.builder()
                    .id(1L)
                    .reporterId(1L)
                    .reportedUserId(2L)
                    .type(Report.ReportType.USER)
                    .reason(Report.ReportReason.HARASSMENT)
                    .build();

            report.process(Report.ReportStatus.RESOLVED, "처리 완료", 100L);

            // when
            boolean result = report.isResolved();

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("상태가 RESOLVED가 아니면 false를 반환한다")
        void should_returnFalse_when_statusIsNotResolved() {
            // given
            Report report = Report.builder()
                    .id(1L)
                    .reporterId(1L)
                    .reportedUserId(2L)
                    .type(Report.ReportType.USER)
                    .reason(Report.ReportReason.SPAM)
                    .build();

            // when
            boolean result = report.isResolved();

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("상태가 DISMISSED이면 false를 반환한다")
        void should_returnFalse_when_statusIsDismissed() {
            // given
            Report report = Report.builder()
                    .id(1L)
                    .reporterId(1L)
                    .reportedUserId(2L)
                    .type(Report.ReportType.USER)
                    .reason(Report.ReportReason.SPAM)
                    .build();

            report.process(Report.ReportStatus.DISMISSED, "기각됨", 100L);

            // when
            boolean result = report.isResolved();

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("ReportReason 열거형")
    class ReportReasonEnum {

        @Test
        @DisplayName("모든 신고 사유가 정의되어 있다")
        void should_haveAllReasons() {
            // then
            assertThat(Report.ReportReason.values()).containsExactlyInAnyOrder(
                    Report.ReportReason.SPAM,
                    Report.ReportReason.HARASSMENT,
                    Report.ReportReason.INAPPROPRIATE_CONTENT,
                    Report.ReportReason.FAKE_PROFILE,
                    Report.ReportReason.SCAM,
                    Report.ReportReason.HATE_SPEECH,
                    Report.ReportReason.VIOLENCE,
                    Report.ReportReason.OTHER
            );
        }
    }

    @Nested
    @DisplayName("ReportType 열거형")
    class ReportTypeEnum {

        @Test
        @DisplayName("모든 신고 타입이 정의되어 있다")
        void should_haveAllTypes() {
            // then
            assertThat(Report.ReportType.values()).containsExactlyInAnyOrder(
                    Report.ReportType.USER,
                    Report.ReportType.MESSAGE,
                    Report.ReportType.CHAT_ROOM
            );
        }
    }

    @Nested
    @DisplayName("ReportStatus 열거형")
    class ReportStatusEnum {

        @Test
        @DisplayName("모든 신고 상태가 정의되어 있다")
        void should_haveAllStatuses() {
            // then
            assertThat(Report.ReportStatus.values()).containsExactlyInAnyOrder(
                    Report.ReportStatus.PENDING,
                    Report.ReportStatus.REVIEWING,
                    Report.ReportStatus.RESOLVED,
                    Report.ReportStatus.DISMISSED
            );
        }
    }
}
