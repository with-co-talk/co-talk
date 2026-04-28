package com.cotalk.application.service.auth;

import com.cotalk.domain.entity.EmailVerificationToken;
import com.cotalk.domain.exception.RateLimitExceededException;
import com.cotalk.domain.port.inbound.auth.ResendVerificationUseCase;
import com.cotalk.domain.port.outbound.EmailSender;
import com.cotalk.domain.port.outbound.EmailVerificationTokenRepository;
import com.cotalk.domain.port.outbound.TimeProvider;
import com.cotalk.domain.port.outbound.UserRepository;
import com.cotalk.domain.util.LogMaskingUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


/**
 * 인증 이메일 재발송 유스케이스 구현체.
 * 이메일 인증이 완료되지 않은 사용자에게 인증 이메일을 재발송한다.
 *
 * @author seunggu.lee
 */
@Slf4j
@Service
@Transactional
public class ResendVerificationService implements ResendVerificationUseCase {

    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository tokenRepository;
    private final EmailSender emailSender;
    private final TimeProvider timeProvider;
    private final String frontendUrl;
    private final String publicServerUrl;
    private final String suppressedEmailDomains;

    private static final long RESEND_COOLDOWN_SECONDS = 60;

    /**
     * ResendVerificationService 생성자.
     * 프론트엔드 URL은 application.yml의 app.frontend-url 속성에서 주입된다.
     *
     * @param userRepository 사용자 저장소
     * @param tokenRepository 이메일 인증 토큰 저장소
     * @param emailSender 이메일 발송 포트
     * @param timeProvider 시간 제공자
     * @param frontendUrl 프론트엔드 URL
     */
    public ResendVerificationService(
            UserRepository userRepository,
            EmailVerificationTokenRepository tokenRepository,
            EmailSender emailSender,
            TimeProvider timeProvider,
            @Value("${app.frontend-url}") String frontendUrl,
            @Value("${app.swagger.server-url}") String publicServerUrl,
            @Value("${app.email.suppressed-domains:}") String suppressedEmailDomains) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.emailSender = emailSender;
        this.timeProvider = timeProvider;
        this.frontendUrl = frontendUrl;
        this.publicServerUrl = publicServerUrl;
        this.suppressedEmailDomains = suppressedEmailDomains;
    }

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
                        && latest.getCreatedAt().plusSeconds(RESEND_COOLDOWN_SECONDS).isAfter(timeProvider.now())) {
                    throw RateLimitExceededException.tooManyRequests(RESEND_COOLDOWN_SECONDS);
                }
            });

            // 기존 토큰 삭제 후 새로 생성
            tokenRepository.deleteByUserId(user.getId());
            EmailVerificationToken token = EmailVerificationToken.create(
                    user.getId(), user.getEmail(), 1440, timeProvider.now()); // 24시간
            tokenRepository.save(token);

            if (isEmailSuppressed(user.getEmail().value())) {
                log.info("Verification email resend suppressed for test/domain policy: {}",
                        LogMaskingUtil.maskEmail(email));
            } else {
                String verificationLink = buildVerificationLink(token.getToken());
                emailSender.sendVerificationEmail(user.getEmail().value(), verificationLink);

                log.info("Verification email resent to: {}", LogMaskingUtil.maskEmail(email));
            }
        });
    }

    private String buildVerificationLink(String token) {
        if (frontendUrl == null || frontendUrl.isBlank() || frontendUrl.contains("localhost")) {
            log.warn("frontendUrl is not publicly reachable. Falling back to API verification endpoint.");
            return publicServerUrl + "/api/v1/auth/verify-email?token=" + token;
        }
        return frontendUrl + "/verify-email?token=" + token;
    }

    private boolean isEmailSuppressed(String email) {
        if (email == null || suppressedEmailDomains == null || suppressedEmailDomains.isBlank()) {
            return false;
        }
        int atIndex = email.lastIndexOf('@');
        if (atIndex < 0 || atIndex == email.length() - 1) {
            return false;
        }
        String domain = email.substring(atIndex + 1).trim().toLowerCase();
        for (String configuredDomain : suppressedEmailDomains.split(",")) {
            if (domain.equals(configuredDomain.trim().toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}
