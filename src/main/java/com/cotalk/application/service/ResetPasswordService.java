package com.cotalk.application.service;

import com.cotalk.domain.entity.PasswordResetToken;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.exception.InvalidPasswordResetTokenException;
import com.cotalk.domain.exception.UserNotFoundException;
import com.cotalk.domain.port.inbound.ResetPasswordUseCase;
import com.cotalk.domain.port.outbound.PasswordResetTokenRepository;
import com.cotalk.domain.port.outbound.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ResetPasswordService implements ResetPasswordUseCase {

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(InvalidPasswordResetTokenException::notFound);

        validateToken(resetToken);

        User user = userRepository.findById(resetToken.getUserId())
                .orElseThrow(() -> new UserNotFoundException(resetToken.getUserId()));

        // 비밀번호 변경
        String encodedPassword = passwordEncoder.encode(newPassword);
        user.updatePassword(encodedPassword);
        userRepository.save(user);

        // 토큰 사용 처리
        resetToken.markAsUsed();
        tokenRepository.save(resetToken);

        log.info("Password reset completed for user: {}", resetToken.getEmail());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean validateToken(String token) {
        return tokenRepository.findByToken(token)
                .map(PasswordResetToken::isValid)
                .orElse(false);
    }

    private void validateToken(PasswordResetToken token) {
        if (token.isExpired()) {
            throw InvalidPasswordResetTokenException.expired();
        }
        if (token.isUsed()) {
            throw InvalidPasswordResetTokenException.alreadyUsed();
        }
    }
}
