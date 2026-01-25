package com.cotalk.adapter.inbound.rest;

import com.cotalk.adapter.inbound.rest.dto.report.ReportMessageRequest;
import com.cotalk.adapter.inbound.rest.dto.report.ReportResponse;
import com.cotalk.adapter.inbound.rest.dto.report.ReportUserRequest;
import com.cotalk.adapter.inbound.rest.dto.report.ReportsResponse;
import com.cotalk.domain.entity.Report;
import com.cotalk.domain.port.inbound.report.CreateReportUseCase;
import com.cotalk.domain.port.inbound.report.GetReportsUseCase;
import com.cotalk.infrastructure.security.CustomUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 신고 기능을 위한 REST 컨트롤러.
 * <p>
 * 사용자 신고, 메시지 신고, 내 신고 내역 조회 기능을 제공합니다.
 *
 * @author seunggu.lee
 */
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Tag(name = "신고", description = "신고 관리 API")
public class ReportController {

    private final CreateReportUseCase createReportUseCase;
    private final GetReportsUseCase getReportsUseCase;

    /**
     * 특정 사용자를 신고합니다.
     *
     * @param principal 인증된 사용자 정보
     * @param request 사용자 신고 요청
     * @return 생성된 신고 정보
     */
    @Operation(summary = "사용자 신고", description = "특정 사용자를 신고합니다.")
    @PostMapping("/users")
    public ResponseEntity<ReportResponse> reportUser(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody ReportUserRequest request) {
        Report report = createReportUseCase.reportUser(
                principal.getUserId(),
                request.reportedUserId(),
                request.reason(),
                request.description());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ReportResponse.from(report));
    }

    /**
     * 특정 메시지를 신고합니다.
     *
     * @param principal 인증된 사용자 정보
     * @param request 메시지 신고 요청
     * @return 생성된 신고 정보
     */
    @Operation(summary = "메시지 신고", description = "특정 메시지를 신고합니다.")
    @PostMapping("/messages")
    public ResponseEntity<ReportResponse> reportMessage(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody ReportMessageRequest request) {
        Report report = createReportUseCase.reportMessage(
                principal.getUserId(),
                request.reportedMessageId(),
                request.reason(),
                request.description());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ReportResponse.from(report));
    }

    /**
     * 내가 신고한 내역을 조회합니다.
     *
     * @param principal 인증된 사용자 정보
     * @return 내 신고 내역 목록
     */
    @Operation(summary = "내 신고 목록 조회", description = "내가 신고한 내역을 조회합니다.")
    @GetMapping("/my")
    public ResponseEntity<ReportsResponse> getMyReports(
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        List<Report> reports = getReportsUseCase.getMyReports(principal.getUserId());
        List<ReportResponse> responses = reports.stream()
                .map(ReportResponse::from)
                .toList();
        return ResponseEntity.ok(ReportsResponse.of(responses));
    }
}
