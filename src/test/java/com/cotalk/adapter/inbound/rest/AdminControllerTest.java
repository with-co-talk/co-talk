package com.cotalk.adapter.inbound.rest;

import com.cotalk.adapter.inbound.rest.dto.admin.ProcessReportRequest;
import com.cotalk.adapter.inbound.rest.dto.admin.SuspendUserRequest;
import com.cotalk.domain.entity.Report;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.port.inbound.admin.AdminUseCase;
import com.cotalk.infrastructure.ratelimit.RateLimitTestConfiguration;
import com.cotalk.infrastructure.security.JwtAuthenticationFilter;
import com.cotalk.infrastructure.security.JwtTokenProvider;
import com.cotalk.infrastructure.security.WithMockCustomUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(RateLimitTestConfiguration.class)
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AdminUseCase adminUseCase;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Nested
    @DisplayName("신고 관리 API")
    class ReportManagementApi {

        @Test
        @DisplayName("대기 중인 신고 목록 조회 - 페이지네이션 메타데이터 포함")
        void should_returnPendingReports() throws Exception {
            // given
            List<Report> reports = List.of(
                    Report.builder()
                            .id(1L)
                            .reporterId(100L)
                            .reportedUserId(200L)
                            .type(Report.ReportType.USER)
                            .reason(Report.ReportReason.SPAM)
                            .status(Report.ReportStatus.PENDING)
                            .build()
            );

            Page<Report> reportPage = new PageImpl<>(reports, PageRequest.of(0, 20), 1);
            given(adminUseCase.getPendingReports(any(Pageable.class))).willReturn(reportPage);

            // when & then
            mockMvc.perform(get("/api/v1/admin/reports/pending")
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.reports").isArray())
                    .andExpect(jsonPath("$.reports.length()").value(1))
                    .andExpect(jsonPath("$.reports[0].status").value("PENDING"))
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.size").value(20))
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.totalPages").value(1));
        }

        @Test
        @DisplayName("신고 처리")
        @WithMockCustomUser(userId = 999L)
        void should_processReport() throws Exception {
            // given
            Long reportId = 1L;
            ProcessReportRequest request =
                    new ProcessReportRequest(Report.ReportStatus.RESOLVED, "처리 완료");

            Report processedReport = Report.builder()
                    .id(reportId)
                    .reporterId(100L)
                    .reportedUserId(200L)
                    .type(Report.ReportType.USER)
                    .reason(Report.ReportReason.SPAM)
                    .status(Report.ReportStatus.RESOLVED)
                    .adminNote("처리 완료")
                    .processedBy(999L)
                    .processedAt(LocalDateTime.now())
                    .build();

            given(adminUseCase.processReport(eq(reportId), eq(999L), eq(Report.ReportStatus.RESOLVED), eq("처리 완료")))
                    .willReturn(processedReport);

            // when & then
            mockMvc.perform(post("/api/v1/admin/reports/{reportId}/process", reportId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("RESOLVED"))
                    .andExpect(jsonPath("$.adminNote").value("처리 완료"));
        }
    }

    @Nested
    @DisplayName("사용자 관리 API")
    class UserManagementApi {

        @Test
        @DisplayName("전체 사용자 목록 조회 - 페이지네이션 메타데이터 포함")
        void should_returnAllUsers() throws Exception {
            // given
            List<User> users = List.of(
                    User.builder()
                            .id(1L)
                            .email("user1@test.com")
                            .nickname("user1")
                            .status(User.UserStatus.ACTIVE)
                            .build(),
                    User.builder()
                            .id(2L)
                            .email("user2@test.com")
                            .nickname("user2")
                            .status(User.UserStatus.SUSPENDED)
                            .build()
            );

            Page<User> userPage = new PageImpl<>(users, PageRequest.of(0, 20), 2);
            given(adminUseCase.getAllUsers(any(Pageable.class))).willReturn(userPage);

            // when & then
            mockMvc.perform(get("/api/v1/admin/users")
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.users").isArray())
                    .andExpect(jsonPath("$.users.length()").value(2))
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.size").value(20))
                    .andExpect(jsonPath("$.totalElements").value(2))
                    .andExpect(jsonPath("$.totalPages").value(1));
        }

        @Test
        @DisplayName("사용자 정지")
        @WithMockCustomUser(userId = 999L)
        void should_suspendUser() throws Exception {
            // given
            Long userId = 1L;
            SuspendUserRequest request =
                    new SuspendUserRequest("부적절한 행동");

            User suspendedUser = User.builder()
                    .id(userId)
                    .email("user@test.com")
                    .nickname("user")
                    .status(User.UserStatus.SUSPENDED)
                    .build();

            given(adminUseCase.suspendUser(eq(999L), eq(userId), eq("부적절한 행동")))
                    .willReturn(suspendedUser);

            // when & then
            mockMvc.perform(post("/api/v1/admin/users/{userId}/suspend", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUSPENDED"));
        }

        @Test
        @DisplayName("사용자 활성화")
        @WithMockCustomUser(userId = 999L)
        void should_activateUser() throws Exception {
            // given
            Long userId = 1L;
            Long adminId = 999L;

            User activatedUser = User.builder()
                    .id(userId)
                    .email("user@test.com")
                    .nickname("user")
                    .status(User.UserStatus.ACTIVE)
                    .build();

            given(adminUseCase.activateUser(eq(999L), eq(userId))).willReturn(activatedUser);

            // when & then
            mockMvc.perform(post("/api/v1/admin/users/{userId}/activate", userId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ACTIVE"));
        }
    }

    @Nested
    @DisplayName("통계 API")
    class StatisticsApi {

        @Test
        @DisplayName("관리자 통계 조회")
        void should_returnStatistics() throws Exception {
            // given
            AdminUseCase.AdminStatistics stats = new AdminUseCase.AdminStatistics(
                    100L, 90L, 5L, 50L, 10L, 200L, 5000L
            );

            given(adminUseCase.getStatistics()).willReturn(stats);

            // when & then
            mockMvc.perform(get("/api/v1/admin/statistics"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalUsers").value(100))
                    .andExpect(jsonPath("$.activeUsers").value(90))
                    .andExpect(jsonPath("$.suspendedUsers").value(5))
                    .andExpect(jsonPath("$.totalReports").value(50))
                    .andExpect(jsonPath("$.pendingReports").value(10))
                    .andExpect(jsonPath("$.totalChatRooms").value(200))
                    .andExpect(jsonPath("$.totalMessages").value(5000));
        }
    }
}
