package com.cotalk.application.service.auth;

import com.cotalk.domain.entity.User;
import com.cotalk.domain.exception.DuplicateEmailException;
import com.cotalk.domain.exception.DuplicateNicknameException;
import com.cotalk.domain.port.inbound.auth.SignUpUseCase;
import com.cotalk.domain.port.outbound.IdGenerator;
import com.cotalk.domain.port.outbound.UserRepository;
import com.cotalk.domain.validator.UserValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원가입 유스케이스 구현체.
 * 이메일, 비밀번호, 닉네임을 검증하고 새로운 사용자를 생성한다.
 *
 * @author seunggu.lee
 */
@Service
@RequiredArgsConstructor
@Transactional
public class SignUpService implements SignUpUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final IdGenerator idGenerator;
    private final UserValidator userValidator;

    /**
     * 새로운 사용자를 등록한다.
     *
     * @param email 사용자 이메일
     * @param password 비밀번호
     * @param nickname 닉네임
     * @return 생성된 사용자의 ID
     * @throws DuplicateEmailException 이메일이 이미 사용 중인 경우
     * @throws DuplicateNicknameException 닉네임이 이미 사용 중인 경우
     */
    @Override
    public Long signUp(String email, String password, String nickname) {
        userValidator.validateEmail(email);
        userValidator.validatePassword(password);
        userValidator.validateNickname(nickname);

        checkEmailNotExists(email);
        checkNicknameNotExists(nickname);

        String passwordHash = passwordEncoder.encode(password);

        User user = User.builder()
                .id(idGenerator.nextId())
                .email(email)
                .passwordHash(passwordHash)
                .nickname(nickname)
                .build();

        User savedUser = userRepository.save(user);
        return savedUser.getId();
    }

    private void checkEmailNotExists(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateEmailException();
        }
    }

    private void checkNicknameNotExists(String nickname) {
        if (userRepository.existsByNickname(nickname)) {
            throw new DuplicateNicknameException();
        }
    }
}
