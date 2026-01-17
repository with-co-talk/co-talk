package com.cotalk.adapter.inbound.rest;

import com.cotalk.adapter.inbound.rest.dto.admin.AdminReportDto;
import com.cotalk.adapter.inbound.rest.dto.admin.AdminReportsResponse;
import com.cotalk.adapter.inbound.rest.dto.admin.AdminUserDto;
import com.cotalk.adapter.inbound.rest.dto.admin.AdminUsersResponse;
import com.cotalk.adapter.inbound.rest.dto.admin.ProcessReportRequest;
import com.cotalk.adapter.inbound.rest.dto.admin.SuspendUserRequest;
import com.cotalk.domain.entity.Report;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.port.inbound.admin.AdminUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 관리자 기능을 위한 REST 컨트롤러.
 * <p>
 * 신고 관리, 사용자 관리, 시스템 통계 조회 등 관리자 전용 기능을 제공합니다.
 *
 * @author seunggu.lee
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "관리자", description = "관리자 API")
public class AdminController {

    private final AdminUseCase adminUseCase;

    // ==================== 신고 관리 ====================

    /**
     * 처리 대기 중인 신고 목록을 조회합니다.
     *
     * @return 대기 중인 신고 목록
     */
    @Operation(summary = "대기 중인 신고 목록 조회", description = "처리 대기 중인 신고 목록을 조회합니다.")
    @GetMapping("/reports/pending")
    public ResponseEntity<AdminReportsResponse> getPendingReports() {
        List<Report> reports = adminUseCase.getPendingReports();
        List<AdminReportDto> reportDtos = reports.stream()
                .map(AdminReportDto::from)
                .toList();
        return ResponseEntity.ok(AdminReportsResponse.of(reportDtos));
    }

    /**
     * 상태별 신고 목록을 조회합니다.
     *
     * @param status 신고 상태 (null인 경우 전체 조회)
     * @return 신고 목록
     */
    @Operation(summary = "신고 목록 조회", description = "상태별 신고 목록을 조회합니다.")
    @GetMapping("/reports")
    public ResponseEntity<AdminReportsResponse> getReports(
            @RequestParam(required = false) Report.ReportStatus status) {
        List<Report> reports = adminUseCase.getAllReports(status);
        List<AdminReportDto> reportDtos = reports.stream()
                .map(AdminReportDto::from)
                .toList();
        return ResponseEntity.ok(AdminReportsResponse.of(reportDtos));
    }

    /**
     * 신고를 처리합니다.
     *
     * @param reportId 신고 ID
     * @param request  신고 처리 요청 (관리자 ID, 처리 상태, 관리자 메모)
     * @return 처리된 신고 정보
     */
    @Operation(summary = "신고 처리", description = "신고를 처리합니다.")
    @PostMapping("/reports/{reportId}/process")
    public ResponseEntity<AdminReportDto> processReport(
            @PathVariable Long reportId,
            @Valid @RequestBody ProcessReportRequest request) {
        Report report = adminUseCase.processReport(
                reportId, request.adminId(), request.status(), request.adminNote());
        return ResponseEntity.ok(AdminReportDto.from(report));
    }

    // ==================== 사용자 관리 ====================

    /**
     * 모든 사용자 목록을 조회합니다.
     *
     * @param status 사용자 상태 (null인 경우 전체 조회)
     * @return 사용자 목록
     */
    @Operation(summary = "전체 사용자 목록 조회", description = "모든 사용자 목록을 조회합니다.")
    @GetMapping("/users")
    public ResponseEntity<AdminUsersResponse> getAllUsers(
            @RequestParam(required = false) User.UserStatus status) {
        List<User> users = status == null
                ? adminUseCase.getAllUsers()
                : adminUseCase.getUsersByStatus(status);
        List<AdminUserDto> userDtos = users.stream()
                .map(AdminUserDto::from)
                .toList();
        return ResponseEntity.ok(AdminUsersResponse.of(userDtos));
    }

    /**
     * 사용자를 정지 처리합니다.
     *
     * @param userId  정지할 사용자 ID
     * @param request 정지 요청 (관리자 ID, 정지 사유)
     * @return 정지된 사용자 정보
     */
    @Operation(summary = "사용자 정지", description = "사용자를 정지 처리합니다.")
    @PostMapping("/users/{userId}/suspend")
    public ResponseEntity<AdminUserDto> suspendUser(
            @PathVariable Long userId,
            @Valid @RequestBody SuspendUserRequest request) {
        User user = adminUseCase.suspendUser(request.adminId(), userId, request.reason());
        return ResponseEntity.ok(AdminUserDto.from(user));
    }

    /**
     * 정지된 사용자를 활성화합니다.
     *
     * @param userId  활성화할 사용자 ID
     * @param adminId 관리자 ID
     * @return 활성화된 사용자 정보
     */
    @Operation(summary = "사용자 활성화", description = "정지된 사용자를 활성화합니다.")
    @PostMapping("/users/{userId}/activate")
    public ResponseEntity<AdminUserDto> activateUser(
            @PathVariable Long userId,
            @RequestParam Long adminId) {
        User user = adminUseCase.activateUser(adminId, userId);
        return ResponseEntity.ok(AdminUserDto.from(user));
    }

    // ==================== 통계 ====================

    /**
     * 전체 시스템 통계를 조회합니다.
     *
     * @return 관리자 통계 정보
     */
    @Operation(summary = "관리자 통계 조회", description = "전체 시스템 통계를 조회합니다.")
    @GetMapping("/statistics")
    public ResponseEntity<AdminUseCase.AdminStatistics> getStatistics() {
        return ResponseEntity.ok(adminUseCase.getStatistics());
    }
}
