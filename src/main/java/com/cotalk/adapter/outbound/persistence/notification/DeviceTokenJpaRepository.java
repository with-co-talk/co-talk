package com.cotalk.adapter.outbound.persistence.notification;

import com.cotalk.adapter.outbound.persistence.entity.DeviceTokenJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 디바이스 토큰 JPA 리포지토리.
 * persistence 계층 전용이며, 도메인 반환은 Adapter에서 매핑한다.
 *
 * @author seunggu.lee
 */
public interface DeviceTokenJpaRepository extends JpaRepository<DeviceTokenJpaEntity, Long> {

    /**
     * 토큰 값으로 디바이스 토큰을 조회한다.
     *
     * @param token 토큰 값
     * @return 디바이스 토큰 (Optional)
     */
    Optional<DeviceTokenJpaEntity> findByToken(String token);

    /**
     * 사용자 ID로 디바이스 토큰 목록을 조회한다.
     *
     * @param userId 사용자 ID
     * @return 디바이스 토큰 목록
     */
    List<DeviceTokenJpaEntity> findByUserId(Long userId);

    /**
     * 사용자 ID로 활성화된 디바이스 토큰 목록을 조회한다.
     *
     * @param userId 사용자 ID
     * @return 활성화된 디바이스 토큰 목록
     */
    @Query("SELECT dt FROM DeviceTokenJpaEntity dt WHERE dt.userId = :userId AND dt.active = true")
    List<DeviceTokenJpaEntity> findActiveByUserId(@Param("userId") Long userId);

    /**
     * 여러 사용자 ID로 활성화된 디바이스 토큰 목록을 조회한다.
     *
     * @param userIds 사용자 ID 목록
     * @return 활성화된 디바이스 토큰 목록
     */
    @Query("SELECT dt FROM DeviceTokenJpaEntity dt WHERE dt.userId IN :userIds AND dt.active = true")
    List<DeviceTokenJpaEntity> findActiveByUserIdIn(@Param("userIds") List<Long> userIds);

    /**
     * 토큰 값으로 디바이스 토큰을 삭제한다.
     *
     * @param token 토큰 값
     */
    void deleteByToken(String token);

    /**
     * 사용자 ID로 모든 디바이스 토큰을 삭제한다.
     *
     * @param userId 사용자 ID
     */
    void deleteByUserId(Long userId);
}
