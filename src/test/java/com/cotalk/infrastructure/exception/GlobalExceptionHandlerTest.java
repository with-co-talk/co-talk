package com.cotalk.infrastructure.exception;

import com.cotalk.domain.exception.ChatRoomAccessDeniedException;
import com.cotalk.domain.exception.ChatRoomNotFoundException;
import com.cotalk.domain.exception.DomainException;
import com.cotalk.domain.exception.FriendNotFoundException;
import com.cotalk.domain.exception.InvalidFriendRequestException;
import com.cotalk.domain.exception.UserNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Nested
    @DisplayName("DomainException 처리")
    class HandleDomainException {

        @Test
        @DisplayName("일반 DomainException은 400 BAD_REQUEST 반환")
        void should_returnBadRequest_when_domainException() {
            // given
            DomainException exception = new DomainException("잘못된 요청입니다.");

            // when
            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleDomainException(exception);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().error()).isEqualTo("잘못된 요청입니다.");
            assertThat(response.getBody().code()).isEqualTo("BAD_REQUEST");
        }
    }

    @Nested
    @DisplayName("UserNotFoundException 처리")
    class HandleUserNotFoundException {

        @Test
        @DisplayName("UserNotFoundException은 404 NOT_FOUND 반환")
        void should_returnNotFound_when_userNotFound() {
            // given
            UserNotFoundException exception = new UserNotFoundException(1L);

            // when
            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleUserNotFoundException(exception);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().code()).isEqualTo("USER_NOT_FOUND");
        }
    }

    @Nested
    @DisplayName("ChatRoomNotFoundException 처리")
    class HandleChatRoomNotFoundException {

        @Test
        @DisplayName("ChatRoomNotFoundException은 404 NOT_FOUND 반환")
        void should_returnNotFound_when_chatRoomNotFound() {
            // given
            ChatRoomNotFoundException exception = new ChatRoomNotFoundException(100L);

            // when
            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleChatRoomNotFoundException(exception);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().code()).isEqualTo("CHAT_ROOM_NOT_FOUND");
        }
    }

    @Nested
    @DisplayName("ChatRoomAccessDeniedException 처리")
    class HandleChatRoomAccessDeniedException {

        @Test
        @DisplayName("ChatRoomAccessDeniedException은 403 FORBIDDEN 반환")
        void should_returnForbidden_when_accessDenied() {
            // given
            ChatRoomAccessDeniedException exception = new ChatRoomAccessDeniedException(100L, 1L);

            // when
            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleChatRoomAccessDeniedException(exception);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().code()).isEqualTo("ACCESS_DENIED");
        }
    }

    @Nested
    @DisplayName("FriendNotFoundException 처리")
    class HandleFriendNotFoundException {

        @Test
        @DisplayName("FriendNotFoundException은 404 NOT_FOUND 반환")
        void should_returnNotFound_when_friendNotFound() {
            // given
            FriendNotFoundException exception = new FriendNotFoundException(1L);

            // when
            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleFriendNotFoundException(exception);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().code()).isEqualTo("FRIEND_NOT_FOUND");
        }
    }

    @Nested
    @DisplayName("InvalidFriendRequestException 처리")
    class HandleInvalidFriendRequestException {

        @Test
        @DisplayName("InvalidFriendRequestException은 400 BAD_REQUEST 반환")
        void should_returnBadRequest_when_invalidFriendRequest() {
            // given
            InvalidFriendRequestException exception = new InvalidFriendRequestException("잘못된 친구 요청입니다.");

            // when
            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleInvalidFriendRequestException(exception);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().code()).isEqualTo("INVALID_FRIEND_REQUEST");
        }
    }

    @Nested
    @DisplayName("일반 Exception 처리")
    class HandleGenericException {

        @Test
        @DisplayName("처리되지 않은 예외는 500 INTERNAL_SERVER_ERROR 반환")
        void should_returnInternalServerError_when_unexpectedException() {
            // given
            Exception exception = new RuntimeException("예상치 못한 오류");

            // when
            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleException(exception);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().error()).isEqualTo("서버 내부 오류가 발생했습니다.");
            assertThat(response.getBody().code()).isEqualTo("INTERNAL_ERROR");
        }
    }
}
