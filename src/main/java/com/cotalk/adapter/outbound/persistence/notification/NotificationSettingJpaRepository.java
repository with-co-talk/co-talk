package com.cotalk.adapter.outbound.persistence.notification;

import com.cotalk.adapter.outbound.persistence.entity.NotificationSettingJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 알림 설정 JPA 리포지토리.
 * persistence 계층 전용이며, 도메인 반환은 Adapter에서 매핑한다.
 *
 * @author seunggu.lee
 */
public interface NotificationSettingJpaRepository extends JpaRepository<NotificationSettingJpaEntity, Long> {

    /**
     * 사용자 ID로 알림 설정을 조회한다.
     *
     * @param userId 사용자 ID
     * @return 알림 설정 (Optional)
     */
    Optional<NotificationSettingJpaEntity> findByUserId(Long userId);

    /**
     * 여러 사용자 ID로 알림 설정을 일괄 조회한다.
     *
     * @param userIds 사용자 ID 목록
     * @return 조회된 알림 설정 목록
     */
    List<NotificationSettingJpaEntity> findByUserIdIn(List<Long> userIds);

    /**
     * 사용자 ID로 알림 설정을 삭제한다.
     *
     * @param userId 사용자 ID
     */
    void deleteByUserId(Long userId);
}
