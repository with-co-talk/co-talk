package com.cotalk.domain.port.inbound.report;

import com.cotalk.domain.entity.Report;

import java.util.List;

/**
 * 신고 조회 유스케이스.
 * 사용자가 제출한 신고 목록을 조회한다.
 *
 * @author seunggu.lee
 */
public interface GetReportsUseCase {

    /**
     * 사용자가 제출한 신고 목록을 조회한다.
     *
     * @param userId 사용자 ID
     * @return 신고 목록
     */
    List<Report> getMyReports(Long userId);
}
