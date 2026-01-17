package com.cotalk.domain.port.outbound;

import com.cotalk.domain.entity.DeviceToken;

import java.util.List;
import java.util.Optional;

/**
 * 디바이스 토큰 레포지토리 포트.
 * 푸시 알림을 위한 디바이스 토큰 데이터 저장 및 조회를 위한 인터페이스를 정의한다.
 *
 * @author seunggu.lee
 */
public interface DeviceTokenRepository {

    /**
     * 디바이스 토큰을 저장한다.
     *
     * @param deviceToken 저장할 디바이스 토큰
     * @return 저장된 디바이스 토큰
     */
    DeviceToken save(DeviceToken deviceToken);

    /**
     * ID로 디바이스 토큰을 조회한다.
     *
     * @param id 디바이스 토큰 ID
     * @return 조회된 디바이스 토큰 (Optional)
     */
    Optional<DeviceToken> findById(Long id);

    /**
     * 토큰 문자열로 디바이스 토큰을 조회한다.
     *
     * @param token 토큰 문자열
     * @return 조회된 디바이스 토큰 (Optional)
     */
    Optional<DeviceToken> findByToken(String token);

    /**
     * 사용자의 모든 디바이스 토큰을 조회한다.
     *
     * @param userId 사용자 ID
     * @return 디바이스 토큰 목록
     */
    List<DeviceToken> findByUserId(Long userId);

    /**
     * 사용자의 활성화된 디바이스 토큰을 조회한다.
     *
     * @param userId 사용자 ID
     * @return 활성화된 디바이스 토큰 목록
     */
    List<DeviceToken> findActiveByUserId(Long userId);

    /**
     * 여러 사용자의 활성화된 디바이스 토큰을 조회한다.
     *
     * @param userIds 사용자 ID 목록
     * @return 활성화된 디바이스 토큰 목록
     */
    List<DeviceToken> findActiveByUserIds(List<Long> userIds);

    /**
     * 토큰 문자열로 디바이스 토큰을 삭제한다.
     *
     * @param token 삭제할 토큰 문자열
     */
    void deleteByToken(String token);

    /**
     * 특정 사용자의 모든 디바이스 토큰을 삭제한다.
     *
     * @param userId 사용자 ID
     */
    void deleteByUserId(Long userId);
}
