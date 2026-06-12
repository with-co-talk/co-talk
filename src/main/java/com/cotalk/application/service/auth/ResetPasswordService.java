package com.cotalk.application.service.auth;

import com.cotalk.domain.entity.PasswordResetToken;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.exception.InvalidPasswordResetTokenException;
import com.cotalk.domain.exception.UserNotFoundException;
import com.cotalk.domain.port.inbound.auth.ResetPasswordUseCase;
import com.cotalk.domain.port.outbound.PasswordEncoderPort;
import com.cotalk.domain.port.outbound.PasswordResetTokenRepository;
import com.cotalk.domain.port.outbound.TimeProvider;
import com.cotalk.domain.port.outbound.UserRepository;
import com.cotalk.domain.util.LogMaskingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

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

    /**
     * 비밀번호 정규식 패턴.
     * 최소 8자, 대문자 1개, 소문자 1개, 숫자 1개, 특수문자 1개 이상 필요.
     */
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#^()\\-_=+])[A-Za-z\\d@$!%*?&#^()\\-_=+]{8,128}$"
    );

    /**
     * 인증 코드 최대 실패 허용 횟수.
     * 초과 시 토큰이 무효화되며 새로운 코드를 요청해야 한다.
     */
    private static final int MAX_VERIFY_ATTEMPTS = 5;

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoderPort passwordEncoder;
    private final TimeProvider timeProvider;

    /**
     * 토큰을 사용하여 비밀번호를 재설정한다.
     *
     * @param token 비밀번호 재설정 토큰
     * @param newPassword 새로운 비밀번호
     * @throws InvalidPasswordResetTokenException 토큰이 유효하지 않거나 만료된 경우
     * @throws UserNotFoundException 사용자를 찾을 수 없는 경우
     * @throws IllegalArgumentException 비밀번호가 보안 요구사항을 충족하지 않는 경우
     */
    @Override
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(InvalidPasswordResetTokenException::notFound);

        validateToken(resetToken);
        validatePasswordStrength(newPassword);

        User user = userRepository.findById(resetToken.getUserId())
                .orElseThrow(() -> new UserNotFoundException(resetToken.getUserId()));

        // 비밀번호 변경
        String encodedPassword = passwordEncoder.encode(newPassword);
        user.updatePassword(encodedPassword);
        userRepository.save(user);

        // 토큰 사용 처리
        resetToken.markAsUsed(timeProvider.now());
        tokenRepository.save(resetToken);

        log.info("Password reset completed for user: {}", LogMaskingUtil.maskEmail(resetToken.getEmail().value()));
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
        var now = timeProvider.now();
        return tokenRepository.findByToken(token)
                .map(t -> t.isValid(now))
                .orElse(false);
    }

    private void validateToken(PasswordResetToken token) {
        if (token.isExpired(timeProvider.now())) {
            throw InvalidPasswordResetTokenException.expired();
        }
        if (token.isUsed()) {
            throw InvalidPasswordResetTokenException.alreadyUsed();
        }
    }

    /**
     * 비밀번호 강도를 검증한다.
     * 회원가입 시와 동일한 비밀번호 정책을 적용한다.
     *
     * @param password 검증할 비밀번호
     * @throws IllegalArgumentException 비밀번호가 보안 요구사항을 충족하지 않는 경우
     */
    private void validatePasswordStrength(String password) {
        if (password == null || !PASSWORD_PATTERN.matcher(password).matches()) {
            throw new IllegalArgumentException(
                    "비밀번호는 8-128자이며, 대문자, 소문자, 숫자, 특수문자를 각각 1개 이상 포함해야 합니다.");
        }
    }

    /**
     * 이메일과 인증 코드를 검증한다.
     * 코드가 유효하지 않은 경우 사유별 예외를 던진다.
     *
     * @param email 사용자 이메일
     * @param code  6자리 인증 코드
     * @throws InvalidPasswordResetTokenException 코드가 유효하지 않은 경우
     */
    @Override
    public void verifyCode(String email, String code) {
        PasswordResetToken token = tokenRepository.findLatestActiveByEmail(email)
                .orElseThrow(InvalidPasswordResetTokenException::invalidCode);

        verifyCodeOnToken(token, code);
    }

    /**
     * 토큰에 대해 인증 코드를 검증한다.
     * <p>
     * 잠금 → 만료/사용됨 → 코드 일치 순으로 검사한다.
     * 인증 코드가 일치하지 않으면 실패 횟수를 증가시켜 저장하고,
     * 최대 허용 횟수를 초과하면 토큰을 무효화한다. 이를 통해 6자리 코드에 대한
     * 무차별 대입 공격을 차단한다.
     * </p>
     *
     * @param token 검증 대상 토큰
     * @param code  사용자가 입력한 인증 코드
     * @throws InvalidPasswordResetTokenException 코드가 유효하지 않은 경우
     */
    private void verifyCodeOnToken(PasswordResetToken token, String code) {
        if (token.isMaxAttemptsExceeded(MAX_VERIFY_ATTEMPTS)) {
            throw InvalidPasswordResetTokenException.maxAttemptsExceeded();
        }

        if (token.isExpired(timeProvider.now())) {
            throw InvalidPasswordResetTokenException.expired();
        }
        if (token.isUsed()) {
            throw InvalidPasswordResetTokenException.alreadyUsed();
        }

        if (!token.matchesCode(code)) {
            token.incrementFailedAttempts();
            tokenRepository.save(token);

            if (token.isMaxAttemptsExceeded(MAX_VERIFY_ATTEMPTS)) {
                throw InvalidPasswordResetTokenException.maxAttemptsExceeded();
            }
            throw InvalidPasswordResetTokenException.invalidCode();
        }
    }

    /**
     * 인증 코드를 사용하여 비밀번호를 재설정한다.
     *
     * @param email       사용자 이메일
     * @param code        6자리 인증 코드
     * @param newPassword 새로운 비밀번호
     * @throws InvalidPasswordResetTokenException 토큰이 없거나 최대 실패 횟수를 초과한 경우
     * @throws UserNotFoundException              사용자를 찾을 수 없는 경우
     * @throws IllegalArgumentException           비밀번호가 보안 요구사항을 충족하지 않는 경우
     */
    @Override
    public void resetPasswordWithCode(String email, String code, String newPassword) {
        PasswordResetToken resetToken = tokenRepository.findLatestActiveByEmail(email)
                .orElseThrow(InvalidPasswordResetTokenException::notFound);

        // 코드 일치 여부를 도메인에서 검증한다. 불일치 시 실패 횟수를 집계하여 무차별 대입을 차단한다.
        verifyCodeOnToken(resetToken, code);

        validateToken(resetToken);
        validatePasswordStrength(newPassword);

        User user = userRepository.findById(resetToken.getUserId())
                .orElseThrow(() -> new UserNotFoundException(resetToken.getUserId()));

        // 비밀번호 변경
        String encodedPassword = passwordEncoder.encode(newPassword);
        user.updatePassword(encodedPassword);
        userRepository.save(user);

        // 토큰 사용 처리
        resetToken.markAsUsed(timeProvider.now());
        tokenRepository.save(resetToken);

        log.info("Password reset with code completed for user: {}", LogMaskingUtil.maskEmail(email));
    }
}
