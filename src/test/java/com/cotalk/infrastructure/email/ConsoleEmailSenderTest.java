package com.cotalk.infrastructure.email;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * ConsoleEmailSender 단위 테스트.
 *
 * @author seunggu.lee
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ConsoleEmailSender 단위 테스트")
class ConsoleEmailSenderTest {

    private final ConsoleEmailSender emailSender = new ConsoleEmailSender();

    @Test
    @DisplayName("이메일 발송 성공 - 콘솔 출력")
    void should_logEmail_when_sendCalled() {
        // given
        String to = "test@example.com";
        String subject = "Test Subject";
        String body = "Test Body";

        // when & then
        assertDoesNotThrow(() -> emailSender.send(to, subject, body));
    }

    @Test
    @DisplayName("비밀번호 재설정 이메일 발송 성공 - 콘솔 출력")
    void should_logPasswordResetEmail_when_sendPasswordResetEmailCalled() {
        // given
        String to = "test@example.com";
        String resetLink = "https://cotalk.com/reset?token=abc123";

        // when & then
        assertDoesNotThrow(() -> emailSender.sendPasswordResetEmail(to, resetLink));
    }

    @Test
    @DisplayName("HTML 본문 이메일 발송 성공")
    void should_logHtmlBody_when_sendWithHtmlBody() {
        // given
        String to = "test@example.com";
        String subject = "HTML Email";
        String body = "<html><body><h1>Hello</h1></body></html>";

        // when & then
        assertDoesNotThrow(() -> emailSender.send(to, subject, body));
    }

    @Test
    @DisplayName("긴 본문 이메일 발송 성공")
    void should_logLongBody_when_sendWithLongBody() {
        // given
        String to = "test@example.com";
        String subject = "Long Body Email";
        String body = "A".repeat(10000);

        // when & then
        assertDoesNotThrow(() -> emailSender.send(to, subject, body));
    }

    @Test
    @DisplayName("특수 문자가 포함된 이메일 발송 성공")
    void should_logSpecialCharacters_when_sendWithSpecialChars() {
        // given
        String to = "test@example.com";
        String subject = "특수 문자 테스트 !@#$%";
        String body = "한글 본문 테스트 \n\t특수문자: <>&\"'";

        // when & then
        assertDoesNotThrow(() -> emailSender.send(to, subject, body));
    }
}
