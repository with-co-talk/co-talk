package com.cotalk.application.service.auth;

import com.cotalk.domain.entity.User;
import com.cotalk.domain.exception.InvalidCredentialsException;
import com.cotalk.domain.exception.UserNotFoundException;
import com.cotalk.domain.port.inbound.auth.LoginUseCase;
import com.cotalk.domain.port.inbound.user.UpdateUserOnlineStatusUseCase;
import com.cotalk.domain.port.outbound.UserRepository;
import com.cotalk.infrastructure.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 로그인 유스케이스 구현체.
 * 이메일과 비밀번호를 검증하고 JWT 토큰을 발급한다.
 *
 * @author seunggu.lee
 */
@Service
@RequiredArgsConstructor
public class LoginService implements LoginUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
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
        User user = userRepository.findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        // 로그인 시 온라인 상태로 변경 및 마지막 접속 시간 업데이트
        updateUserOnlineStatusUseCase.setOnline(user.getId());

        String accessToken = jwtTokenProvider.generateToken(user.getId());
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
}
