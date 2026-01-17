package com.cotalk.adapter.outbound.persistence.notification;

import com.cotalk.domain.entity.DeviceToken;
import com.cotalk.domain.port.outbound.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 디바이스 토큰 영속성 어댑터.
 * JPA를 통해 디바이스 토큰 데이터를 저장하고 조회한다.
 *
 * @author seunggu.lee
 */
@Repository
@RequiredArgsConstructor
public class DeviceTokenRepositoryAdapter implements DeviceTokenRepository {

    private final DeviceTokenJpaRepository deviceTokenJpaRepository;

    /**
     * 디바이스 토큰을 저장한다.
     *
     * @param deviceToken 저장할 디바이스 토큰 엔티티
     * @return 저장된 디바이스 토큰 엔티티
     */
    @Override
    public DeviceToken save(DeviceToken deviceToken) {
        return deviceTokenJpaRepository.save(deviceToken);
    }

    /**
     * ID로 디바이스 토큰을 조회한다.
     *
     * @param id 디바이스 토큰 ID
     * @return 디바이스 토큰 (Optional)
     */
    @Override
    public Optional<DeviceToken> findById(Long id) {
        return deviceTokenJpaRepository.findById(id);
    }

    /**
     * 토큰 값으로 디바이스 토큰을 조회한다.
     *
     * @param token 토큰 값
     * @return 디바이스 토큰 (Optional)
     */
    @Override
    public Optional<DeviceToken> findByToken(String token) {
        return deviceTokenJpaRepository.findByToken(token);
    }

    /**
     * 사용자 ID로 디바이스 토큰 목록을 조회한다.
     *
     * @param userId 사용자 ID
     * @return 디바이스 토큰 목록
     */
    @Override
    public List<DeviceToken> findByUserId(Long userId) {
        return deviceTokenJpaRepository.findByUserId(userId);
    }

    /**
     * 사용자 ID로 활성화된 디바이스 토큰 목록을 조회한다.
     *
     * @param userId 사용자 ID
     * @return 활성화된 디바이스 토큰 목록
     */
    @Override
    public List<DeviceToken> findActiveByUserId(Long userId) {
        return deviceTokenJpaRepository.findActiveByUserId(userId);
    }

    /**
     * 여러 사용자 ID로 활성화된 디바이스 토큰 목록을 조회한다.
     *
     * @param userIds 사용자 ID 목록
     * @return 활성화된 디바이스 토큰 목록
     */
    @Override
    public List<DeviceToken> findActiveByUserIds(List<Long> userIds) {
        return deviceTokenJpaRepository.findActiveByUserIdIn(userIds);
    }

    /**
     * 토큰 값으로 디바이스 토큰을 삭제한다.
     *
     * @param token 토큰 값
     */
    @Override
    public void deleteByToken(String token) {
        deviceTokenJpaRepository.deleteByToken(token);
    }

    /**
     * 사용자 ID로 모든 디바이스 토큰을 삭제한다.
     *
     * @param userId 사용자 ID
     */
    @Override
    public void deleteByUserId(Long userId) {
        deviceTokenJpaRepository.deleteByUserId(userId);
    }
}
