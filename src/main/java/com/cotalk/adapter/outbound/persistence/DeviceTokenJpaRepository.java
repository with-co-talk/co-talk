package com.cotalk.adapter.outbound.persistence;

import com.cotalk.domain.entity.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DeviceTokenJpaRepository extends JpaRepository<DeviceToken, Long> {
    
    Optional<DeviceToken> findByToken(String token);
    
    List<DeviceToken> findByUserId(Long userId);
    
    @Query("SELECT dt FROM DeviceToken dt WHERE dt.userId = :userId AND dt.active = true")
    List<DeviceToken> findActiveByUserId(@Param("userId") Long userId);
    
    @Query("SELECT dt FROM DeviceToken dt WHERE dt.userId IN :userIds AND dt.active = true")
    List<DeviceToken> findActiveByUserIdIn(@Param("userIds") List<Long> userIds);
    
    void deleteByToken(String token);
    
    void deleteByUserId(Long userId);
}
