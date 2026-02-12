package com.cotalk.adapter.inbound.rest.dto.admin;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 관리자용 신고 목록 응답 DTO.
 * 페이지네이션 메타데이터를 포함한다.
 *
 * @param reports       신고 목록
 * @param page          현재 페이지 번호 (0-based)
 * @param size          페이지 크기
 * @param totalElements 전체 요소 수
 * @param totalPages    전체 페이지 수
 * @author seunggu.lee
 */
public record AdminReportsResponse(
        List<AdminReportDto> reports,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    /**
     * 신고 목록 응답을 생성한다. (하위 호환용)
     *
     * @param reports 신고 DTO 목록
     * @return AdminReportsResponse 인스턴스
     */
    public static AdminReportsResponse of(List<AdminReportDto> reports) {
        return new AdminReportsResponse(reports, 0, reports.size(), reports.size(), 1);
    }

    /**
     * Page 객체와 매핑된 DTO 목록으로부터 응답을 생성한다.
     *
     * @param reports  신고 DTO 목록
     * @param pageData Page 메타데이터 소스
     * @return AdminReportsResponse 인스턴스
     */
    public static AdminReportsResponse of(List<AdminReportDto> reports, Page<?> pageData) {
        return new AdminReportsResponse(
                reports,
                pageData.getNumber(),
                pageData.getSize(),
                pageData.getTotalElements(),
                pageData.getTotalPages()
        );
    }
}
