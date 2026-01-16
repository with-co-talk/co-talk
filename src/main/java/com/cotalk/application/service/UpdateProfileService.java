package com.cotalk.application.service;

import com.cotalk.domain.entity.User;
import com.cotalk.domain.exception.DomainException;
import com.cotalk.domain.port.inbound.UpdateProfileUseCase;
import com.cotalk.domain.port.outbound.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UpdateProfileService implements UpdateProfileUseCase {

    private final UserRepository userRepository;

    @Override
    public void updateProfile(Long userId, String nickname, String avatarUrl) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new DomainException("사용자를 찾을 수 없습니다."));

        if (nickname != null) {
            user.updateNickname(nickname);
        }
        if (avatarUrl != null) {
            user.updateAvatarUrl(avatarUrl);
        }

        userRepository.save(user);
    }
}
