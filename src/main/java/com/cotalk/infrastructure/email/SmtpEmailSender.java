package com.cotalk.infrastructure.email;

import com.cotalk.domain.port.outbound.EmailSender;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.mail.host")
public class SmtpEmailSender implements EmailSender {

    private final JavaMailSender mailSender;

    @Override
    @Async
    public void send(String to, String subject, String body) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true); // HTML 지원

            mailSender.send(message);
            log.info("Email sent successfully to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send email to: {}", to, e);
            throw new RuntimeException("이메일 발송에 실패했습니다.", e);
        }
    }

    @Override
    @Async
    public void sendPasswordResetEmail(String to, String resetLink) {
        String subject = "[Co-Talk] 비밀번호 재설정";
        String body = buildPasswordResetEmailBody(resetLink);
        send(to, subject, body);
    }

    private String buildPasswordResetEmailBody(String resetLink) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: 'Apple SD Gothic Neo', sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: #4F46E5; color: white; padding: 20px; text-align: center; border-radius: 8px 8px 0 0; }
                    .content { background: #f9f9f9; padding: 30px; border-radius: 0 0 8px 8px; }
                    .button { display: inline-block; background: #4F46E5; color: white; padding: 12px 30px; 
                              text-decoration: none; border-radius: 6px; margin: 20px 0; }
                    .footer { text-align: center; color: #666; font-size: 12px; margin-top: 20px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🔐 비밀번호 재설정</h1>
                    </div>
                    <div class="content">
                        <p>안녕하세요,</p>
                        <p>Co-Talk 비밀번호 재설정을 요청하셨습니다.</p>
                        <p>아래 버튼을 클릭하여 새 비밀번호를 설정해주세요:</p>
                        <p style="text-align: center;">
                            <a href="%s" class="button">비밀번호 재설정</a>
                        </p>
                        <p><strong>이 링크는 30분 후에 만료됩니다.</strong></p>
                        <p>만약 비밀번호 재설정을 요청하지 않으셨다면, 이 이메일을 무시하셔도 됩니다.</p>
                    </div>
                    <div class="footer">
                        <p>© 2026 Co-Talk. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(resetLink);
    }
}
