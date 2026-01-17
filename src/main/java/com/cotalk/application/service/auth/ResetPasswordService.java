package com.cotalk.application.service.auth;

import com.cotalk.domain.entity.PasswordResetToken;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.exception.InvalidPasswordResetTokenException;
import com.cotalk.domain.exception.UserNotFoundException;
import com.cotalk.domain.port.inbound.auth.ResetPasswordUseCase;
import com.cotalk.domain.port.outbound.PasswordResetTokenRepository;
import com.cotalk.domain.port.outbound.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 비밀번호 재설정 유스케이스 구현체.
 * 토큰을 검증하고 새로운 비밀번호로 변경한다.
 *
 * @author seunggu.lee
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ResetPasswordService implements ResetPasswordUseCase {

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 토큰을 사용하여 비밀번호를 재설정한다.
     *
     * @param token 비밀번호 재설정 토큰
     * @param newPassword 새로운 비밀번호
     * @throws InvalidPasswordResetTokenException 토큰이 유효하지 않거나 만료된 경우
     * @throws UserNotFoundException 사용자를 찾을 수 없는 경우
     */
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

    /**
     * 비밀번호 재설정 토큰의 유효성을 검증한다.
     *
     * @param token 검증할 토큰
     * @return 토큰이 유효하면 true, 그렇지 않으면 false
     */
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
