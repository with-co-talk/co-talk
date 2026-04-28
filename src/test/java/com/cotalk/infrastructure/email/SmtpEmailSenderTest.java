package com.cotalk.infrastructure.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
        lenient().when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

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

    @Test
    @DisplayName("From 표시명과 주소를 설정한다")
    void should_setConfiguredFromAddress_when_fromPropertiesProvided() throws Exception {
        // given
        MailProperties mailProperties = new MailProperties();
        mailProperties.setHost("smtp.example.com");
        mailProperties.setUsername("tp.bmsg@gmail.com");
        mailProperties.setPassword("test-password");
        mailProperties.setFromAddress("no-reply@cotalk.co.kr");
        mailProperties.setFromName("Co-Talk");

        SmtpEmailSender configuredSender = new SmtpEmailSender(mailSender, mailProperties);
        doNothing().when(mailSender).send(any(MimeMessage.class));

        // when
        configuredSender.send("test@example.com", "Test Subject", "Test Body");

        // then
        ArgumentCaptor<InternetAddress> fromCaptor = ArgumentCaptor.forClass(InternetAddress.class);
        verify(mimeMessage).setFrom(fromCaptor.capture());
        InternetAddress from = fromCaptor.getValue();
        assertThat(from.getAddress()).isEqualTo("no-reply@cotalk.co.kr");
        assertThat(from.getPersonal()).isEqualTo("Co-Talk");
    }

    @Test
    @DisplayName("차단 도메인 이메일이면 SMTP 발송을 건너뛴다")
    void should_suppressEmail_when_emailDomainIsSuppressed() {
        // given
        ReflectionTestUtils.setField(emailSender, "suppressedEmailDomains", "test.cotalk.com");

        // when
        emailSender.send("loadtest@test.cotalk.com", "Test Subject", "Test Body");

        // then
        verify(mailSender, never()).createMimeMessage();
        verify(mailSender, never()).send(any(MimeMessage.class));
    }
}
