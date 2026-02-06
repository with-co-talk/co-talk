package com.cotalk.adapter.outbound.persistence.report;

import com.cotalk.domain.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 신고 JPA 리포지토리.
 * Spring Data JPA를 통해 신고 데이터에 접근한다.
 *
 * @author seunggu.lee
 */
public interface ReportJpaRepository extends JpaRepository<Report, Long> {

    /**
     * 신고자 ID로 신고 목록을 조회한다.
     *
     * @param reporterId 신고자 ID
     * @return 신고 목록
     */
    List<Report> findByReporterId(Long reporterId);

    /**
     * 피신고자 ID로 신고 목록을 조회한다.
     *
     * @param reportedUserId 피신고자 ID
     * @return 신고 목록
     */
    List<Report> findByReportedUserId(Long reportedUserId);

    /**
     * 신고자 ID와 피신고자 ID로 신고가 존재하는지 확인한다.
     *
     * @param reporterId 신고자 ID
     * @param reportedUserId 피신고자 ID
     * @return 신고 존재 여부
     */
    boolean existsByReporterIdAndReportedUserId(Long reporterId, Long reportedUserId);

    /**
     * 신고자 ID와 신고된 메시지 ID로 신고가 존재하는지 확인한다.
     *
     * @param reporterId 신고자 ID
     * @param reportedMessageId 신고된 메시지 ID
     * @return 신고 존재 여부
     */
    boolean existsByReporterIdAndReportedMessageId(Long reporterId, Long reportedMessageId);

    /**
     * 특정 상태의 신고 목록을 조회한다.
     *
     * @param status 신고 상태
     * @return 신고 목록
     */
    List<Report> findByStatus(Report.ReportStatus status);

    /**
     * 특정 상태의 신고 수를 조회한다.
     *
     * @param status 신고 상태
     * @return 신고 수
     */
    long countByStatus(Report.ReportStatus status);

    /**
     * 신고자 ID로 모든 신고를 삭제한다.
     *
     * @param reporterId 신고자 ID
     */
    void deleteByReporterId(Long reporterId);
}
