package com.cotalk.domain.port.inbound.auth;

/**
 * 비밀번호 변경 유스케이스.
 * 인증된 사용자가 현재 비밀번호를 확인 후 새 비밀번호로 변경한다.
 *
 * @author seunggu.lee
 */
public interface ChangePasswordUseCase {

    /**
     * 비밀번호를 변경한다.
     *
     * @param userId 사용자 ID
     * @param currentPassword 현재 비밀번호
     * @param newPassword 새 비밀번호
     */
    void changePassword(Long userId, String currentPassword, String newPassword);
}
