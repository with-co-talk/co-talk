package com.cotalk.adapter.inbound.rest.dto.admin;

import com.cotalk.domain.entity.Report;
import jakarta.validation.constraints.NotNull;

/**
 * 신고 처리 요청 DTO.
 *
 * @param adminId   관리자 ID
 * @param status    처리 상태
 * @param adminNote 관리자 메모 (선택)
 * @author seunggu.lee
 */
public record ProcessReportRequest(
        @NotNull(message = "관리자 ID는 필수입니다.")
        Long adminId,

        @NotNull(message = "처리 상태는 필수입니다.")
        Report.ReportStatus status,

        String adminNote
) {}
