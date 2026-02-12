package com.cotalk.domain.port.outbound;

import com.cotalk.domain.entity.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * 신고 레포지토리 포트.
 * 사용자 및 메시지 신고 데이터 저장 및 조회를 위한 인터페이스를 정의한다.
 *
 * @author seunggu.lee
 */
public interface ReportRepository {

    /**
     * 신고를 저장한다.
     *
     * @param report 저장할 신고
     * @return 저장된 신고
     */
    Report save(Report report);

    /**
     * ID로 신고를 조회한다.
     *
     * @param id 신고 ID
     * @return 조회된 신고 (Optional)
     */
    Optional<Report> findById(Long id);

    /**
     * 특정 신고자가 제출한 모든 신고를 조회한다.
     *
     * @param reporterId 신고자 ID
     * @return 신고 목록
     */
    List<Report> findByReporterId(Long reporterId);

    /**
     * 특정 사용자가 신고당한 모든 신고를 조회한다.
     *
     * @param reportedUserId 신고당한 사용자 ID
     * @return 신고 목록
     */
    List<Report> findByReportedUserId(Long reportedUserId);

    /**
     * 특정 신고자와 신고당한 사용자 간의 신고 존재 여부를 확인한다.
     *
     * @param reporterId     신고자 ID
     * @param reportedUserId 신고당한 사용자 ID
     * @return 존재 여부
     */
    boolean existsByReporterIdAndReportedUserId(Long reporterId, Long reportedUserId);

    /**
     * 특정 신고자와 신고당한 메시지 간의 신고 존재 여부를 확인한다.
     *
     * @param reporterId        신고자 ID
     * @param reportedMessageId 신고당한 메시지 ID
     * @return 존재 여부
     */
    boolean existsByReporterIdAndReportedMessageId(Long reporterId, Long reportedMessageId);

    /**
     * 특정 상태의 모든 신고를 조회한다.
     *
     * @param status 신고 상태
     * @return 신고 목록
     */
    List<Report> findByStatus(Report.ReportStatus status);

    /**
     * 특정 상태의 신고를 DB 레벨 페이지네이션으로 조회한다.
     *
     * @param status   신고 상태
     * @param pageable 페이지네이션 정보
     * @return 페이지네이션된 신고 목록
     */
    Page<Report> findByStatus(Report.ReportStatus status, Pageable pageable);

    /**
     * 모든 신고를 조회한다.
     *
     * @return 전체 신고 목록
     */
    List<Report> findAll();

    /**
     * 모든 신고를 DB 레벨 페이지네이션으로 조회한다.
     *
     * @param pageable 페이지네이션 정보
     * @return 페이지네이션된 신고 목록
     */
    Page<Report> findAll(Pageable pageable);

    /**
     * 전체 신고 수를 조회한다.
     *
     * @return 신고 수
     */
    long count();

    /**
     * 특정 상태의 신고 수를 조회한다.
     *
     * @param status 신고 상태
     * @return 해당 상태의 신고 수
     */
    long countByStatus(Report.ReportStatus status);

    /**
     * 특정 신고자가 제출한 모든 신고를 삭제한다.
     *
     * @param reporterId 신고자 ID
     */
    void deleteByReporterId(Long reporterId);
}
