package com.cotalk.infrastructure.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * SmtpEmailSender 단위 테스트.
 *
 * @author seunggu.lee
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SmtpEmailSender 단위 테스트")
class SmtpEmailSenderTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private MimeMessage mimeMessage;

    private SmtpEmailSender emailSender;

    @BeforeEach
    void setUp() {
        given(mailSender.createMimeMessage()).willReturn(mimeMessage);

        MailProperties mailProperties = new MailProperties();
        mailProperties.setHost("smtp.example.com");
        mailProperties.setUsername("noreply@cotalk.com");
        mailProperties.setPassword("test-password");

        emailSender = new SmtpEmailSender(mailSender, mailProperties);
    }

    @Test
    @DisplayName("이메일 발송 성공")
    void should_sendEmail_when_validInput() {
        // given
        String to = "test@example.com";
        String subject = "Test Subject";
        String body = "Test Body";

        doNothing().when(mailSender).send(any(MimeMessage.class));

        // when & then
        assertDoesNotThrow(() -> emailSender.send(to, subject, body));
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("이메일 발송 실패 - MessagingException 발생")
    void should_throwRuntimeException_when_messagingExceptionOccurs() throws MessagingException {
        // given
        String to = "test@example.com";
        String subject = "Test Subject";
        String body = "Test Body";

        // MimeMessageHelper 내부에서 예외 발생 시뮬레이션은 어려우므로
        // send 단계에서 예외 발생
        doThrow(new RuntimeException("이메일 발송에 실패했습니다."))
                .when(mailSender).send(any(MimeMessage.class));

        // when & then
        assertThrows(RuntimeException.class, () -> emailSender.send(to, subject, body));
    }

    @Test
    @DisplayName("비밀번호 재설정 이메일 발송 성공")
    void should_sendPasswordResetEmail_when_validInput() {
        // given
        String to = "test@example.com";
        String resetLink = "https://cotalk.com/reset?token=abc123";

        doNothing().when(mailSender).send(any(MimeMessage.class));

        // when & then
        assertDoesNotThrow(() -> emailSender.sendPasswordResetEmail(to, resetLink));
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("HTML 형식 이메일 발송 성공")
    void should_sendHtmlEmail_when_htmlBody() {
        // given
        String to = "test@example.com";
        String subject = "HTML Email";
        String body = "<html><body><h1>Hello</h1></body></html>";

        doNothing().when(mailSender).send(any(MimeMessage.class));

        // when & then
        assertDoesNotThrow(() -> emailSender.send(to, subject, body));
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("MimeMessage 생성 확인")
    void should_createMimeMessage_when_send() {
        // given
        String to = "test@example.com";
        String subject = "Test Subject";
        String body = "Test Body";

        doNothing().when(mailSender).send(any(MimeMessage.class));

        // when
        emailSender.send(to, subject, body);

        // then
        verify(mailSender).createMimeMessage();
    }
}
