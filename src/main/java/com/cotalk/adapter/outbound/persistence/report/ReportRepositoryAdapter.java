package com.cotalk.adapter.outbound.persistence.report;

import com.cotalk.domain.entity.Report;
import com.cotalk.domain.port.outbound.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 신고 영속성 어댑터.
 * JPA를 통해 신고 데이터를 저장하고 조회한다.
 *
 * @author seunggu.lee
 */
@Repository
@RequiredArgsConstructor
public class ReportRepositoryAdapter implements ReportRepository {

    private final ReportJpaRepository reportJpaRepository;

    /**
     * 신고를 저장한다.
     *
     * @param report 저장할 신고 엔티티
     * @return 저장된 신고 엔티티
     */
    @Override
    public Report save(Report report) {
        return reportJpaRepository.save(report);
    }

    /**
     * ID로 신고를 조회한다.
     *
     * @param id 신고 ID
     * @return 신고 (Optional)
     */
    @Override
    public Optional<Report> findById(Long id) {
        return reportJpaRepository.findById(id);
    }

    /**
     * 신고자 ID로 신고 목록을 조회한다.
     *
     * @param reporterId 신고자 ID
     * @return 신고 목록
     */
    @Override
    public List<Report> findByReporterId(Long reporterId) {
        return reportJpaRepository.findByReporterId(reporterId);
    }

    /**
     * 피신고자 ID로 신고 목록을 조회한다.
     *
     * @param reportedUserId 피신고자 ID
     * @return 신고 목록
     */
    @Override
    public List<Report> findByReportedUserId(Long reportedUserId) {
        return reportJpaRepository.findByReportedUserId(reportedUserId);
    }

    /**
     * 신고자 ID와 피신고자 ID로 신고가 존재하는지 확인한다.
     *
     * @param reporterId 신고자 ID
     * @param reportedUserId 피신고자 ID
     * @return 신고 존재 여부
     */
    @Override
    public boolean existsByReporterIdAndReportedUserId(Long reporterId, Long reportedUserId) {
        return reportJpaRepository.existsByReporterIdAndReportedUserId(reporterId, reportedUserId);
    }

    /**
     * 신고자 ID와 신고된 메시지 ID로 신고가 존재하는지 확인한다.
     *
     * @param reporterId 신고자 ID
     * @param reportedMessageId 신고된 메시지 ID
     * @return 신고 존재 여부
     */
    @Override
    public boolean existsByReporterIdAndReportedMessageId(Long reporterId, Long reportedMessageId) {
        return reportJpaRepository.existsByReporterIdAndReportedMessageId(reporterId, reportedMessageId);
    }

    /**
     * 특정 상태의 신고 목록을 조회한다.
     *
     * @param status 신고 상태
     * @return 신고 목록
     */
    @Override
    public List<Report> findByStatus(Report.ReportStatus status) {
        return reportJpaRepository.findByStatus(status);
    }

    /**
     * 모든 신고 목록을 조회한다.
     *
     * @return 신고 목록
     */
    @Override
    public List<Report> findAll() {
        return reportJpaRepository.findAll();
    }

    /**
     * 전체 신고 수를 조회한다.
     *
     * @return 신고 수
     */
    @Override
    public long count() {
        return reportJpaRepository.count();
    }

    /**
     * 특정 상태의 신고 수를 조회한다.
     *
     * @param status 신고 상태
     * @return 신고 수
     */
    @Override
    public long countByStatus(Report.ReportStatus status) {
        return reportJpaRepository.countByStatus(status);
    }
}
