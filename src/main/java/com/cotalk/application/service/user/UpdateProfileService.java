package com.cotalk.application.service.user;

import com.cotalk.domain.entity.User;
import com.cotalk.domain.exception.DomainException;
import com.cotalk.domain.port.inbound.user.UpdateProfileUseCase;
import com.cotalk.domain.port.outbound.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 프로필 수정 유스케이스 구현체.
 * 사용자의 닉네임과 프로필 이미지를 변경한다.
 *
 * @author seunggu.lee
 */
@Service
@RequiredArgsConstructor
@Transactional
public class UpdateProfileService implements UpdateProfileUseCase {

    private final UserRepository userRepository;

    /**
     * 사용자 프로필을 수정한다.
     *
     * @param userId 수정할 사용자 ID
     * @param nickname 새로운 닉네임 (null이면 변경하지 않음)
     * @param avatarUrl 새로운 프로필 이미지 URL (null이면 변경하지 않음)
     * @throws DomainException 사용자를 찾을 수 없는 경우
     */
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
