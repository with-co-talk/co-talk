package com.cotalk.application.service;

import com.cotalk.domain.entity.PasswordResetToken;
import com.cotalk.domain.port.inbound.RequestPasswordResetUseCase;
import com.cotalk.domain.port.outbound.EmailSender;
import com.cotalk.domain.port.outbound.PasswordResetTokenRepository;
import com.cotalk.domain.port.outbound.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class RequestPasswordResetService implements RequestPasswordResetUseCase {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailSender emailSender;
    private final String frontendUrl;
    private final int tokenExpirationMinutes;

    public RequestPasswordResetService(
            UserRepository userRepository,
            PasswordResetTokenRepository tokenRepository,
            EmailSender emailSender,
            @Value("${app.frontend-url:http://localhost:3000}") String frontendUrl,
            @Value("${app.password-reset.expiration-minutes:30}") int tokenExpirationMinutes) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.emailSender = emailSender;
        this.frontendUrl = frontendUrl;
        this.tokenExpirationMinutes = tokenExpirationMinutes;
    }

    @Override
    public void requestPasswordReset(String email) {
        // 보안상 이메일 존재 여부와 관계없이 동일하게 응답
        userRepository.findByEmail(email).ifPresent(user -> {
            // 기존 토큰 삭제
            tokenRepository.deleteByUserId(user.getId());

            // 새 토큰 생성
            PasswordResetToken token = PasswordResetToken.create(
                    user.getId(),
                    email,
                    tokenExpirationMinutes
            );
            tokenRepository.save(token);

            // 이메일 발송
            String resetLink = frontendUrl + "/reset-password?token=" + token.getToken();
            emailSender.sendPasswordResetEmail(email, resetLink);

            log.info("Password reset email sent to: {}", email);
        });
    }
}
