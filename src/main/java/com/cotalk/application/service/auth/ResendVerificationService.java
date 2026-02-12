package com.cotalk.application.service.auth;

import com.cotalk.domain.entity.EmailVerificationToken;
import com.cotalk.domain.exception.RateLimitExceededException;
import com.cotalk.domain.port.inbound.auth.ResendVerificationUseCase;
import com.cotalk.domain.port.outbound.EmailSender;
import com.cotalk.domain.port.outbound.EmailVerificationTokenRepository;
import com.cotalk.domain.port.outbound.UserRepository;
import com.cotalk.infrastructure.config.properties.AppProperties;
import com.cotalk.infrastructure.util.LogMaskingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 인증 이메일 재발송 유스케이스 구현체.
 * 이메일 인증이 완료되지 않은 사용자에게 인증 이메일을 재발송한다.
 *
 * @author seunggu.lee
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ResendVerificationService implements ResendVerificationUseCase {

    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository tokenRepository;
    private final EmailSender emailSender;
    private final AppProperties appProperties;

    private static final long RESEND_COOLDOWN_SECONDS = 60;

    /**
     * 이메일 인증 이메일을 재발송한다.
     * 보안상 이메일 존재 여부와 관계없이 동일한 응답을 반환한다.
     *
     * @param email 수신자 이메일 주소
     * @throws RateLimitExceededException 쿨다운 시간 내 재요청 시
     */
    @Override
    public void resendVerification(String email) {
        // 보안: 이메일 존재 여부와 관계없이 동일 응답
        userRepository.findByEmail(email).ifPresent(user -> {
            if (user.isEmailVerified()) {
                return; // 이미 인증됨, 무시
            }

            // Rate limit: 마지막 토큰 생성 시간 확인
            tokenRepository.findLatestByUserId(user.getId()).ifPresent(latest -> {
                if (latest.getCreatedAt() != null
                        && latest.getCreatedAt().plusSeconds(RESEND_COOLDOWN_SECONDS).isAfter(LocalDateTime.now())) {
                    throw RateLimitExceededException.tooManyRequests(RESEND_COOLDOWN_SECONDS);
                }
            });

            // 기존 토큰 삭제 후 새로 생성
            tokenRepository.deleteByUserId(user.getId());
            EmailVerificationToken token = EmailVerificationToken.create(
                    user.getId(), user.getEmail(), 1440); // 24시간
            tokenRepository.save(token);

            String verificationLink = appProperties.frontendUrl() + "/verify-email?token=" + token.getToken();
            emailSender.sendVerificationEmail(user.getEmail(), verificationLink);

            log.info("Verification email resent to: {}", LogMaskingUtil.maskEmail(email));
        });
    }
}
