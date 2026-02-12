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

/**
 * SMTP 프로토콜을 사용한 이메일 발송 구현체.
 * {@link EmailSender} 포트를 구현하여 실제 이메일을 발송한다.
 *
 * <p>메일 서버가 설정되었을 때({@code spring.mail.host} 프로퍼티 존재 시) 자동으로 활성화된다.
 * HTML 형식의 이메일을 지원하며, 비동기로 발송하여 응답 지연을 방지한다.
 *
 * @author seunggu.lee
 * @see EmailSender
 * @see ConsoleEmailSender
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.mail.host")
public class SmtpEmailSender implements EmailSender {

    private final JavaMailSender mailSender;

    /**
     * 이메일을 비동기로 발송한다.
     *
     * <p>HTML 형식을 지원하며 UTF-8 인코딩을 사용한다.
     *
     * @param to      수신자 이메일 주소
     * @param subject 이메일 제목
     * @param body    이메일 본문 (HTML 지원)
     * @throws RuntimeException 이메일 발송에 실패한 경우
     */
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

    /**
     * 비밀번호 재설정 이메일을 비동기로 발송한다.
     *
     * <p>미리 정의된 HTML 템플릿을 사용하여 비밀번호 재설정 링크가 포함된 이메일을 발송한다.
     *
     * @param to        수신자 이메일 주소
     * @param resetLink 비밀번호 재설정 링크 URL
     */
    @Override
    @Async
    public void sendPasswordResetEmail(String to, String resetLink) {
        String subject = "[Co-Talk] 비밀번호 재설정";
        String body = buildPasswordResetEmailBody(resetLink);
        send(to, subject, body);
    }

    /**
     * 이메일 인증 이메일을 비동기로 발송한다.
     *
     * <p>미리 정의된 HTML 템플릿을 사용하여 이메일 인증 링크가 포함된 이메일을 발송한다.
     *
     * @param to               수신자 이메일 주소
     * @param verificationLink 이메일 인증 링크 URL
     */
    @Override
    @Async
    public void sendVerificationEmail(String to, String verificationLink) {
        String subject = "[Co-Talk] 이메일 인증";
        String body = buildVerificationEmailBody(verificationLink);
        send(to, subject, body);
    }

    @Override
    @Async
    public void sendPasswordResetCode(String to, String code) {
        String subject = "[Co-Talk] 비밀번호 재설정 인증 코드";
        String body = buildPasswordResetCodeEmailBody(code);
        send(to, subject, body);
    }

    /**
     * 이메일 인증 이메일의 HTML 본문을 생성한다.
     *
     * @param verificationLink 이메일 인증 링크 URL
     * @return 생성된 HTML 본문 문자열
     */
    private String buildVerificationEmailBody(String verificationLink) {
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
                        <h1>Co-Talk 이메일 인증</h1>
                    </div>
                    <div class="content">
                        <p>안녕하세요,</p>
                        <p>Co-Talk 회원가입을 환영합니다!</p>
                        <p>아래 버튼을 클릭하여 이메일 인증을 완료해주세요:</p>
                        <p style="text-align: center;">
                            <a href="%s" class="button">이메일 인증하기</a>
                        </p>
                        <p><strong>이 링크는 24시간 후에 만료됩니다.</strong></p>
                        <p>만약 회원가입을 요청하지 않으셨다면, 이 이메일을 무시하셔도 됩니다.</p>
                    </div>
                    <div class="footer">
                        <p>&copy; 2026 Co-Talk. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(verificationLink);
    }

    /**
     * 비밀번호 재설정 이메일의 HTML 본문을 생성한다.
     *
     * @param resetLink 비밀번호 재설정 링크 URL
     * @return 생성된 HTML 본문 문자열
     */
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

    private String buildPasswordResetCodeEmailBody(String code) {
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
                    .code { font-size: 36px; font-weight: bold; letter-spacing: 8px; color: #4F46E5;
                            text-align: center; padding: 20px; background: white; border-radius: 8px;
                            border: 2px dashed #4F46E5; margin: 20px 0; }
                    .footer { text-align: center; color: #666; font-size: 12px; margin-top: 20px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>비밀번호 재설정</h1>
                    </div>
                    <div class="content">
                        <p>안녕하세요,</p>
                        <p>Co-Talk 비밀번호 재설정을 위한 인증 코드입니다.</p>
                        <div class="code">%s</div>
                        <p><strong>이 코드는 30분 후에 만료됩니다.</strong></p>
                        <p>만약 비밀번호 재설정을 요청하지 않으셨다면, 이 이메일을 무시하셔도 됩니다.</p>
                    </div>
                    <div class="footer">
                        <p>&copy; 2026 Co-Talk. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(code);
    }
}
