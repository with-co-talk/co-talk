package com.cotalk.domain.port.inbound.admin;

import com.cotalk.domain.entity.Report;
import com.cotalk.domain.entity.User;

import java.util.List;

/**
 * 관리자 유스케이스.
 * 신고 관리, 사용자 관리, 통계 조회 등 관리자 기능을 제공한다.
 *
 * @author seunggu.lee
 */
public interface AdminUseCase {

    /**
     * 처리 대기 중인 신고 목록을 조회한다.
     *
     * @return 대기 중인 신고 목록
     */
    List<Report> getPendingReports();

    /**
     * 특정 상태의 모든 신고 목록을 조회한다.
     *
     * @param status 신고 상태
     * @return 신고 목록
     */
    List<Report> getAllReports(Report.ReportStatus status);

    /**
     * 신고를 처리한다.
     *
     * @param reportId 신고 ID
     * @param adminId 처리하는 관리자 ID
     * @param newStatus 변경할 신고 상태
     * @param adminNote 관리자 메모
     * @return 처리된 신고
     */
    Report processReport(Long reportId, Long adminId, Report.ReportStatus newStatus, String adminNote);

    /**
     * 모든 사용자 목록을 조회한다.
     *
     * @return 사용자 목록
     */
    List<User> getAllUsers();

    /**
     * 특정 상태의 사용자 목록을 조회한다.
     *
     * @param status 사용자 상태
     * @return 사용자 목록
     */
    List<User> getUsersByStatus(User.UserStatus status);

    /**
     * 사용자를 정지시킨다.
     *
     * @param adminId 관리자 ID
     * @param userId 정지할 사용자 ID
     * @param reason 정지 사유
     * @return 정지된 사용자
     */
    User suspendUser(Long adminId, Long userId, String reason);

    /**
     * 사용자 정지를 해제하고 활성화한다.
     *
     * @param adminId 관리자 ID
     * @param userId 활성화할 사용자 ID
     * @return 활성화된 사용자
     */
    User activateUser(Long adminId, Long userId);

    /**
     * 관리자 통계를 조회한다.
     *
     * @return 관리자 통계
     */
    AdminStatistics getStatistics();

    /**
     * 관리자 통계 정보.
     *
     * @param totalUsers 전체 사용자 수
     * @param activeUsers 활성 사용자 수
     * @param suspendedUsers 정지된 사용자 수
     * @param totalReports 전체 신고 수
     * @param pendingReports 대기 중인 신고 수
     * @param totalChatRooms 전체 채팅방 수
     * @param totalMessages 전체 메시지 수
     */
    record AdminStatistics(
            long totalUsers,
            long activeUsers,
            long suspendedUsers,
            long totalReports,
            long pendingReports,
            long totalChatRooms,
            long totalMessages
    ) {}
}
