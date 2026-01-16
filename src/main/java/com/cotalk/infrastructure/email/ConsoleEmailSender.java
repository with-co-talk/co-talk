package com.cotalk.infrastructure.email;

import com.cotalk.domain.port.outbound.EmailSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * 개발/테스트용 콘솔 이메일 발송자
 * 실제 이메일을 발송하지 않고 콘솔에 출력
 */
@Slf4j
@Component
@ConditionalOnMissingBean(SmtpEmailSender.class)
public class ConsoleEmailSender implements EmailSender {

    @Override
    public void send(String to, String subject, String body) {
        log.info("=== Console Email ===");
        log.info("To: {}", to);
        log.info("Subject: {}", subject);
        log.info("Body: {}", body);
        log.info("=====================");
    }

    @Override
    public void sendPasswordResetEmail(String to, String resetLink) {
        log.info("=== Password Reset Email ===");
        log.info("To: {}", to);
        log.info("Reset Link: {}", resetLink);
        log.info("============================");
    }
}
