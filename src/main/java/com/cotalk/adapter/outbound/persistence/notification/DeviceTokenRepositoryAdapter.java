package com.cotalk.adapter.outbound.persistence.notification;

import com.cotalk.adapter.outbound.persistence.entity.DeviceTokenJpaEntity;
import com.cotalk.adapter.outbound.persistence.mapper.DeviceTokenMapper;
import com.cotalk.domain.entity.DeviceToken;
import com.cotalk.domain.port.outbound.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 디바이스 토큰 영속성 어댑터.
 * JPA 엔티티와 도메인 간 매핑을 수행하며, 도메인 포트를 구현한다.
 *
 * @author seunggu.lee
 */
@Repository
@RequiredArgsConstructor
public class DeviceTokenRepositoryAdapter implements DeviceTokenRepository {

    private final DeviceTokenJpaRepository deviceTokenJpaRepository;
    private final DeviceTokenMapper mapper;

    /**
     * 디바이스 토큰을 저장한다.
     *
     * @param deviceToken 저장할 디바이스 토큰 엔티티
     * @return 저장된 디바이스 토큰 엔티티
     */
    @Override
    public DeviceToken save(DeviceToken deviceToken) {
        DeviceTokenJpaEntity saved = deviceTokenJpaRepository.save(mapper.toJpa(deviceToken));
        return mapper.toDomain(saved);
    }

    /**
     * 디바이스 토큰을 저장하고 즉시 flush한다.
     *
     * @param deviceToken 저장할 디바이스 토큰 엔티티
     * @return 저장된 디바이스 토큰 엔티티
     */
    @Override
    public DeviceToken saveAndFlush(DeviceToken deviceToken) {
        DeviceTokenJpaEntity saved = deviceTokenJpaRepository.saveAndFlush(mapper.toJpa(deviceToken));
        return mapper.toDomain(saved);
    }

    /**
     * ID로 디바이스 토큰을 조회한다.
     *
     * @param id 디바이스 토큰 ID
     * @return 디바이스 토큰 (Optional)
     */
    @Override
    public Optional<DeviceToken> findById(Long id) {
        return deviceTokenJpaRepository.findById(id).map(mapper::toDomain);
    }

    /**
     * 토큰 값으로 디바이스 토큰을 조회한다.
     *
     * @param token 토큰 값
     * @return 디바이스 토큰 (Optional)
     */
    @Override
    public Optional<DeviceToken> findByToken(String token) {
        return deviceTokenJpaRepository.findByToken(token).map(mapper::toDomain);
    }

    /**
     * 사용자 ID로 디바이스 토큰 목록을 조회한다.
     *
     * @param userId 사용자 ID
     * @return 디바이스 토큰 목록
     */
    @Override
    public List<DeviceToken> findByUserId(Long userId) {
        return deviceTokenJpaRepository.findByUserId(userId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    /**
     * 사용자 ID로 활성화된 디바이스 토큰 목록을 조회한다.
     *
     * @param userId 사용자 ID
     * @return 활성화된 디바이스 토큰 목록
     */
    @Override
    public List<DeviceToken> findActiveByUserId(Long userId) {
        return deviceTokenJpaRepository.findActiveByUserId(userId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    /**
     * 여러 사용자 ID로 활성화된 디바이스 토큰 목록을 조회한다.
     *
     * @param userIds 사용자 ID 목록
     * @return 활성화된 디바이스 토큰 목록
     */
    @Override
    public List<DeviceToken> findActiveByUserIds(List<Long> userIds) {
        return deviceTokenJpaRepository.findActiveByUserIdIn(userIds).stream()
                .map(mapper::toDomain)
                .toList();
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
