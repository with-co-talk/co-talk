package com.cotalk.adapter.inbound.rest.dto.report;

import com.cotalk.domain.entity.Report;
import jakarta.validation.constraints.NotNull;

/**
 * 사용자 신고 요청 DTO.
 *
 * @param reportedUserId 신고할 사용자 ID
 * @param reason         신고 사유
 * @param description    상세 설명 (선택)
 * @author seunggu.lee
 */
public record ReportUserRequest(
        @NotNull(message = "신고할 사용자 ID는 필수입니다.")
        Long reportedUserId,

        @NotNull(message = "신고 사유는 필수입니다.")
        Report.ReportReason reason,

        String description
) {}
