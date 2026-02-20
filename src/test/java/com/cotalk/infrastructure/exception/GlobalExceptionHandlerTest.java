package com.cotalk.infrastructure.exception;

import com.cotalk.domain.exception.BlockNotFoundException;
import com.cotalk.domain.exception.ChatRoomAccessDeniedException;
import com.cotalk.domain.exception.ChatRoomNotFoundException;
import com.cotalk.domain.exception.DomainException;
import com.cotalk.domain.exception.DuplicateEmailException;
import com.cotalk.domain.exception.DuplicateNicknameException;
import com.cotalk.domain.exception.FileUploadException;
import com.cotalk.domain.exception.FriendNotFoundException;
import com.cotalk.domain.exception.FriendRequestNotFoundException;
import com.cotalk.domain.exception.InvalidBlockException;
import com.cotalk.domain.exception.InvalidCredentialsException;
import com.cotalk.domain.exception.InvalidEmojiException;
import com.cotalk.domain.exception.InvalidFriendRequestException;
import com.cotalk.domain.exception.InvalidPasswordResetTokenException;
import com.cotalk.domain.exception.InvalidRefreshTokenException;
import com.cotalk.domain.exception.InvalidReportException;
import com.cotalk.domain.exception.MessageNotFoundException;
import com.cotalk.domain.exception.MessageReactionNotFoundException;
import com.cotalk.domain.exception.RateLimitExceededException;
import com.cotalk.domain.exception.ReportNotFoundException;
import com.cotalk.domain.exception.ResourceAccessDeniedException;
import com.cotalk.domain.exception.TermsAgreementException;
import com.cotalk.domain.exception.UnauthorizedException;
import com.cotalk.domain.exception.UserNotFoundException;
import com.cotalk.infrastructure.lock.DistributedLockException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleDomainException(exception);

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
            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleDomainException(exception);

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
            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleDomainException(exception);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().code()).isEqualTo("CHAT_ROOM_ACCESS_DENIED");
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
            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleDomainException(exception);

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
            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleDomainException(exception);

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

    @Nested
    @DisplayName("BlockNotFoundException 처리")
    class HandleBlockNotFoundException {

        @Test
        @DisplayName("BlockNotFoundException은 404 NOT_FOUND 반환")
        void should_returnNotFound_when_blockNotFound() {
            // given
            BlockNotFoundException exception = new BlockNotFoundException("차단 정보를 찾을 수 없습니다.");

            // when
            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleDomainException(exception);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().code()).isEqualTo("BLOCK_NOT_FOUND");
        }
    }

    @Nested
    @DisplayName("InvalidBlockException 처리")
    class HandleInvalidBlockException {

        @Test
        @DisplayName("InvalidBlockException은 400 BAD_REQUEST 반환")
        void should_returnBadRequest_when_invalidBlock() {
            // given
            InvalidBlockException exception = new InvalidBlockException("이미 차단한 사용자입니다.");

            // when
            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleDomainException(exception);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().code()).isEqualTo("INVALID_BLOCK");
        }
    }

    @Nested
    @DisplayName("ReportNotFoundException 처리")
    class HandleReportNotFoundException {

        @Test
        @DisplayName("ReportNotFoundException은 404 NOT_FOUND 반환")
        void should_returnNotFound_when_reportNotFound() {
            // given
            ReportNotFoundException exception = new ReportNotFoundException(1L);

            // when
            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleDomainException(exception);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().code()).isEqualTo("REPORT_NOT_FOUND");
        }
    }

    @Nested
    @DisplayName("InvalidReportException 처리")
    class HandleInvalidReportException {

        @Test
        @DisplayName("InvalidReportException은 400 BAD_REQUEST 반환")
        void should_returnBadRequest_when_invalidReport() {
            // given
            InvalidReportException exception = new InvalidReportException("유효하지 않은 신고입니다.");

            // when
            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleDomainException(exception);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().code()).isEqualTo("INVALID_REPORT");
        }
    }

    @Nested
    @DisplayName("DistributedLockException 처리")
    class HandleDistributedLockException {

        @Test
        @DisplayName("DistributedLockException은 503 SERVICE_UNAVAILABLE 반환")
        void should_returnServiceUnavailable_when_lockFailed() {
            // given
            DistributedLockException exception = new DistributedLockException("분산락 획득 실패");

            // when
            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleDistributedLockException(exception);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().error()).isEqualTo("일시적으로 서비스를 이용할 수 없습니다. 잠시 후 다시 시도해주세요.");
            assertThat(response.getBody().code()).isEqualTo("SERVICE_UNAVAILABLE");
        }
    }

    @Nested
    @DisplayName("DuplicateEmailException 처리")
    class HandleDuplicateEmailException {

        @Test
        @DisplayName("DuplicateEmailException은 409 CONFLICT 반환")
        void should_returnConflict_when_duplicateEmail() {
            // given
            DuplicateEmailException exception = new DuplicateEmailException("test@example.com");

            // when
            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleDomainException(exception);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().code()).isEqualTo("DUPLICATE_EMAIL");
        }
    }

    @Nested
    @DisplayName("InvalidCredentialsException 처리")
    class HandleInvalidCredentialsException {

        @Test
        @DisplayName("InvalidCredentialsException은 401 UNAUTHORIZED 반환")
        void should_returnUnauthorized_when_invalidCredentials() {
            // given
            InvalidCredentialsException exception = new InvalidCredentialsException();

            // when
            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleInvalidCredentialsException(exception);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().code()).isEqualTo("INVALID_CREDENTIALS");
        }
    }

    @Nested
    @DisplayName("ResourceAccessDeniedException 처리")
    class HandleResourceAccessDeniedException {

        @Test
        @DisplayName("ResourceAccessDeniedException은 403 FORBIDDEN 반환")
        void should_returnForbidden_when_resourceAccessDenied() {
            // given
            ResourceAccessDeniedException exception = new ResourceAccessDeniedException();

            // when
            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleDomainException(exception);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().code()).isEqualTo("ACCESS_DENIED");
        }
    }

    @Nested
    @DisplayName("UnauthorizedException 처리")
    class HandleUnauthorizedException {

        @Test
        @DisplayName("UnauthorizedException은 401 UNAUTHORIZED 반환")
        void should_returnUnauthorized_when_unauthorized() {
            // given
            UnauthorizedException exception = new UnauthorizedException("인증이 필요합니다.");

            // when
            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleDomainException(exception);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().code()).isEqualTo("UNAUTHORIZED");
        }
    }

    @Nested
    @DisplayName("InvalidRefreshTokenException 처리")
    class HandleInvalidRefreshTokenException {

        @Test
        @DisplayName("InvalidRefreshTokenException은 401 UNAUTHORIZED 반환")
        void should_returnUnauthorized_when_invalidRefreshToken() {
            // given
            InvalidRefreshTokenException exception = new InvalidRefreshTokenException();

            // when
            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleDomainException(exception);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().code()).isEqualTo("INVALID_REFRESH_TOKEN");
        }
    }

    @Nested
    @DisplayName("FriendRequestNotFoundException 처리")
    class HandleFriendRequestNotFoundException {

        @Test
        @DisplayName("FriendRequestNotFoundException은 404 NOT_FOUND 반환")
        void should_returnNotFound_when_friendRequestNotFound() {
            // given
            FriendRequestNotFoundException exception = new FriendRequestNotFoundException(1L);

            // when
            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleDomainException(exception);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().code()).isEqualTo("FRIEND_REQUEST_NOT_FOUND");
        }
    }

    @Nested
    @DisplayName("ResourceAccessDeniedException (friendRequest) 처리")
    class HandleFriendRequestAccessDeniedException {

        @Test
        @DisplayName("friendRequestNotReceiver는 403 FORBIDDEN 반환")
        void should_returnForbidden_when_friendRequestAccessDenied() {
            // given
            ResourceAccessDeniedException exception = ResourceAccessDeniedException.friendRequestNotReceiver();

            // when
            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleDomainException(exception);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().code()).isEqualTo("ACCESS_DENIED");
        }
    }

    @Nested
    @DisplayName("DuplicateNicknameException 처리")
    class HandleDuplicateNicknameException {

        @Test
        @DisplayName("DuplicateNicknameException은 409 CONFLICT 반환")
        void should_returnConflict_when_duplicateNickname() {
            // given
            DuplicateNicknameException exception = new DuplicateNicknameException("testNickname");

            // when
            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleDomainException(exception);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().code()).isEqualTo("DUPLICATE_NICKNAME");
        }
    }

    @Nested
    @DisplayName("TermsAgreementException 처리")
    class HandleTermsAgreementException {

        @Test
        @DisplayName("TermsAgreementException은 400 BAD_REQUEST 반환")
        void should_returnBadRequest_when_termsAgreementRequired() {
            // given
            TermsAgreementException exception = TermsAgreementException.serviceTermsRequired();

            // when
            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleDomainException(exception);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().code()).isEqualTo("TERMS_AGREEMENT_REQUIRED");
        }
    }

    @Nested
    @DisplayName("FileUploadException 처리")
    class HandleFileUploadException {

        @Test
        @DisplayName("FileUploadException은 400 BAD_REQUEST 반환")
        void should_returnBadRequest_when_fileUploadFailed() {
            // given
            FileUploadException exception = FileUploadException.invalidFileType("application/exe");

            // when
            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleDomainException(exception);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().code()).isEqualTo("FILE_UPLOAD_ERROR");
        }
    }

    @Nested
    @DisplayName("InvalidPasswordResetTokenException 처리")
    class HandleInvalidPasswordResetTokenException {

        @Test
        @DisplayName("InvalidPasswordResetTokenException은 400 BAD_REQUEST 반환")
        void should_returnBadRequest_when_invalidPasswordResetToken() {
            // given
            InvalidPasswordResetTokenException exception = InvalidPasswordResetTokenException.expired();

            // when
            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleDomainException(exception);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().code()).isEqualTo("INVALID_PASSWORD_RESET_TOKEN");
        }
    }

    @Nested
    @DisplayName("MessageNotFoundException 처리")
    class HandleMessageNotFoundException {

        @Test
        @DisplayName("MessageNotFoundException은 404 NOT_FOUND 반환")
        void should_returnNotFound_when_messageNotFound() {
            // given
            MessageNotFoundException exception = new MessageNotFoundException(500L);

            // when
            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleDomainException(exception);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().code()).isEqualTo("MESSAGE_NOT_FOUND");
        }
    }

    @Nested
    @DisplayName("ResourceAccessDeniedException (message) 처리")
    class HandleMessageAccessDeniedException {

        @Test
        @DisplayName("messageNotSender는 403 FORBIDDEN 반환")
        void should_returnForbidden_when_messageAccessDenied() {
            // given
            ResourceAccessDeniedException exception = ResourceAccessDeniedException.messageNotSender();

            // when
            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleDomainException(exception);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().code()).isEqualTo("ACCESS_DENIED");
        }
    }

    @Nested
    @DisplayName("InvalidEmojiException 처리")
    class HandleInvalidEmojiException {

        @Test
        @DisplayName("InvalidEmojiException은 400 BAD_REQUEST 반환")
        void should_returnBadRequest_when_invalidEmoji() {
            // given
            InvalidEmojiException exception = InvalidEmojiException.invalidFormat("invalid");

            // when
            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleDomainException(exception);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().code()).isEqualTo("INVALID_EMOJI");
        }
    }

    @Nested
    @DisplayName("MessageReactionNotFoundException 처리")
    class HandleMessageReactionNotFoundException {

        @Test
        @DisplayName("MessageReactionNotFoundException은 404 NOT_FOUND 반환")
        void should_returnNotFound_when_messageReactionNotFound() {
            // given
            MessageReactionNotFoundException exception = new MessageReactionNotFoundException(1L, 2L, "\uD83D\uDC4D");

            // when
            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleDomainException(exception);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().code()).isEqualTo("MESSAGE_REACTION_NOT_FOUND");
        }
    }

    @Nested
    @DisplayName("RateLimitExceededException 처리")
    class HandleRateLimitExceededException {

        @Test
        @DisplayName("RateLimitExceededException은 429 TOO_MANY_REQUESTS 반환")
        void should_returnTooManyRequests_when_rateLimitExceeded() {
            // given
            RateLimitExceededException exception = RateLimitExceededException.tooManyRequests(60);

            // when
            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleRateLimitExceededException(exception);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().code()).isEqualTo("RATE_LIMIT_EXCEEDED");
            assertThat(response.getHeaders().get("Retry-After")).containsExactly("60");
        }
    }

    @Nested
    @DisplayName("MethodArgumentNotValidException 처리")
    class HandleMethodArgumentNotValidException {

        @Test
        @DisplayName("MethodArgumentNotValidException은 400 BAD_REQUEST 반환")
        void should_returnBadRequest_when_validationFailed() {
            // given
            BindingResult bindingResult = mock(BindingResult.class);
            FieldError fieldError = new FieldError("request", "email", "이메일 형식이 올바르지 않습니다.");
            when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

            MethodParameter methodParameter = mock(MethodParameter.class);
            MethodArgumentNotValidException exception = new MethodArgumentNotValidException(methodParameter, bindingResult);

            // when
            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleValidationException(exception);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().code()).isEqualTo("VALIDATION_ERROR");
            assertThat(response.getBody().error()).contains("email");
        }

        @Test
        @DisplayName("필드 에러가 없을 경우 기본 메시지 반환")
        void should_returnDefaultMessage_when_noFieldErrors() {
            // given
            BindingResult bindingResult = mock(BindingResult.class);
            when(bindingResult.getFieldErrors()).thenReturn(List.of());

            MethodParameter methodParameter = mock(MethodParameter.class);
            MethodArgumentNotValidException exception = new MethodArgumentNotValidException(methodParameter, bindingResult);

            // when
            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleValidationException(exception);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().error()).isEqualTo("유효성 검사 실패");
        }
    }

    @Nested
    @DisplayName("MissingServletRequestParameterException 처리")
    class HandleMissingServletRequestParameterException {

        @Test
        @DisplayName("MissingServletRequestParameterException은 400 BAD_REQUEST 반환")
        void should_returnBadRequest_when_missingParameter() {
            // given
            MissingServletRequestParameterException exception =
                    new MissingServletRequestParameterException("userId", "Long");

            // when
            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleMissingServletRequestParameterException(exception);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().code()).isEqualTo("MISSING_PARAMETER");
            assertThat(response.getBody().error()).contains("userId");
        }
    }

    @Nested
    @DisplayName("IllegalArgumentException 처리")
    class HandleIllegalArgumentException {

        @Test
        @DisplayName("IllegalArgumentException은 400 BAD_REQUEST 반환")
        void should_returnBadRequest_when_illegalArgument() {
            // given
            IllegalArgumentException exception = new IllegalArgumentException("잘못된 인자입니다.");

            // when
            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleIllegalArgumentException(exception);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().code()).isEqualTo("INVALID_ARGUMENT");
            assertThat(response.getBody().error()).isEqualTo("잘못된 인자입니다.");
        }
    }

    @Nested
    @DisplayName("ErrorResponse record 테스트")
    class ErrorResponseTest {

        @Test
        @DisplayName("ErrorResponse record가 올바르게 생성된다")
        void should_createRecord_when_validArguments() {
            // given
            java.time.LocalDateTime now = java.time.LocalDateTime.now();

            // when
            GlobalExceptionHandler.ErrorResponse response = new GlobalExceptionHandler.ErrorResponse("테스트 에러", "TEST_CODE", now);

            // then
            assertThat(response.error()).isEqualTo("테스트 에러");
            assertThat(response.code()).isEqualTo("TEST_CODE");
            assertThat(response.timestamp()).isEqualTo(now);
        }
    }
}
