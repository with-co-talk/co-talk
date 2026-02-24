package com.cotalk.application.service.auth;

import com.cotalk.domain.entity.EmailVerificationToken;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.exception.InvalidEmailVerificationTokenException;
import com.cotalk.domain.exception.UserNotFoundException;
import com.cotalk.domain.port.inbound.auth.VerifyEmailUseCase;
import com.cotalk.domain.port.outbound.EmailVerificationTokenRepository;
import com.cotalk.domain.port.outbound.TimeProvider;
import com.cotalk.domain.port.outbound.UserRepository;
import com.cotalk.domain.util.LogMaskingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 이메일 인증 유스케이스 구현체.
 * 토큰을 검증하고 사용자의 이메일 인증 상태를 업데이트한다.
 *
 * @author seunggu.lee
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class VerifyEmailService implements VerifyEmailUseCase {

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final TimeProvider timeProvider;

    /**
     * 토큰을 사용하여 이메일 인증을 완료한다.
     *
     * @param token 이메일 인증 토큰
     * @throws InvalidEmailVerificationTokenException 토큰이 유효하지 않은 경우
     * @throws UserNotFoundException                  사용자를 찾을 수 없는 경우
     */
    @Override
    public void verifyEmail(String token) {
        EmailVerificationToken verificationToken = tokenRepository.findByToken(token)
                .orElseThrow(InvalidEmailVerificationTokenException::notFound);

        if (verificationToken.isExpired(timeProvider.now())) {
            throw InvalidEmailVerificationTokenException.expired();
        }
        if (verificationToken.isVerified()) {
            throw InvalidEmailVerificationTokenException.alreadyVerified();
        }

        User user = userRepository.findById(verificationToken.getUserId())
                .orElseThrow(() -> new UserNotFoundException(verificationToken.getUserId()));

        user.verifyEmail();
        userRepository.save(user);

        verificationToken.markAsVerified(timeProvider.now());
        tokenRepository.save(verificationToken);

        log.info("Email verified for user: {}", LogMaskingUtil.maskEmail(verificationToken.getEmail().value()));
    }
}
