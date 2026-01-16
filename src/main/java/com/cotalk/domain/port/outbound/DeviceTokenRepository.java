package com.cotalk.domain.port.outbound;

import com.cotalk.domain.entity.DeviceToken;

import java.util.List;
import java.util.Optional;

public interface DeviceTokenRepository {
    
    DeviceToken save(DeviceToken deviceToken);
    
    Optional<DeviceToken> findById(Long id);
    
    Optional<DeviceToken> findByToken(String token);
    
    List<DeviceToken> findByUserId(Long userId);
    
    List<DeviceToken> findActiveByUserId(Long userId);
    
    List<DeviceToken> findActiveByUserIds(List<Long> userIds);
    
    void deleteByToken(String token);
    
    void deleteByUserId(Long userId);
}
