package com.cotalk.application.service.admin;

import com.cotalk.domain.entity.Report;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.exception.ReportNotFoundException;
import com.cotalk.domain.exception.UserNotFoundException;
import com.cotalk.domain.port.inbound.admin.AdminUseCase;
import com.cotalk.domain.port.outbound.ChatRoomRepository;
import com.cotalk.domain.port.outbound.MessageRepository;
import com.cotalk.domain.port.outbound.ReportRepository;
import com.cotalk.domain.port.outbound.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.cotalk.infrastructure.config.CacheConfig.STATISTICS_CACHE;
import static com.cotalk.infrastructure.config.CacheConfig.USER_CACHE;

/**
 * 관리자 기능 유스케이스 구현체.
 * 신고 처리, 사용자 관리, 통계 조회 등 관리자 기능을 제공한다.
 *
 * @author seunggu.lee
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AdminService implements AdminUseCase {

    private static final int MAX_ADMIN_LIST_SIZE = 100;

    private final UserRepository userRepository;
    private final ReportRepository reportRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final MessageRepository messageRepository;

    /**
     * 처리 대기 중인 신고 목록을 조회한다.
     * 메모리 보호를 위해 최대 {@value MAX_ADMIN_LIST_SIZE}건으로 제한한다.
     *
     * @return 대기 중인 신고 목록
     */
    @Override
    @Transactional(readOnly = true)
    public List<Report> getPendingReports() {
        return reportRepository.findByStatus(Report.ReportStatus.PENDING)
                .stream()
                .limit(MAX_ADMIN_LIST_SIZE)
                .toList();
    }

    /**
     * 처리 대기 중인 신고 목록을 DB 레벨 페이지네이션으로 조회한다.
     *
     * @param pageable 페이지네이션 정보
     * @return 페이지네이션된 대기 중인 신고 목록
     */
    @Override
    @Transactional(readOnly = true)
    public Page<Report> getPendingReports(Pageable pageable) {
        return reportRepository.findByStatus(Report.ReportStatus.PENDING, pageable);
    }

    /**
     * 전체 신고 목록을 조회한다.
     * 상태 필터가 제공되면 해당 상태의 신고만 조회한다.
     * 메모리 보호를 위해 최대 {@value MAX_ADMIN_LIST_SIZE}건으로 제한한다.
     *
     * @param status 필터링할 신고 상태 (null이면 전체 조회)
     * @return 신고 목록
     */
    @Override
    @Transactional(readOnly = true)
    public List<Report> getAllReports(Report.ReportStatus status) {
        List<Report> reports;
        if (status == null) {
            reports = reportRepository.findAll();
        } else {
            reports = reportRepository.findByStatus(status);
        }
        return reports.stream().limit(MAX_ADMIN_LIST_SIZE).toList();
    }

    /**
     * 신고를 처리한다.
     * 신고 상태를 변경하고 관리자 메모를 추가한다.
     *
     * @param reportId  처리할 신고 ID
     * @param adminId   처리하는 관리자 ID
     * @param newStatus 변경할 신고 상태
     * @param adminNote 관리자 메모
     * @return 처리된 신고 정보
     * @throws ReportNotFoundException 신고를 찾을 수 없는 경우
     */
    @Override
    public Report processReport(Long reportId, Long adminId, Report.ReportStatus newStatus, String adminNote) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ReportNotFoundException(reportId));

        report.process(newStatus, adminNote, adminId);
        return reportRepository.save(report);
    }

    /**
     * 전체 사용자 목록을 조회한다.
     * 메모리 보호를 위해 최대 {@value MAX_ADMIN_LIST_SIZE}건으로 제한한다.
     *
     * @return 전체 사용자 목록
     */
    @Override
    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll().stream()
                .limit(MAX_ADMIN_LIST_SIZE)
                .toList();
    }

    /**
     * 전체 사용자 목록을 DB 레벨 페이지네이션으로 조회한다.
     *
     * @param pageable 페이지네이션 정보
     * @return 페이지네이션된 사용자 목록
     */
    @Override
    @Transactional(readOnly = true)
    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    /**
     * 특정 상태의 사용자 목록을 조회한다.
     * 메모리 보호를 위해 최대 {@value MAX_ADMIN_LIST_SIZE}건으로 제한한다.
     *
     * @param status 필터링할 사용자 상태
     * @return 해당 상태의 사용자 목록
     */
    @Override
    @Transactional(readOnly = true)
    public List<User> getUsersByStatus(User.UserStatus status) {
        return userRepository.findByStatus(status)
                .stream()
                .limit(MAX_ADMIN_LIST_SIZE)
                .toList();
    }

    /**
     * 특정 상태의 사용자 목록을 DB 레벨 페이지네이션으로 조회한다.
     *
     * @param status   필터링할 사용자 상태
     * @param pageable 페이지네이션 정보
     * @return 페이지네이션된 사용자 목록
     */
    @Override
    @Transactional(readOnly = true)
    public Page<User> getUsersByStatus(User.UserStatus status, Pageable pageable) {
        return userRepository.findByStatus(status, pageable);
    }

    /**
     * 사용자를 정지시킨다.
     * 관련 캐시(사용자, 통계)를 무효화한다.
     *
     * @param adminId 처리하는 관리자 ID
     * @param userId  정지시킬 사용자 ID
     * @param reason  정지 사유
     * @return 정지된 사용자 정보
     * @throws UserNotFoundException 사용자를 찾을 수 없는 경우
     */
    @Override
    @CacheEvict(value = {USER_CACHE, STATISTICS_CACHE}, allEntries = true)
    public User suspendUser(Long adminId, Long userId, String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        user.suspend();
        return userRepository.save(user);
    }

    /**
     * 사용자를 활성화한다.
     * 정지된 사용자를 다시 활성 상태로 변경하며, 관련 캐시를 무효화한다.
     *
     * @param adminId 처리하는 관리자 ID
     * @param userId  활성화할 사용자 ID
     * @return 활성화된 사용자 정보
     * @throws UserNotFoundException 사용자를 찾을 수 없는 경우
     */
    @Override
    @CacheEvict(value = {USER_CACHE, STATISTICS_CACHE}, allEntries = true)
    public User activateUser(Long adminId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        user.activate();
        return userRepository.save(user);
    }

    /**
     * 관리자 통계를 조회한다.
     * 사용자, 신고, 채팅방, 메시지 관련 통계를 반환하며 캐시된다.
     *
     * @return 관리자 통계 정보
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = STATISTICS_CACHE, key = "'admin-stats'")
    public AdminStatistics getStatistics() {
        return new AdminStatistics(
                userRepository.count(),
                userRepository.countByStatus(User.UserStatus.ACTIVE),
                userRepository.countByStatus(User.UserStatus.SUSPENDED),
                reportRepository.count(),
                reportRepository.countByStatus(Report.ReportStatus.PENDING),
                chatRoomRepository.count(),
                messageRepository.count()
        );
    }
}
