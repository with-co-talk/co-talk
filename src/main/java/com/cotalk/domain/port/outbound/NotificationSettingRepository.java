package com.cotalk.domain.port.outbound;

import com.cotalk.domain.entity.NotificationSetting;

import java.util.List;
import java.util.Optional;

/**
 * 알림 설정 레포지토리 포트.
 * 사용자 알림 설정 데이터 저장 및 조회를 위한 인터페이스를 정의한다.
 *
 * @author seunggu.lee
 */
public interface NotificationSettingRepository {

    /**
     * 알림 설정을 저장한다.
     *
     * @param setting 저장할 알림 설정
     * @return 저장된 알림 설정
     */
    NotificationSetting save(NotificationSetting setting);

    /**
     * 사용자 ID로 알림 설정을 조회한다.
     *
     * @param userId 사용자 ID
     * @return 조회된 알림 설정 (Optional)
     */
    Optional<NotificationSetting> findByUserId(Long userId);

    /**
     * 여러 사용자 ID로 알림 설정을 일괄 조회한다.
     *
     * @param userIds 사용자 ID 목록
     * @return 조회된 알림 설정 목록
     */
    List<NotificationSetting> findByUserIds(List<Long> userIds);

    /**
     * 특정 사용자의 알림 설정을 삭제한다.
     *
     * @param userId 사용자 ID
     */
    void deleteByUserId(Long userId);
}
