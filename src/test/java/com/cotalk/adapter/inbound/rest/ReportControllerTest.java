package com.cotalk.adapter.inbound.rest;

import com.cotalk.domain.entity.Report;
import com.cotalk.domain.exception.InvalidReportException;
import com.cotalk.domain.port.inbound.report.CreateReportUseCase;
import com.cotalk.domain.port.inbound.report.GetReportsUseCase;
import com.cotalk.infrastructure.exception.GlobalExceptionHandler;
import com.cotalk.infrastructure.ratelimit.RateLimitTestConfiguration;
import com.cotalk.infrastructure.security.JwtAuthenticationFilter;
import com.cotalk.infrastructure.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReportController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({RateLimitTestConfiguration.class, GlobalExceptionHandler.class})
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CreateReportUseCase createReportUseCase;

    @MockBean
    private GetReportsUseCase getReportsUseCase;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @DisplayName("사용자 신고 성공")
    void should_returnCreated_when_reportUserSuccess() throws Exception {
        // given
        Long reporterId = 100L;
        Long reportedUserId = 200L;

        Report report = Report.builder()
                .id(1L)
                .reporterId(reporterId)
                .reportedUserId(reportedUserId)
                .type(Report.ReportType.USER)
                .reason(Report.ReportReason.HARASSMENT)
                .description("욕설을 사용했습니다.")
                .status(Report.ReportStatus.PENDING)
                .build();

        given(createReportUseCase.reportUser(
                eq(reporterId),
                eq(reportedUserId),
                eq(Report.ReportReason.HARASSMENT),
                eq("욕설을 사용했습니다.")))
                .willReturn(report);

        String requestBody = """
                {
                    "reportedUserId": 200,
                    "reason": "HARASSMENT",
                    "description": "욕설을 사용했습니다."
                }
                """;

        // when & then
        mockMvc.perform(post("/api/v1/reports/users")
                        .param("reporterId", reporterId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.type").value("USER"))
                .andExpect(jsonPath("$.reason").value("HARASSMENT"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("메시지 신고 성공")
    void should_returnCreated_when_reportMessageSuccess() throws Exception {
        // given
        Long reporterId = 100L;
        Long reportedMessageId = 500L;

        Report report = Report.builder()
                .id(2L)
                .reporterId(reporterId)
                .reportedMessageId(reportedMessageId)
                .type(Report.ReportType.MESSAGE)
                .reason(Report.ReportReason.SPAM)
                .description("스팸 메시지입니다.")
                .status(Report.ReportStatus.PENDING)
                .build();

        given(createReportUseCase.reportMessage(
                eq(reporterId),
                eq(reportedMessageId),
                eq(Report.ReportReason.SPAM),
                eq("스팸 메시지입니다.")))
                .willReturn(report);

        String requestBody = """
                {
                    "reportedMessageId": 500,
                    "reason": "SPAM",
                    "description": "스팸 메시지입니다."
                }
                """;

        // when & then
        mockMvc.perform(post("/api/v1/reports/messages")
                        .param("reporterId", reporterId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.type").value("MESSAGE"))
                .andExpect(jsonPath("$.reason").value("SPAM"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("내 신고 목록 조회 성공")
    void should_returnReportList_when_getMyReports() throws Exception {
        // given
        Long userId = 100L;

        Report report1 = Report.builder()
                .id(1L)
                .reporterId(userId)
                .reportedUserId(200L)
                .type(Report.ReportType.USER)
                .reason(Report.ReportReason.HARASSMENT)
                .status(Report.ReportStatus.PENDING)
                .build();

        Report report2 = Report.builder()
                .id(2L)
                .reporterId(userId)
                .reportedMessageId(500L)
                .type(Report.ReportType.MESSAGE)
                .reason(Report.ReportReason.SPAM)
                .status(Report.ReportStatus.RESOLVED)
                .build();

        given(getReportsUseCase.getMyReports(userId))
                .willReturn(List.of(report1, report2));

        // when & then
        mockMvc.perform(get("/api/v1/reports/my")
                        .param("userId", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reports").isArray())
                .andExpect(jsonPath("$.reports.length()").value(2))
                .andExpect(jsonPath("$.reports[0].id").value(1))
                .andExpect(jsonPath("$.reports[0].type").value("USER"))
                .andExpect(jsonPath("$.reports[1].id").value(2))
                .andExpect(jsonPath("$.reports[1].type").value("MESSAGE"));
    }

    @Test
    @DisplayName("사용자 신고 실패 - 자기 자신 신고")
    void should_returnBadRequest_when_reportSelf() throws Exception {
        // given
        Long userId = 100L;

        given(createReportUseCase.reportUser(
                eq(userId),
                eq(userId),
                eq(Report.ReportReason.HARASSMENT),
                eq("test")))
                .willThrow(new InvalidReportException("자기 자신을 신고할 수 없습니다."));

        String requestBody = """
                {
                    "reportedUserId": 100,
                    "reason": "HARASSMENT",
                    "description": "test"
                }
                """;

        // when & then
        mockMvc.perform(post("/api/v1/reports/users")
                        .param("reporterId", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("사용자 신고 실패 - 중복 신고")
    void should_returnBadRequest_when_duplicateReport() throws Exception {
        // given
        Long reporterId = 100L;
        Long reportedUserId = 200L;

        given(createReportUseCase.reportUser(
                eq(reporterId),
                eq(reportedUserId),
                eq(Report.ReportReason.SPAM),
                eq("test")))
                .willThrow(new InvalidReportException("이미 신고한 사용자입니다."));

        String requestBody = """
                {
                    "reportedUserId": 200,
                    "reason": "SPAM",
                    "description": "test"
                }
                """;

        // when & then
        mockMvc.perform(post("/api/v1/reports/users")
                        .param("reporterId", reporterId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("신고 실패 - reporterId 누락")
    void should_returnBadRequest_when_reporterIdMissing() throws Exception {
        // given
        String requestBody = """
                {
                    "reportedUserId": 200,
                    "reason": "HARASSMENT",
                    "description": "test"
                }
                """;

        // when & then
        mockMvc.perform(post("/api/v1/reports/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("신고 실패 - reason 누락")
    void should_returnBadRequest_when_reasonMissing() throws Exception {
        // given
        String requestBody = """
                {
                    "reportedUserId": 200,
                    "description": "test"
                }
                """;

        // when & then
        mockMvc.perform(post("/api/v1/reports/users")
                        .param("reporterId", "100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }
}
