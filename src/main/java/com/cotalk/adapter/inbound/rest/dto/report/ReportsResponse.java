package com.cotalk.adapter.inbound.rest.dto.report;

import java.util.List;

/**
 * 신고 목록 응답 DTO.
 *
 * @param reports 신고 목록
 * @author seunggu.lee
 */
public record ReportsResponse(List<ReportResponse> reports) {

    /**
     * 신고 목록 응답을 생성한다.
     *
     * @param reports 신고 응답 DTO 목록
     * @return ReportsResponse 인스턴스
     */
    public static ReportsResponse of(List<ReportResponse> reports) {
        return new ReportsResponse(reports);
    }
}
