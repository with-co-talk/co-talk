package com.cotalk.domain.port.inbound;

public interface UpdateProfileUseCase {
    void updateProfile(Long userId, String nickname, String avatarUrl);
}
