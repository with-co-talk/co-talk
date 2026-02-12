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
import com.cotalk.infrastructure.security.CustomUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "관리자", description = "관리자 API")
public class AdminController {

    private final AdminUseCase adminUseCase;

    // ==================== 신고 관리 ====================

    /**
     * 처리 대기 중인 신고 목록을 조회합니다.
     * DB 레벨 페이지네이션을 사용하여 대규모 데이터에서도 효율적으로 동작합니다.
     *
     * @param page 페이지 번호 (기본값: 0)
     * @param size 페이지 크기 (기본값: 20, 최대: 100)
     * @return 페이지네이션된 대기 중인 신고 목록
     */
    @Operation(summary = "대기 중인 신고 목록 조회", description = "처리 대기 중인 신고 목록을 조회합니다.")
    @GetMapping("/reports/pending")
    public ResponseEntity<AdminReportsResponse> getPendingReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int safeSize = Math.min(size, 100);
        Page<Report> reportPage = adminUseCase.getPendingReports(PageRequest.of(page, safeSize));
        List<AdminReportDto> reportDtos = reportPage.getContent().stream()
                .map(AdminReportDto::from)
                .toList();
        return ResponseEntity.ok(AdminReportsResponse.of(reportDtos, reportPage));
    }

    /**
     * 상태별 신고 목록을 조회합니다.
     * 이 엔드포인트는 기존 호환성을 위해 인메모리 페이지네이션을 유지합니다.
     *
     * @param status 신고 상태 (null인 경우 전체 조회)
     * @param page   페이지 번호 (기본값: 0)
     * @param size   페이지 크기 (기본값: 20, 최대: 100)
     * @return 신고 목록
     */
    @Operation(summary = "신고 목록 조회", description = "상태별 신고 목록을 조회합니다.")
    @GetMapping("/reports")
    public ResponseEntity<AdminReportsResponse> getReports(
            @RequestParam(required = false) Report.ReportStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<Report> reports = adminUseCase.getAllReports(status);
        int safeSize = Math.min(size, 100);
        List<AdminReportDto> reportDtos = reports.stream()
                .skip((long) page * safeSize)
                .limit(safeSize)
                .map(AdminReportDto::from)
                .toList();
        return ResponseEntity.ok(AdminReportsResponse.of(reportDtos));
    }

    /**
     * 신고를 처리합니다.
     *
     * @param principal 인증된 관리자 정보
     * @param reportId  신고 ID
     * @param request   신고 처리 요청 (처리 상태, 관리자 메모)
     * @return 처리된 신고 정보
     */
    @Operation(summary = "신고 처리", description = "신고를 처리합니다.")
    @PostMapping("/reports/{reportId}/process")
    public ResponseEntity<AdminReportDto> processReport(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long reportId,
            @Valid @RequestBody ProcessReportRequest request) {
        Report report = adminUseCase.processReport(
                reportId, principal.getUserId(), request.status(), request.adminNote());
        return ResponseEntity.ok(AdminReportDto.from(report));
    }

    // ==================== 사용자 관리 ====================

    /**
     * 모든 사용자 목록을 조회합니다.
     * DB 레벨 페이지네이션을 사용하여 대규모 데이터에서도 효율적으로 동작합니다.
     *
     * @param status 사용자 상태 (null인 경우 전체 조회)
     * @param page   페이지 번호 (기본값: 0)
     * @param size   페이지 크기 (기본값: 20, 최대: 100)
     * @return 페이지네이션된 사용자 목록
     */
    @Operation(summary = "전체 사용자 목록 조회", description = "모든 사용자 목록을 조회합니다.")
    @GetMapping("/users")
    public ResponseEntity<AdminUsersResponse> getAllUsers(
            @RequestParam(required = false) User.UserStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int safeSize = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, safeSize);
        Page<User> userPage = status == null
                ? adminUseCase.getAllUsers(pageable)
                : adminUseCase.getUsersByStatus(status, pageable);
        List<AdminUserDto> userDtos = userPage.getContent().stream()
                .map(AdminUserDto::from)
                .toList();
        return ResponseEntity.ok(AdminUsersResponse.of(userDtos, userPage));
    }

    /**
     * 사용자를 정지 처리합니다.
     *
     * @param principal 인증된 관리자 정보
     * @param userId    정지할 사용자 ID
     * @param request   정지 요청 (정지 사유)
     * @return 정지된 사용자 정보
     */
    @Operation(summary = "사용자 정지", description = "사용자를 정지 처리합니다.")
    @PostMapping("/users/{userId}/suspend")
    public ResponseEntity<AdminUserDto> suspendUser(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long userId,
            @Valid @RequestBody SuspendUserRequest request) {
        User user = adminUseCase.suspendUser(principal.getUserId(), userId, request.reason());
        return ResponseEntity.ok(AdminUserDto.from(user));
    }

    /**
     * 정지된 사용자를 활성화합니다.
     *
     * @param principal 인증된 관리자 정보
     * @param userId    활성화할 사용자 ID
     * @return 활성화된 사용자 정보
     */
    @Operation(summary = "사용자 활성화", description = "정지된 사용자를 활성화합니다.")
    @PostMapping("/users/{userId}/activate")
    public ResponseEntity<AdminUserDto> activateUser(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long userId) {
        User user = adminUseCase.activateUser(principal.getUserId(), userId);
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
