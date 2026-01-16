package com.cotalk.application.service;

import com.cotalk.domain.entity.User;
import com.cotalk.domain.exception.InvalidCredentialsException;
import com.cotalk.domain.port.inbound.LoginUseCase;
import com.cotalk.domain.port.inbound.UpdateUserOnlineStatusUseCase;
import com.cotalk.domain.port.outbound.UserRepository;
import com.cotalk.infrastructure.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LoginService implements LoginUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final UpdateUserOnlineStatusUseCase updateUserOnlineStatusUseCase;

    @Override
    @Transactional
    public String login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        // 로그인 시 온라인 상태로 변경 및 마지막 접속 시간 업데이트
        updateUserOnlineStatusUseCase.setOnline(user.getId());

        return jwtTokenProvider.generateToken(user.getId());
    }
}
