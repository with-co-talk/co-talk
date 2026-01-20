package com.cotalk.infrastructure.websocket;

import com.cotalk.domain.exception.ChatRoomAccessDeniedException;
import com.cotalk.domain.exception.ChatRoomNotFoundException;
import com.cotalk.domain.exception.DomainException;
import com.cotalk.domain.exception.InvalidEmojiException;
import com.cotalk.domain.exception.MessageNotFoundException;
import com.cotalk.domain.exception.UnauthorizedException;
import com.cotalk.domain.exception.UserNotFoundException;
import com.cotalk.infrastructure.websocket.WebSocketExceptionHandler.WebSocketErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * WebSocketExceptionHandler 단위 테스트.
 *
 * @author seunggu.lee
 */
@DisplayName("WebSocketExceptionHandler")
class WebSocketExceptionHandlerTest {

    private WebSocketExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new WebSocketExceptionHandler();
    }

    @Nested
    @DisplayName("인증 예외 처리")
    class UnauthorizedExceptionHandling {

        @Test
        @DisplayName("UnauthorizedException을 처리한다")
        void should_handleUnauthorizedException() {
            // given
            UnauthorizedException e = new UnauthorizedException("인증되지 않은 사용자입니다.");

            // when
            WebSocketErrorResponse response = handler.handleUnauthorizedException(e);

            // then
            assertThat(response.code()).isEqualTo("UNAUTHORIZED");
            assertThat(response.message()).isEqualTo("인증되지 않은 사용자입니다.");
            assertThat(response.timestamp()).isNotNull();
        }
    }

    @Nested
    @DisplayName("사용자 예외 처리")
    class UserExceptionHandling {

        @Test
        @DisplayName("UserNotFoundException을 처리한다")
        void should_handleUserNotFoundException() {
            // given
            UserNotFoundException e = new UserNotFoundException(1L);

            // when
            WebSocketErrorResponse response = handler.handleUserNotFoundException(e);

            // then
            assertThat(response.code()).isEqualTo("USER_NOT_FOUND");
            assertThat(response.message()).contains("1");
            assertThat(response.timestamp()).isNotNull();
        }
    }

    @Nested
    @DisplayName("채팅방 예외 처리")
    class ChatRoomExceptionHandling {

        @Test
        @DisplayName("ChatRoomNotFoundException을 처리한다")
        void should_handleChatRoomNotFoundException() {
            // given
            ChatRoomNotFoundException e = new ChatRoomNotFoundException(100L);

            // when
            WebSocketErrorResponse response = handler.handleChatRoomNotFoundException(e);

            // then
            assertThat(response.code()).isEqualTo("CHAT_ROOM_NOT_FOUND");
            assertThat(response.message()).contains("100");
            assertThat(response.timestamp()).isNotNull();
        }

        @Test
        @DisplayName("ChatRoomAccessDeniedException을 처리한다")
        void should_handleChatRoomAccessDeniedException() {
            // given
            ChatRoomAccessDeniedException e = new ChatRoomAccessDeniedException(100L, 1L);

            // when
            WebSocketErrorResponse response = handler.handleChatRoomAccessDeniedException(e);

            // then
            assertThat(response.code()).isEqualTo("ACCESS_DENIED");
            assertThat(response.timestamp()).isNotNull();
        }
    }

    @Nested
    @DisplayName("메시지 예외 처리")
    class MessageExceptionHandling {

        @Test
        @DisplayName("MessageNotFoundException을 처리한다")
        void should_handleMessageNotFoundException() {
            // given
            MessageNotFoundException e = new MessageNotFoundException(500L);

            // when
            WebSocketErrorResponse response = handler.handleMessageNotFoundException(e);

            // then
            assertThat(response.code()).isEqualTo("MESSAGE_NOT_FOUND");
            assertThat(response.message()).contains("500");
            assertThat(response.timestamp()).isNotNull();
        }
    }

    @Nested
    @DisplayName("이모지 예외 처리")
    class EmojiExceptionHandling {

        @Test
        @DisplayName("InvalidEmojiException을 처리한다")
        void should_handleInvalidEmojiException() {
            // given
            InvalidEmojiException e = InvalidEmojiException.invalidFormat("invalid");

            // when
            WebSocketErrorResponse response = handler.handleInvalidEmojiException(e);

            // then
            assertThat(response.code()).isEqualTo("INVALID_EMOJI");
            assertThat(response.timestamp()).isNotNull();
        }
    }

    @Nested
    @DisplayName("도메인 예외 처리")
    class DomainExceptionHandling {

        @Test
        @DisplayName("DomainException을 처리한다")
        void should_handleDomainException() {
            // given
            DomainException e = new DomainException("도메인 오류가 발생했습니다.");

            // when
            WebSocketErrorResponse response = handler.handleDomainException(e);

            // then
            assertThat(response.code()).isEqualTo("BAD_REQUEST");
            assertThat(response.message()).isEqualTo("도메인 오류가 발생했습니다.");
            assertThat(response.timestamp()).isNotNull();
        }
    }

    @Nested
    @DisplayName("일반 예외 처리")
    class GeneralExceptionHandling {

        @Test
        @DisplayName("예상하지 못한 Exception을 처리한다")
        void should_handleGenericException() {
            // given
            Exception e = new RuntimeException("예상치 못한 오류");

            // when
            WebSocketErrorResponse response = handler.handleException(e);

            // then
            assertThat(response.code()).isEqualTo("INTERNAL_ERROR");
            assertThat(response.message()).isEqualTo("메시지 처리 중 오류가 발생했습니다.");
            assertThat(response.timestamp()).isNotNull();
        }
    }

    @Nested
    @DisplayName("WebSocketErrorResponse record 테스트")
    class WebSocketErrorResponseTest {

        @Test
        @DisplayName("WebSocketErrorResponse record가 올바르게 생성된다")
        void should_createRecord_when_validArguments() {
            // given
            java.time.LocalDateTime now = java.time.LocalDateTime.now();

            // when
            WebSocketErrorResponse response = new WebSocketErrorResponse("TEST_CODE", "테스트 메시지", now);

            // then
            assertThat(response.code()).isEqualTo("TEST_CODE");
            assertThat(response.message()).isEqualTo("테스트 메시지");
            assertThat(response.timestamp()).isEqualTo(now);
        }
    }
}
