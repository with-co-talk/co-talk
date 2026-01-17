package com.cotalk.application.service.report;

import com.cotalk.domain.entity.Report;
import com.cotalk.domain.exception.InvalidReportException;
import com.cotalk.domain.port.outbound.ReportRepository;
import com.cotalk.infrastructure.id.SnowflakeIdGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private SnowflakeIdGenerator idGenerator;

    @InjectMocks
    private ReportService reportService;

    @Test
    @DisplayName("사용자 신고 성공")
    void should_createUserReport_when_validInput() {
        // given
        Long reporterId = 100L;
        Long reportedUserId = 200L;
        Report.ReportReason reason = Report.ReportReason.HARASSMENT;
        String description = "욕설을 사용했습니다.";

        given(idGenerator.nextId()).willReturn(1L);
        given(reportRepository.existsByReporterIdAndReportedUserId(reporterId, reportedUserId))
                .willReturn(false);
        given(reportRepository.save(any(Report.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        Report result = reportService.reportUser(reporterId, reportedUserId, reason, description);

        // then
        assertThat(result.getReporterId()).isEqualTo(reporterId);
        assertThat(result.getReportedUserId()).isEqualTo(reportedUserId);
        assertThat(result.getType()).isEqualTo(Report.ReportType.USER);
        assertThat(result.getReason()).isEqualTo(reason);
        assertThat(result.getDescription()).isEqualTo(description);
        assertThat(result.getStatus()).isEqualTo(Report.ReportStatus.PENDING);
    }

    @Test
    @DisplayName("자기 자신을 신고하면 실패")
    void should_throwException_when_reportSelf() {
        // given
        Long userId = 100L;

        // when & then
        assertThatThrownBy(() -> reportService.reportUser(userId, userId, Report.ReportReason.SPAM, "테스트"))
                .isInstanceOf(InvalidReportException.class)
                .hasMessage("자기 자신을 신고할 수 없습니다.");
    }

    @Test
    @DisplayName("이미 신고한 사용자를 다시 신고하면 실패")
    void should_throwException_when_alreadyReportedUser() {
        // given
        Long reporterId = 100L;
        Long reportedUserId = 200L;

        given(reportRepository.existsByReporterIdAndReportedUserId(reporterId, reportedUserId))
                .willReturn(true);

        // when & then
        assertThatThrownBy(() -> reportService.reportUser(reporterId, reportedUserId, Report.ReportReason.SPAM, "테스트"))
                .isInstanceOf(InvalidReportException.class)
                .hasMessage("이미 신고한 사용자입니다.");
    }

    @Test
    @DisplayName("메시지 신고 성공")
    void should_createMessageReport_when_validInput() {
        // given
        Long reporterId = 100L;
        Long messageId = 500L;
        Report.ReportReason reason = Report.ReportReason.INAPPROPRIATE_CONTENT;
        String description = "부적절한 내용입니다.";

        given(idGenerator.nextId()).willReturn(1L);
        given(reportRepository.existsByReporterIdAndReportedMessageId(reporterId, messageId))
                .willReturn(false);
        given(reportRepository.save(any(Report.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        Report result = reportService.reportMessage(reporterId, messageId, reason, description);

        // then
        assertThat(result.getReporterId()).isEqualTo(reporterId);
        assertThat(result.getReportedMessageId()).isEqualTo(messageId);
        assertThat(result.getType()).isEqualTo(Report.ReportType.MESSAGE);
        assertThat(result.getReason()).isEqualTo(reason);
    }

    @Test
    @DisplayName("내 신고 내역 조회")
    void should_returnMyReports_when_validUserId() {
        // given
        Long userId = 100L;

        Report report1 = Report.builder()
                .id(1L)
                .reporterId(userId)
                .reportedUserId(200L)
                .type(Report.ReportType.USER)
                .reason(Report.ReportReason.SPAM)
                .build();

        Report report2 = Report.builder()
                .id(2L)
                .reporterId(userId)
                .reportedMessageId(500L)
                .type(Report.ReportType.MESSAGE)
                .reason(Report.ReportReason.HARASSMENT)
                .build();

        given(reportRepository.findByReporterId(userId))
                .willReturn(List.of(report1, report2));

        // when
        List<Report> result = reportService.getMyReports(userId);

        // then
        assertThat(result).hasSize(2);
    }
}
