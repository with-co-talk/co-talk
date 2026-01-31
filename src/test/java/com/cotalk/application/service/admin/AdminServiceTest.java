package com.cotalk.application.service.admin;

import com.cotalk.domain.entity.Report;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.exception.ReportNotFoundException;
import com.cotalk.domain.exception.UserNotFoundException;
import com.cotalk.domain.port.inbound.admin.AdminUseCase;
import com.cotalk.domain.port.outbound.ChatRoomRepository;
import com.cotalk.domain.port.outbound.MessageRepository;
import com.cotalk.domain.port.outbound.ReportRepository;
import com.cotalk.domain.port.outbound.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private MessageRepository messageRepository;

    @InjectMocks
    private AdminService adminService;

    @Nested
    @DisplayName("신고 처리")
    class ProcessReport {

        @Test
        @DisplayName("대기 중인 신고 목록을 조회할 수 있다")
        void should_returnPendingReports() {
            // given
            List<Report> pendingReports = List.of(
                    Report.builder()
                            .id(1L)
                            .reporterId(100L)
                            .reportedUserId(200L)
                            .type(Report.ReportType.USER)
                            .reason(Report.ReportReason.SPAM)
                            .status(Report.ReportStatus.PENDING)
                            .build()
            );

            given(reportRepository.findByStatus(Report.ReportStatus.PENDING))
                    .willReturn(pendingReports);

            // when
            List<Report> result = adminService.getPendingReports();

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStatus()).isEqualTo(Report.ReportStatus.PENDING);
        }

        @Test
        @DisplayName("전체 신고 목록을 조회할 수 있다 (status가 null인 경우)")
        void should_returnAllReports_when_statusIsNull() {
            // given
            List<Report> allReports = List.of(
                    Report.builder()
                            .id(1L)
                            .reporterId(100L)
                            .reportedUserId(200L)
                            .type(Report.ReportType.USER)
                            .reason(Report.ReportReason.SPAM)
                            .status(Report.ReportStatus.PENDING)
                            .build(),
                    Report.builder()
                            .id(2L)
                            .reporterId(101L)
                            .reportedUserId(201L)
                            .type(Report.ReportType.MESSAGE)
                            .reason(Report.ReportReason.HARASSMENT)
                            .status(Report.ReportStatus.RESOLVED)
                            .build()
            );

            given(reportRepository.findAll()).willReturn(allReports);

            // when
            List<Report> result = adminService.getAllReports(null);

            // then
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("특정 상태의 신고 목록을 조회할 수 있다")
        void should_returnReportsByStatus_when_statusProvided() {
            // given
            List<Report> resolvedReports = List.of(
                    Report.builder()
                            .id(1L)
                            .reporterId(100L)
                            .reportedUserId(200L)
                            .type(Report.ReportType.USER)
                            .reason(Report.ReportReason.SPAM)
                            .status(Report.ReportStatus.RESOLVED)
                            .build()
            );

            given(reportRepository.findByStatus(Report.ReportStatus.RESOLVED))
                    .willReturn(resolvedReports);

            // when
            List<Report> result = adminService.getAllReports(Report.ReportStatus.RESOLVED);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStatus()).isEqualTo(Report.ReportStatus.RESOLVED);
        }

        @Test
        @DisplayName("신고를 처리할 수 있다")
        void should_processReport_when_validRequest() {
            // given
            Long reportId = 1L;
            Long adminId = 999L;
            String adminNote = "스팸으로 확인되어 처리함";

            Report report = Report.builder()
                    .id(reportId)
                    .reporterId(100L)
                    .reportedUserId(200L)
                    .type(Report.ReportType.USER)
                    .reason(Report.ReportReason.SPAM)
                    .status(Report.ReportStatus.PENDING)
                    .build();

            given(reportRepository.findById(reportId)).willReturn(Optional.of(report));
            given(reportRepository.save(any(Report.class))).willAnswer(inv -> inv.getArgument(0));

            // when
            Report result = adminService.processReport(reportId, adminId, Report.ReportStatus.RESOLVED, adminNote);

            // then
            assertThat(result.getStatus()).isEqualTo(Report.ReportStatus.RESOLVED);
            assertThat(result.getAdminNote()).isEqualTo(adminNote);
            assertThat(result.getProcessedBy()).isEqualTo(adminId);
        }

        @Test
        @DisplayName("존재하지 않는 신고 처리시 예외 발생")
        void should_throwException_when_reportNotFound() {
            // given
            Long reportId = 999L;

            given(reportRepository.findById(reportId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> adminService.processReport(reportId, 1L, Report.ReportStatus.RESOLVED, "note"))
                    .isInstanceOf(ReportNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("사용자 관리")
    class UserManagement {

        @Test
        @DisplayName("모든 사용자 목록을 조회할 수 있다")
        void should_returnAllUsers() {
            // given
            List<User> users = List.of(
                    User.builder()
                            .id(1L)
                            .email("user1@test.com")
                            .passwordHash("hash")
                            .nickname("user1")
                            .build(),
                    User.builder()
                            .id(2L)
                            .email("user2@test.com")
                            .passwordHash("hash")
                            .nickname("user2")
                            .build()
            );

            given(userRepository.findAll()).willReturn(users);

            // when
            List<User> result = adminService.getAllUsers();

            // then
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("상태별 사용자 목록을 조회할 수 있다")
        void should_returnUsersByStatus() {
            // given
            List<User> suspendedUsers = List.of(
                    User.builder()
                            .id(1L)
                            .email("suspended@test.com")
                            .nickname("suspended")
                            .status(User.UserStatus.SUSPENDED)
                            .build()
            );

            given(userRepository.findByStatus(User.UserStatus.SUSPENDED))
                    .willReturn(suspendedUsers);

            // when
            List<User> result = adminService.getUsersByStatus(User.UserStatus.SUSPENDED);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStatus()).isEqualTo(User.UserStatus.SUSPENDED);
        }

        @Test
        @DisplayName("사용자를 정지시킬 수 있다")
        void should_suspendUser_when_validRequest() {
            // given
            Long adminId = 999L;
            Long userId = 1L;
            String reason = "부적절한 행동";

            User user = User.builder()
                    .id(userId)
                    .email("user@test.com")
                    .nickname("user")
                    .status(User.UserStatus.ACTIVE)
                    .build();

            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(userRepository.save(any(User.class))).willAnswer(inv -> inv.getArgument(0));

            // when
            User result = adminService.suspendUser(adminId, userId, reason);

            // then
            assertThat(result.getStatus()).isEqualTo(User.UserStatus.SUSPENDED);
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("사용자를 활성화할 수 있다")
        void should_activateUser_when_validRequest() {
            // given
            Long adminId = 999L;
            Long userId = 1L;

            User user = User.builder()
                    .id(userId)
                    .email("user@test.com")
                    .nickname("user")
                    .status(User.UserStatus.SUSPENDED)
                    .build();

            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(userRepository.save(any(User.class))).willAnswer(inv -> inv.getArgument(0));

            // when
            User result = adminService.activateUser(adminId, userId);

            // then
            assertThat(result.getStatus()).isEqualTo(User.UserStatus.ACTIVE);
        }

        @Test
        @DisplayName("존재하지 않는 사용자 정지시 예외 발생")
        void should_throwException_when_userNotFound() {
            // given
            Long userId = 999L;

            given(userRepository.findById(userId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> adminService.suspendUser(1L, userId, "reason"))
                    .isInstanceOf(UserNotFoundException.class);
        }

        @Test
        @DisplayName("존재하지 않는 사용자 활성화시 예외 발생")
        void should_throwException_when_activateUserNotFound() {
            // given
            Long userId = 999L;

            given(userRepository.findById(userId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> adminService.activateUser(1L, userId))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("통계 조회")
    class Statistics {

        @Test
        @DisplayName("관리자 통계를 조회할 수 있다")
        void should_returnStatistics() {
            // given
            given(userRepository.count()).willReturn(100L);
            given(userRepository.countByStatus(User.UserStatus.ACTIVE)).willReturn(90L);
            given(userRepository.countByStatus(User.UserStatus.SUSPENDED)).willReturn(5L);
            given(reportRepository.count()).willReturn(50L);
            given(reportRepository.countByStatus(Report.ReportStatus.PENDING)).willReturn(10L);
            given(chatRoomRepository.count()).willReturn(200L);
            given(messageRepository.count()).willReturn(5000L);

            // when
            AdminUseCase.AdminStatistics stats = adminService.getStatistics();

            // then
            assertThat(stats.totalUsers()).isEqualTo(100L);
            assertThat(stats.activeUsers()).isEqualTo(90L);
            assertThat(stats.suspendedUsers()).isEqualTo(5L);
            assertThat(stats.totalReports()).isEqualTo(50L);
            assertThat(stats.pendingReports()).isEqualTo(10L);
            assertThat(stats.totalChatRooms()).isEqualTo(200L);
            assertThat(stats.totalMessages()).isEqualTo(5000L);
        }
    }
}
