package com.cotalk.adapter.outbound.persistence.report;

import com.cotalk.adapter.outbound.persistence.entity.ReportJpaEntity;
import com.cotalk.adapter.outbound.persistence.mapper.ReportMapper;
import com.cotalk.domain.entity.Report;
import com.cotalk.domain.port.outbound.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 신고 영속성 어댑터.
 * JPA 엔티티와 도메인 간 매핑을 수행하며, 도메인 포트를 구현한다.
 *
 * @author seunggu.lee
 */
@Repository
@RequiredArgsConstructor
public class ReportRepositoryAdapter implements ReportRepository {

    private final ReportJpaRepository reportJpaRepository;
    private final ReportMapper mapper;

    /**
     * 신고를 저장한다.
     *
     * @param report 저장할 신고 엔티티
     * @return 저장된 신고 엔티티
     */
    @Override
    public Report save(Report report) {
        ReportJpaEntity saved = reportJpaRepository.save(mapper.toJpa(report));
        return mapper.toDomain(saved);
    }

    /**
     * ID로 신고를 조회한다.
     *
     * @param id 신고 ID
     * @return 신고 (Optional)
     */
    @Override
    public Optional<Report> findById(Long id) {
        return reportJpaRepository.findById(id).map(mapper::toDomain);
    }

    /**
     * 신고자 ID로 신고 목록을 조회한다.
     *
     * @param reporterId 신고자 ID
     * @return 신고 목록
     */
    @Override
    public List<Report> findByReporterId(Long reporterId) {
        return reportJpaRepository.findByReporterId(reporterId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    /**
     * 피신고자 ID로 신고 목록을 조회한다.
     *
     * @param reportedUserId 피신고자 ID
     * @return 신고 목록
     */
    @Override
    public List<Report> findByReportedUserId(Long reportedUserId) {
        return reportJpaRepository.findByReportedUserId(reportedUserId).stream()
                .map(mapper::toDomain)
                .toList();
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
        return reportJpaRepository.findByStatus(status).stream()
                .map(mapper::toDomain)
                .toList();
    }

    /**
     * 특정 상태의 신고 목록을 페이지네이션하여 조회한다.
     *
     * @param status   신고 상태
     * @param pageable 페이지네이션 정보
     * @return 페이지네이션된 신고 목록
     */
    @Override
    public Page<Report> findByStatus(Report.ReportStatus status, Pageable pageable) {
        return reportJpaRepository.findByStatus(status, pageable).map(mapper::toDomain);
    }

    /**
     * 모든 신고 목록을 조회한다.
     *
     * @return 신고 목록
     */
    @Override
    public List<Report> findAll() {
        return reportJpaRepository.findAll().stream()
                .map(mapper::toDomain)
                .toList();
    }

    /**
     * 모든 신고 목록을 페이지네이션하여 조회한다.
     *
     * @param pageable 페이지네이션 정보
     * @return 페이지네이션된 신고 목록
     */
    @Override
    public Page<Report> findAll(Pageable pageable) {
        return reportJpaRepository.findAll(pageable).map(mapper::toDomain);
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

    /**
     * 신고자 ID로 모든 신고를 삭제한다.
     *
     * @param reporterId 신고자 ID
     */
    @Override
    public void deleteByReporterId(Long reporterId) {
        reportJpaRepository.deleteByReporterId(reporterId);
    }

    /**
     * 피신고자 ID로 모든 신고를 삭제한다.
     *
     * @param reportedUserId 피신고자 ID
     */
    @Override
    public void deleteByReportedUserId(Long reportedUserId) {
        reportJpaRepository.deleteByReportedUserId(reportedUserId);
    }

    /**
     * 특정 발신자가 보낸 메시지를 대상으로 한 모든 신고를 삭제한다.
     *
     * @param senderId 메시지 발신자(사용자) ID
     */
    @Override
    public void deleteByReportedMessageSenderId(Long senderId) {
        reportJpaRepository.deleteByReportedMessageSenderId(senderId);
    }
}
