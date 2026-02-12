package com.cotalk.application.service.auth;

import com.cotalk.domain.entity.EmailVerificationToken;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.exception.DuplicateEmailException;
import com.cotalk.domain.exception.DuplicateNicknameException;
import com.cotalk.domain.port.inbound.auth.SignUpUseCase;
import com.cotalk.domain.port.outbound.EmailSender;
import com.cotalk.domain.port.outbound.EmailVerificationTokenRepository;
import com.cotalk.domain.port.outbound.IdGenerator;
import com.cotalk.domain.port.outbound.PasswordEncoderPort;
import com.cotalk.domain.port.outbound.UserRepository;
import com.cotalk.domain.validator.UserValidator;
import com.cotalk.infrastructure.config.properties.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원가입 유스케이스 구현체.
 * 이메일, 비밀번호, 닉네임을 검증하고 새로운 사용자를 생성한다.
 *
 * @author seunggu.lee
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SignUpService implements SignUpUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoderPort passwordEncoder;
    private final IdGenerator idGenerator;
    private final UserValidator userValidator;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final EmailSender emailSender;
    private final AppProperties appProperties;

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
        log.debug("Sign-up attempt: email={}, nickname={}", maskEmail(email), nickname);

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
                .emailVerified(false)
                .build();

        User savedUser = userRepository.save(user);

        // 이메일 인증 토큰 생성 및 발송
        EmailVerificationToken verificationToken = EmailVerificationToken.create(
                savedUser.getId(), email, 1440); // 24시간
        emailVerificationTokenRepository.save(verificationToken);

        String verificationLink = appProperties.frontendUrl() + "/verify-email?token=" + verificationToken.getToken();
        emailSender.sendVerificationEmail(email, verificationLink);

        log.info("Sign-up successful: userId={}, email={}", savedUser.getId(), maskEmail(email));
        return savedUser.getId();
    }

    @Override
    public Long signUp(String email, String password, String nickname, String phoneNumber) {
        Long userId = signUp(email, password, nickname);

        if (phoneNumber != null && !phoneNumber.isBlank()) {
            userRepository.findById(userId).ifPresent(user -> {
                user.updatePhoneNumber(phoneNumber);
                userRepository.save(user);
            });
        }

        return userId;
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

    /**
     * 이메일 마스킹 처리 (로그 보안)
     *
     * @param email 원본 이메일
     * @return 마스킹된 이메일 (예: te**@example.com)
     */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        String[] parts = email.split("@");
        String localPart = parts[0];
        if (localPart.length() <= 2) {
            return "**@" + parts[1];
        }
        return localPart.substring(0, 2) + "**@" + parts[1];
    }
}
