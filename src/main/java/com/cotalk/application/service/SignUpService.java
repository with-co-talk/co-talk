package com.cotalk.application.service;

import com.cotalk.domain.entity.User;
import com.cotalk.domain.exception.DuplicateEmailException;
import com.cotalk.domain.exception.DuplicateNicknameException;
import com.cotalk.domain.port.inbound.SignUpUseCase;
import com.cotalk.domain.port.outbound.UserRepository;
import com.cotalk.domain.validator.UserValidator;
import com.cotalk.infrastructure.id.SnowflakeIdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class SignUpService implements SignUpUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SnowflakeIdGenerator idGenerator;
    private final UserValidator userValidator;

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
