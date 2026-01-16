package com.cotalk.application.service.user;

import com.cotalk.domain.entity.User;
import com.cotalk.domain.exception.DomainException;
import com.cotalk.domain.exception.UserNotFoundException;
import com.cotalk.domain.port.inbound.user.UpdateUserProfileUseCase;
import com.cotalk.domain.port.outbound.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 사용자 프로필 수정 Use Case 구현체.
 *
 * @author Co-Talk Team
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
@Transactional
public class UpdateUserProfileService implements UpdateUserProfileUseCase {

    private final UserRepository userRepository;

    @Override
    public User updateProfile(UUID userId, String nickname, String avatarUrl) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // 닉네임 업데이트
        if (nickname != null) {
            // 닉네임 중복 확인
            if (userRepository.existsByNickname(nickname) && 
                !user.getNickname().equals(nickname)) {
                throw new DomainException("이미 사용 중인 닉네임입니다.");
            }
            user.updateNickname(nickname);
        }

        // 프로필 사진 URL 업데이트
        if (avatarUrl != null) {
            user.updateAvatarUrl(avatarUrl);
        }

        return userRepository.save(user);
    }
}
