package com.cotalk.application.service.auth;

import com.cotalk.domain.entity.User;
import com.cotalk.domain.exception.InvalidCredentialsException;
import com.cotalk.domain.exception.UserNotFoundException;
import com.cotalk.domain.port.inbound.auth.LoginResult;
import com.cotalk.domain.port.inbound.auth.LoginUseCase;
import com.cotalk.domain.port.inbound.user.UpdateUserOnlineStatusUseCase;
import com.cotalk.domain.port.outbound.AuthTokenPort;
import com.cotalk.domain.port.outbound.PasswordEncoderPort;
import com.cotalk.domain.port.outbound.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 로그인 유스케이스 구현체.
 * 이메일과 비밀번호를 검증하고 JWT 토큰을 발급한다.
 *
 * @author seunggu.lee
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginService implements LoginUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoderPort passwordEncoder;
    private final AuthTokenPort authTokenPort;
    private final UpdateUserOnlineStatusUseCase updateUserOnlineStatusUseCase;

    /**
     * 이메일과 비밀번호로 로그인한다.
     * 인증 성공 시 사용자를 온라인 상태로 변경하고 JWT 토큰을 발급한다.
     *
     * @param email 사용자 이메일
     * @param password 비밀번호
     * @return 로그인 결과 (Access Token과 사용자 ID)
     * @throws InvalidCredentialsException 이메일 또는 비밀번호가 잘못된 경우
     */
    @Override
    @Transactional
    public LoginResult login(String email, String password) {
        log.debug("Login attempt for email: {}", maskEmail(email));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Login failed: user not found for email: {}", maskEmail(email));
                    return new InvalidCredentialsException();
                });

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            log.warn("Login failed: invalid password for userId: {}", user.getId());
            throw new InvalidCredentialsException();
        }

        if (!user.isActive()) {
            log.warn("Login failed: inactive account for userId: {}", user.getId());
            throw new InvalidCredentialsException("계정이 비활성화 또는 정지되었습니다.");
        }

        // 로그인 시 온라인 상태로 변경 및 마지막 접속 시간 업데이트
        updateUserOnlineStatusUseCase.setOnline(user.getId());

        String accessToken = authTokenPort.generateAccessToken(user.getId());
        log.info("Login successful: userId={}", user.getId());
        return new LoginResult(accessToken, user.getId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public Long getUserIdByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(User::getId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + email));
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
