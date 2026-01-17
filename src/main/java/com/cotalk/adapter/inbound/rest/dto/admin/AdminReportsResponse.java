package com.cotalk.adapter.inbound.rest.dto.admin;

import java.util.List;

/**
 * 관리자용 신고 목록 응답 DTO.
 *
 * @param reports 신고 목록
 * @author seunggu.lee
 */
public record AdminReportsResponse(List<AdminReportDto> reports) {

    /**
     * 신고 목록 응답을 생성한다.
     *
     * @param reports 신고 DTO 목록
     * @return AdminReportsResponse 인스턴스
     */
    public static AdminReportsResponse of(List<AdminReportDto> reports) {
        return new AdminReportsResponse(reports);
    }
}
