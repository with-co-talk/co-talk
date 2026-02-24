package com.cotalk.infrastructure.email;

import com.cotalk.domain.port.outbound.EmailSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * 콘솔 출력 기반 이메일 발송 폴백 구현체.
 * {@link EmailSender} 포트를 구현하여 실제 이메일을 발송하지 않고 콘솔에 출력한다.
 *
 * <p>{@link SmtpEmailSender}가 등록되지 않은 경우 모든 환경(로컬·개발·프로덕션 포함)에서
 * 자동으로 활성화된다. SMTP 설정({@code spring.mail.host})이 없으면 {@link SmtpEmailSender}가
 * 비활성화되므로, 이 빈이 안전한 폴백으로 동작하여 NoSuchBeanDefinitionException을 방지한다.
 *
 * @author seunggu.lee
 * @see EmailSender
 * @see SmtpEmailSender
 */
@Slf4j
@Component
@ConditionalOnMissingBean(SmtpEmailSender.class)
public class ConsoleEmailSender implements EmailSender {

    /**
     * 이메일 내용을 콘솔에 출력한다.
     *
     * @param to      수신자 이메일 주소
     * @param subject 이메일 제목
     * @param body    이메일 본문
     */
    @Override
    public void send(String to, String subject, String body) {
        log.info("=== Console Email ===");
        log.info("To: {}", to);
        log.info("Subject: {}", subject);
        log.info("Body: {}", body);
        log.info("=====================");
    }

    /**
     * 비밀번호 재설정 이메일 내용을 콘솔에 출력한다.
     *
     * @param to        수신자 이메일 주소
     * @param resetLink 비밀번호 재설정 링크 URL
     */
    @Override
    public void sendPasswordResetEmail(String to, String resetLink) {
        log.info("=== Password Reset Email ===");
        log.info("To: {}", to);
        log.info("Reset Link: {}", resetLink);
        log.info("============================");
    }

    /**
     * 이메일 인증 이메일 내용을 콘솔에 출력한다.
     *
     * @param to               수신자 이메일 주소
     * @param verificationLink 이메일 인증 링크 URL
     */
    @Override
    public void sendVerificationEmail(String to, String verificationLink) {
        log.info("=== Verification Email ===");
        log.info("To: {}", to);
        log.info("Verification Link: {}", verificationLink);
        log.info("==========================");
    }

    @Override
    public void sendPasswordResetCode(String to, String code) {
        log.info("=== Password Reset Code ===");
        log.info("To: {}", to);
        log.info("Code: {}", code);
        log.info("===========================");
    }
}
