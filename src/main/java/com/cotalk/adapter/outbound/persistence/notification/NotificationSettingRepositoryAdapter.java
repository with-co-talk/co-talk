package com.cotalk.adapter.outbound.persistence.notification;

import com.cotalk.domain.entity.NotificationSetting;
import com.cotalk.domain.port.outbound.NotificationSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 알림 설정 영속성 어댑터.
 * JPA를 통해 알림 설정 데이터를 저장하고 조회한다.
 *
 * @author seunggu.lee
 */
@Repository
@RequiredArgsConstructor
public class NotificationSettingRepositoryAdapter implements NotificationSettingRepository {

    private final NotificationSettingJpaRepository notificationSettingJpaRepository;

    /**
     * 알림 설정을 저장한다.
     *
     * @param setting 저장할 알림 설정 엔티티
     * @return 저장된 알림 설정 엔티티
     */
    @Override
    public NotificationSetting save(NotificationSetting setting) {
        return notificationSettingJpaRepository.save(setting);
    }

    /**
     * 사용자 ID로 알림 설정을 조회한다.
     *
     * @param userId 사용자 ID
     * @return 알림 설정 (Optional)
     */
    @Override
    public Optional<NotificationSetting> findByUserId(Long userId) {
        return notificationSettingJpaRepository.findByUserId(userId);
    }
}
