package com.cotalk.infrastructure.email;

import com.cotalk.domain.port.outbound.EmailSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * 개발/테스트용 콘솔 이메일 발송 구현체.
 * {@link EmailSender} 포트를 구현하여 실제 이메일을 발송하지 않고 콘솔에 출력한다.
 *
 * <p>{@link SmtpEmailSender}가 등록되지 않은 경우 자동으로 활성화된다.
 * 로컬 개발 환경이나 테스트에서 이메일 서버 없이 이메일 발송 로직을 테스트할 때 유용하다.
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
}
