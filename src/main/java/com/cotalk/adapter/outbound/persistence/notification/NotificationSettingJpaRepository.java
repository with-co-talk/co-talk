package com.cotalk.adapter.outbound.persistence.notification;

import com.cotalk.domain.entity.NotificationSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 알림 설정 JPA 리포지토리.
 * Spring Data JPA를 통해 알림 설정 데이터에 접근한다.
 *
 * @author seunggu.lee
 */
public interface NotificationSettingJpaRepository extends JpaRepository<NotificationSetting, Long> {

    /**
     * 사용자 ID로 알림 설정을 조회한다.
     *
     * @param userId 사용자 ID
     * @return 알림 설정 (Optional)
     */
    Optional<NotificationSetting> findByUserId(Long userId);
}
