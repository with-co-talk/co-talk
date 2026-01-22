package com.cotalk.adapter.outbound.persistence;

import com.cotalk.adapter.outbound.persistence.report.ReportRepositoryAdapter;
import com.cotalk.adapter.outbound.persistence.user.UserRepositoryAdapter;
import com.cotalk.domain.entity.Report;
import com.cotalk.domain.entity.Report.ReportReason;
import com.cotalk.domain.entity.Report.ReportStatus;
import com.cotalk.domain.entity.Report.ReportType;
import com.cotalk.domain.entity.User;
import com.cotalk.infrastructure.config.JpaAuditingConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ReportRepositoryAdapter 테스트.
 *
 * @author seunggu.lee
 */
@DataJpaTest
@ActiveProfiles("test")
@Import({ReportRepositoryAdapter.class, UserRepositoryAdapter.class, JpaAuditingConfig.class})
@DisplayName("ReportRepositoryAdapter")
class ReportRepositoryAdapterTest {

    @Autowired
    private ReportRepositoryAdapter reportRepository;

    @Autowired
    private UserRepositoryAdapter userRepository;

    private User user1;
    private User user2;
    private User user3;

    @BeforeEach
    void setUp() {
        user1 = userRepository.save(User.builder()
                .id(1L)
                .email("user1@example.com")
                .passwordHash("hash")
                .nickname("user1")
                .build());

        user2 = userRepository.save(User.builder()
                .id(2L)
                .email("user2@example.com")
                .passwordHash("hash")
                .nickname("user2")
                .build());

        user3 = userRepository.save(User.builder()
                .id(3L)
                .email("user3@example.com")
                .passwordHash("hash")
                .nickname("user3")
                .build());
    }

    @Nested
    @DisplayName("저장 시")
    class Save {

        @Test
        @DisplayName("신고를 저장한다")
        void should_saveReport_when_reportProvided() {
            // given
            Report report = Report.builder()
                    .id(100L)
                    .reporterId(user1.getId())
                    .reportedUserId(user2.getId())
                    .type(ReportType.USER)
                    .reason(ReportReason.SPAM)
                    .status(ReportStatus.PENDING)
                    .build();

            // when
            Report saved = reportRepository.save(report);

            // then
            assertThat(saved.getId()).isEqualTo(100L);
            assertThat(saved.getReporterId()).isEqualTo(user1.getId());
            assertThat(saved.getReportedUserId()).isEqualTo(user2.getId());
            assertThat(saved.getReason()).isEqualTo(ReportReason.SPAM);
        }

        @Test
        @DisplayName("메시지 신고를 저장한다")
        void should_saveMessageReport_when_messageIdProvided() {
            // given
            Report report = Report.builder()
                    .id(100L)
                    .reporterId(user1.getId())
                    .reportedUserId(user2.getId())
                    .reportedMessageId(50000L)
                    .type(ReportType.MESSAGE)
                    .reason(ReportReason.INAPPROPRIATE_CONTENT)
                    .status(ReportStatus.PENDING)
                    .build();

            // when
            Report saved = reportRepository.save(report);

            // then
            assertThat(saved.getReportedMessageId()).isEqualTo(50000L);
            assertThat(saved.getType()).isEqualTo(ReportType.MESSAGE);
        }
    }

    @Nested
    @DisplayName("조회 시")
    class Find {

        @Test
        @DisplayName("ID로 신고를 조회한다")
        void should_findReport_when_idProvided() {
            // given
            reportRepository.save(Report.builder()
                    .id(100L)
                    .reporterId(user1.getId())
                    .reportedUserId(user2.getId())
                    .type(ReportType.USER)
                    .reason(ReportReason.HARASSMENT)
                    .status(ReportStatus.PENDING)
                    .build());

            // when
            Optional<Report> found = reportRepository.findById(100L);

            // then
            assertThat(found).isPresent();
            assertThat(found.get().getReason()).isEqualTo(ReportReason.HARASSMENT);
        }

        @Test
        @DisplayName("신고자 ID로 신고 목록을 조회한다")
        void should_findReports_when_reporterIdProvided() {
            // given
            reportRepository.save(Report.builder()
                    .id(100L)
                    .reporterId(user1.getId())
                    .reportedUserId(user2.getId())
                    .type(ReportType.USER)
                    .reason(ReportReason.SPAM)
                    .status(ReportStatus.PENDING)
                    .build());
            reportRepository.save(Report.builder()
                    .id(101L)
                    .reporterId(user1.getId())
                    .reportedUserId(user3.getId())
                    .type(ReportType.USER)
                    .reason(ReportReason.HARASSMENT)
                    .status(ReportStatus.PENDING)
                    .build());

            // when
            List<Report> reports = reportRepository.findByReporterId(user1.getId());

            // then
            assertThat(reports).hasSize(2);
        }

        @Test
        @DisplayName("피신고자 ID로 신고 목록을 조회한다")
        void should_findReports_when_reportedUserIdProvided() {
            // given
            reportRepository.save(Report.builder()
                    .id(100L)
                    .reporterId(user1.getId())
                    .reportedUserId(user2.getId())
                    .type(ReportType.USER)
                    .reason(ReportReason.SPAM)
                    .status(ReportStatus.PENDING)
                    .build());
            reportRepository.save(Report.builder()
                    .id(101L)
                    .reporterId(user3.getId())
                    .reportedUserId(user2.getId())
                    .type(ReportType.USER)
                    .reason(ReportReason.HARASSMENT)
                    .status(ReportStatus.PENDING)
                    .build());

            // when
            List<Report> reports = reportRepository.findByReportedUserId(user2.getId());

            // then
            assertThat(reports).hasSize(2);
        }

        @Test
        @DisplayName("상태별로 신고 목록을 조회한다")
        void should_findReports_when_statusProvided() {
            // given
            reportRepository.save(Report.builder()
                    .id(100L)
                    .reporterId(user1.getId())
                    .reportedUserId(user2.getId())
                    .type(ReportType.USER)
                    .reason(ReportReason.SPAM)
                    .status(ReportStatus.PENDING)
                    .build());
            reportRepository.save(Report.builder()
                    .id(101L)
                    .reporterId(user1.getId())
                    .reportedUserId(user3.getId())
                    .type(ReportType.USER)
                    .reason(ReportReason.SCAM)
                    .status(ReportStatus.RESOLVED)
                    .build());

            // when
            List<Report> pending = reportRepository.findByStatus(ReportStatus.PENDING);
            List<Report> resolved = reportRepository.findByStatus(ReportStatus.RESOLVED);

            // then
            assertThat(pending).hasSize(1);
            assertThat(resolved).hasSize(1);
        }

        @Test
        @DisplayName("모든 신고를 조회한다")
        void should_findAllReports_when_called() {
            // given
            reportRepository.save(Report.builder()
                    .id(100L)
                    .reporterId(user1.getId())
                    .reportedUserId(user2.getId())
                    .type(ReportType.USER)
                    .reason(ReportReason.SPAM)
                    .status(ReportStatus.PENDING)
                    .build());
            reportRepository.save(Report.builder()
                    .id(101L)
                    .reporterId(user2.getId())
                    .reportedUserId(user3.getId())
                    .type(ReportType.USER)
                    .reason(ReportReason.HARASSMENT)
                    .status(ReportStatus.PENDING)
                    .build());

            // when
            List<Report> all = reportRepository.findAll();

            // then
            assertThat(all).hasSize(2);
        }
    }

    @Nested
    @DisplayName("존재 여부 확인 시")
    class Exists {

        @Test
        @DisplayName("신고자와 피신고자로 신고가 존재하면 true를 반환한다")
        void should_returnTrue_when_userReportExists() {
            // given
            reportRepository.save(Report.builder()
                    .id(100L)
                    .reporterId(user1.getId())
                    .reportedUserId(user2.getId())
                    .type(ReportType.USER)
                    .reason(ReportReason.SPAM)
                    .status(ReportStatus.PENDING)
                    .build());

            // when & then
            assertThat(reportRepository.existsByReporterIdAndReportedUserId(
                    user1.getId(), user2.getId())).isTrue();
        }

        @Test
        @DisplayName("신고자와 메시지 ID로 신고가 존재하면 true를 반환한다")
        void should_returnTrue_when_messageReportExists() {
            // given
            reportRepository.save(Report.builder()
                    .id(100L)
                    .reporterId(user1.getId())
                    .reportedUserId(user2.getId())
                    .reportedMessageId(50000L)
                    .type(ReportType.MESSAGE)
                    .reason(ReportReason.INAPPROPRIATE_CONTENT)
                    .status(ReportStatus.PENDING)
                    .build());

            // when & then
            assertThat(reportRepository.existsByReporterIdAndReportedMessageId(
                    user1.getId(), 50000L)).isTrue();
        }

        @Test
        @DisplayName("신고가 존재하지 않으면 false를 반환한다")
        void should_returnFalse_when_reportNotExists() {
            // when & then
            assertThat(reportRepository.existsByReporterIdAndReportedUserId(
                    user1.getId(), user2.getId())).isFalse();
        }
    }

    @Nested
    @DisplayName("카운트 시")
    class Count {

        @Test
        @DisplayName("전체 신고 수를 조회한다")
        void should_returnCount_when_called() {
            // given
            reportRepository.save(Report.builder()
                    .id(100L)
                    .reporterId(user1.getId())
                    .reportedUserId(user2.getId())
                    .type(ReportType.USER)
                    .reason(ReportReason.SPAM)
                    .status(ReportStatus.PENDING)
                    .build());
            reportRepository.save(Report.builder()
                    .id(101L)
                    .reporterId(user2.getId())
                    .reportedUserId(user3.getId())
                    .type(ReportType.USER)
                    .reason(ReportReason.HARASSMENT)
                    .status(ReportStatus.RESOLVED)
                    .build());

            // when
            long count = reportRepository.count();

            // then
            assertThat(count).isEqualTo(2);
        }

        @Test
        @DisplayName("상태별 신고 수를 조회한다")
        void should_returnCountByStatus_when_statusProvided() {
            // given
            reportRepository.save(Report.builder()
                    .id(100L)
                    .reporterId(user1.getId())
                    .reportedUserId(user2.getId())
                    .type(ReportType.USER)
                    .reason(ReportReason.SPAM)
                    .status(ReportStatus.PENDING)
                    .build());
            reportRepository.save(Report.builder()
                    .id(101L)
                    .reporterId(user2.getId())
                    .reportedUserId(user3.getId())
                    .type(ReportType.USER)
                    .reason(ReportReason.HARASSMENT)
                    .status(ReportStatus.PENDING)
                    .build());
            reportRepository.save(Report.builder()
                    .id(102L)
                    .reporterId(user3.getId())
                    .reportedUserId(user1.getId())
                    .type(ReportType.USER)
                    .reason(ReportReason.SCAM)
                    .status(ReportStatus.RESOLVED)
                    .build());

            // when
            long pendingCount = reportRepository.countByStatus(ReportStatus.PENDING);
            long resolvedCount = reportRepository.countByStatus(ReportStatus.RESOLVED);

            // then
            assertThat(pendingCount).isEqualTo(2);
            assertThat(resolvedCount).isEqualTo(1);
        }
    }
}
