package com.cotalk.domain.port.inbound.user;

/**
 * 프로필 수정 유스케이스.
 * 사용자의 프로필 정보를 수정한다.
 *
 * @author seunggu.lee
 */
public interface UpdateProfileUseCase {

    /**
     * 사용자의 프로필 정보를 수정한다.
     *
     * @param userId        사용자 ID
     * @param nickname      새로운 닉네임
     * @param statusMessage 새로운 상태메시지
     * @param avatarUrl     새로운 프로필 이미지 URL
     */
    void updateProfile(Long userId, String nickname, String statusMessage, String avatarUrl);
}
