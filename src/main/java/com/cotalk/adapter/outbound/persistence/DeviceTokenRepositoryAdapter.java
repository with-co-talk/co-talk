package com.cotalk.adapter.outbound.persistence;

import com.cotalk.domain.entity.DeviceToken;
import com.cotalk.domain.port.outbound.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class DeviceTokenRepositoryAdapter implements DeviceTokenRepository {

    private final DeviceTokenJpaRepository deviceTokenJpaRepository;

    @Override
    public DeviceToken save(DeviceToken deviceToken) {
        return deviceTokenJpaRepository.save(deviceToken);
    }

    @Override
    public Optional<DeviceToken> findById(Long id) {
        return deviceTokenJpaRepository.findById(id);
    }

    @Override
    public Optional<DeviceToken> findByToken(String token) {
        return deviceTokenJpaRepository.findByToken(token);
    }

    @Override
    public List<DeviceToken> findByUserId(Long userId) {
        return deviceTokenJpaRepository.findByUserId(userId);
    }

    @Override
    public List<DeviceToken> findActiveByUserId(Long userId) {
        return deviceTokenJpaRepository.findActiveByUserId(userId);
    }

    @Override
    public List<DeviceToken> findActiveByUserIds(List<Long> userIds) {
        return deviceTokenJpaRepository.findActiveByUserIdIn(userIds);
    }

    @Override
    public void deleteByToken(String token) {
        deviceTokenJpaRepository.deleteByToken(token);
    }

    @Override
    public void deleteByUserId(Long userId) {
        deviceTokenJpaRepository.deleteByUserId(userId);
    }
}
