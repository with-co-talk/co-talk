package com.cotalk.domain.port.inbound;

import com.cotalk.domain.entity.DeviceToken;

/**
 * 디바이스 토큰 등록 유스케이스
 */
public interface RegisterDeviceTokenUseCase {
    
    /**
     * 디바이스 토큰을 등록합니다.
     * 이미 존재하는 토큰이면 업데이트합니다.
     * 
     * @param userId 사용자 ID
     * @param token 디바이스 토큰 (FCM/APNs)
     * @param deviceType 디바이스 타입
     * @return 등록된 DeviceToken
     */
    DeviceToken register(Long userId, String token, DeviceToken.DeviceType deviceType);
    
    /**
     * 디바이스 토큰을 삭제합니다.
     * 로그아웃 시 호출됩니다.
     * 
     * @param token 삭제할 토큰
     */
    void unregister(String token);
}
