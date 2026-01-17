package com.cotalk.adapter.inbound.rest.dto.report;

import com.cotalk.domain.entity.Report;
import jakarta.validation.constraints.NotNull;

/**
 * 메시지 신고 요청 DTO.
 *
 * @param reportedMessageId 신고할 메시지 ID
 * @param reason            신고 사유
 * @param description       상세 설명 (선택)
 * @author seunggu.lee
 */
public record ReportMessageRequest(
        @NotNull(message = "신고할 메시지 ID는 필수입니다.")
        Long reportedMessageId,

        @NotNull(message = "신고 사유는 필수입니다.")
        Report.ReportReason reason,

        String description
) {}
