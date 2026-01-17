package com.cotalk.application.service.report;

import com.cotalk.domain.entity.Report;
import com.cotalk.domain.exception.InvalidReportException;
import com.cotalk.domain.port.inbound.report.CreateReportUseCase;
import com.cotalk.domain.port.inbound.report.GetReportsUseCase;
import com.cotalk.domain.port.outbound.ReportRepository;
import com.cotalk.infrastructure.id.SnowflakeIdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 신고 관련 유스케이스 구현체.
 * 사용자 및 메시지 신고 생성과 신고 내역 조회 기능을 제공한다.
 *
 * @author seunggu.lee
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ReportService implements CreateReportUseCase, GetReportsUseCase {

    private final ReportRepository reportRepository;
    private final SnowflakeIdGenerator idGenerator;

    /**
     * 사용자를 신고한다.
     * 자기 자신 신고 및 중복 신고는 불가하다.
     *
     * @param reporterId     신고하는 사용자 ID
     * @param reportedUserId 신고당하는 사용자 ID
     * @param reason         신고 사유
     * @param description    신고 상세 설명
     * @return 생성된 신고 정보
     * @throws InvalidReportException 자기 자신을 신고하거나 중복 신고인 경우
     */
    @Override
    public Report reportUser(Long reporterId, Long reportedUserId, Report.ReportReason reason, String description) {
        validateSelfReport(reporterId, reportedUserId);
        validateDuplicateUserReport(reporterId, reportedUserId);

        Report report = Report.builder()
                .id(idGenerator.nextId())
                .reporterId(reporterId)
                .reportedUserId(reportedUserId)
                .type(Report.ReportType.USER)
                .reason(reason)
                .description(description)
                .build();

        return reportRepository.save(report);
    }

    /**
     * 메시지를 신고한다.
     * 중복 신고는 불가하다.
     *
     * @param reporterId        신고하는 사용자 ID
     * @param reportedMessageId 신고당하는 메시지 ID
     * @param reason            신고 사유
     * @param description       신고 상세 설명
     * @return 생성된 신고 정보
     * @throws InvalidReportException 중복 신고인 경우
     */
    @Override
    public Report reportMessage(Long reporterId, Long reportedMessageId, Report.ReportReason reason, String description) {
        validateDuplicateMessageReport(reporterId, reportedMessageId);

        Report report = Report.builder()
                .id(idGenerator.nextId())
                .reporterId(reporterId)
                .reportedMessageId(reportedMessageId)
                .type(Report.ReportType.MESSAGE)
                .reason(reason)
                .description(description)
                .build();

        return reportRepository.save(report);
    }

    /**
     * 본인이 생성한 신고 내역을 조회한다.
     *
     * @param userId 조회할 사용자 ID
     * @return 본인이 생성한 신고 목록
     */
    @Override
    @Transactional(readOnly = true)
    public List<Report> getMyReports(Long userId) {
        return reportRepository.findByReporterId(userId);
    }

    private void validateSelfReport(Long reporterId, Long reportedUserId) {
        if (reporterId.equals(reportedUserId)) {
            throw new InvalidReportException("자기 자신을 신고할 수 없습니다.");
        }
    }

    private void validateDuplicateUserReport(Long reporterId, Long reportedUserId) {
        if (reportRepository.existsByReporterIdAndReportedUserId(reporterId, reportedUserId)) {
            throw new InvalidReportException("이미 신고한 사용자입니다.");
        }
    }

    private void validateDuplicateMessageReport(Long reporterId, Long reportedMessageId) {
        if (reportRepository.existsByReporterIdAndReportedMessageId(reporterId, reportedMessageId)) {
            throw new InvalidReportException("이미 신고한 메시지입니다.");
        }
    }
}
