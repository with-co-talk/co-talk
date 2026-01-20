package com.cotalk.domain.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 도메인 예외 클래스들의 단위 테스트.
 *
 * @author seunggu.lee
 */
@DisplayName("도메인 예외 클래스")
class ExceptionClassesTest {

    @Nested
    @DisplayName("UserNotFoundException")
    class UserNotFoundExceptionTest {

        @Test
        @DisplayName("메시지로 예외를 생성한다")
        void should_createException_when_messageProvided() {
            // when
            UserNotFoundException e = new UserNotFoundException("테스트 메시지");

            // then
            assertThat(e.getMessage()).isEqualTo("테스트 메시지");
        }

        @Test
        @DisplayName("사용자 ID로 예외를 생성한다")
        void should_createException_when_userIdProvided() {
            // when
            UserNotFoundException e = new UserNotFoundException(123L);

            // then
            assertThat(e.getMessage()).contains("123");
        }
    }

    @Nested
    @DisplayName("ChatRoomNotFoundException")
    class ChatRoomNotFoundExceptionTest {

        @Test
        @DisplayName("메시지로 예외를 생성한다")
        void should_createException_when_messageProvided() {
            // when
            ChatRoomNotFoundException e = new ChatRoomNotFoundException("테스트 메시지");

            // then
            assertThat(e.getMessage()).isEqualTo("테스트 메시지");
        }

        @Test
        @DisplayName("채팅방 ID로 예외를 생성한다")
        void should_createException_when_chatRoomIdProvided() {
            // when
            ChatRoomNotFoundException e = new ChatRoomNotFoundException(456L);

            // then
            assertThat(e.getMessage()).contains("456");
        }
    }

    @Nested
    @DisplayName("MessageNotFoundException")
    class MessageNotFoundExceptionTest {

        @Test
        @DisplayName("메시지로 예외를 생성한다")
        void should_createException_when_messageProvided() {
            // when
            MessageNotFoundException e = new MessageNotFoundException("테스트 메시지");

            // then
            assertThat(e.getMessage()).isEqualTo("테스트 메시지");
        }

        @Test
        @DisplayName("메시지 ID로 예외를 생성한다")
        void should_createException_when_messageIdProvided() {
            // when
            MessageNotFoundException e = new MessageNotFoundException(789L);

            // then
            assertThat(e.getMessage()).contains("789");
        }
    }

    @Nested
    @DisplayName("MessageBrokerException")
    class MessageBrokerExceptionTest {

        @Test
        @DisplayName("메시지로 예외를 생성한다")
        void should_createException_when_messageProvided() {
            // when
            MessageBrokerException e = new MessageBrokerException("테스트 메시지");

            // then
            assertThat(e.getMessage()).isEqualTo("테스트 메시지");
        }

        @Test
        @DisplayName("메시지와 원인으로 예외를 생성한다")
        void should_createException_when_messageAndCauseProvided() {
            // given
            Throwable cause = new RuntimeException("원인 예외");

            // when
            MessageBrokerException e = new MessageBrokerException("테스트 메시지", cause);

            // then
            assertThat(e.getMessage()).isEqualTo("테스트 메시지");
            assertThat(e.getCause()).isEqualTo(cause);
        }

        @Test
        @DisplayName("직렬화 실패 예외를 생성한다")
        void should_createSerializationFailedException() {
            // given
            Throwable cause = new RuntimeException("직렬화 실패");

            // when
            MessageBrokerException e = MessageBrokerException.serializationFailed(cause);

            // then
            assertThat(e.getMessage()).contains("직렬화");
            assertThat(e.getCause()).isEqualTo(cause);
        }

        @Test
        @DisplayName("리액션 직렬화 실패 예외를 생성한다")
        void should_createReactionSerializationFailedException() {
            // given
            Throwable cause = new RuntimeException("리액션 직렬화 실패");

            // when
            MessageBrokerException e = MessageBrokerException.reactionSerializationFailed(cause);

            // then
            assertThat(e.getMessage()).contains("리액션");
            assertThat(e.getCause()).isEqualTo(cause);
        }

        @Test
        @DisplayName("발행 실패 예외를 생성한다")
        void should_createPublishFailedException() {
            // given
            Throwable cause = new RuntimeException("발행 실패");

            // when
            MessageBrokerException e = MessageBrokerException.publishFailed("chat:room:1", cause);

            // then
            assertThat(e.getMessage()).contains("chat:room:1");
            assertThat(e.getCause()).isEqualTo(cause);
        }
    }

    @Nested
    @DisplayName("RateLimitExceededException")
    class RateLimitExceededExceptionTest {

        @Test
        @DisplayName("메시지와 재시도 시간으로 예외를 생성한다")
        void should_createException_when_messageAndRetryAfterProvided() {
            // when
            RateLimitExceededException e = new RateLimitExceededException("테스트 메시지", 60L);

            // then
            assertThat(e.getMessage()).isEqualTo("테스트 메시지");
            assertThat(e.getRetryAfterSeconds()).isEqualTo(60L);
        }

        @Test
        @DisplayName("tooManyRequests 팩토리 메서드로 예외를 생성한다")
        void should_createException_when_tooManyRequestsCalled() {
            // when
            RateLimitExceededException e = RateLimitExceededException.tooManyRequests(30L);

            // then
            assertThat(e.getMessage()).contains("30");
            assertThat(e.getRetryAfterSeconds()).isEqualTo(30L);
        }
    }

    @Nested
    @DisplayName("InvalidEmojiException")
    class InvalidEmojiExceptionTest {

        @Test
        @DisplayName("메시지로 예외를 생성한다")
        void should_createException_when_messageProvided() {
            // when
            InvalidEmojiException e = new InvalidEmojiException("테스트 메시지");

            // then
            assertThat(e.getMessage()).isEqualTo("테스트 메시지");
        }

        @Test
        @DisplayName("invalidFormat 팩토리 메서드로 예외를 생성한다")
        void should_createException_when_invalidFormatCalled() {
            // when
            InvalidEmojiException e = InvalidEmojiException.invalidFormat("invalid_emoji");

            // then
            assertThat(e.getMessage()).contains("invalid_emoji");
        }
    }

    @Nested
    @DisplayName("ChatRoomAccessDeniedException")
    class ChatRoomAccessDeniedExceptionTest {

        @Test
        @DisplayName("메시지로 예외를 생성한다")
        void should_createException_when_messageProvided() {
            // when
            ChatRoomAccessDeniedException e = new ChatRoomAccessDeniedException("테스트 메시지");

            // then
            assertThat(e.getMessage()).isEqualTo("테스트 메시지");
        }

        @Test
        @DisplayName("채팅방 ID와 사용자 ID로 예외를 생성한다")
        void should_createException_when_chatRoomIdAndUserIdProvided() {
            // when
            ChatRoomAccessDeniedException e = new ChatRoomAccessDeniedException(100L, 1L);

            // then
            assertThat(e.getMessage()).contains("100");
            assertThat(e.getMessage()).contains("1");
        }
    }

    @Nested
    @DisplayName("UnauthorizedException")
    class UnauthorizedExceptionTest {

        @Test
        @DisplayName("메시지로 예외를 생성한다")
        void should_createException_when_messageProvided() {
            // when
            UnauthorizedException e = new UnauthorizedException("인증 실패");

            // then
            assertThat(e.getMessage()).isEqualTo("인증 실패");
        }
    }

    @Nested
    @DisplayName("DuplicateEmailException")
    class DuplicateEmailExceptionTest {

        @Test
        @DisplayName("이메일로 예외를 생성한다")
        void should_createException_when_emailProvided() {
            // when
            DuplicateEmailException e = new DuplicateEmailException("test@example.com");

            // then
            assertThat(e.getMessage()).contains("test@example.com");
        }
    }

    @Nested
    @DisplayName("DuplicateNicknameException")
    class DuplicateNicknameExceptionTest {

        @Test
        @DisplayName("닉네임으로 예외를 생성한다")
        void should_createException_when_nicknameProvided() {
            // when
            DuplicateNicknameException e = new DuplicateNicknameException("testNick");

            // then
            assertThat(e.getMessage()).contains("testNick");
        }
    }

    @Nested
    @DisplayName("InvalidCredentialsException")
    class InvalidCredentialsExceptionTest {

        @Test
        @DisplayName("기본 메시지로 예외를 생성한다")
        void should_createException() {
            // when
            InvalidCredentialsException e = new InvalidCredentialsException();

            // then
            assertThat(e.getMessage()).isNotNull();
        }
    }

    @Nested
    @DisplayName("InvalidRefreshTokenException")
    class InvalidRefreshTokenExceptionTest {

        @Test
        @DisplayName("기본 메시지로 예외를 생성한다")
        void should_createException() {
            // when
            InvalidRefreshTokenException e = new InvalidRefreshTokenException();

            // then
            assertThat(e.getMessage()).isNotNull();
        }
    }

    @Nested
    @DisplayName("TermsAgreementException")
    class TermsAgreementExceptionTest {

        @Test
        @DisplayName("메시지로 예외를 생성한다")
        void should_createException_when_messageProvided() {
            // when
            TermsAgreementException e = new TermsAgreementException("약관 미동의");

            // then
            assertThat(e.getMessage()).isEqualTo("약관 미동의");
        }
    }

    @Nested
    @DisplayName("FriendNotFoundException")
    class FriendNotFoundExceptionTest {

        @Test
        @DisplayName("메시지로 예외를 생성한다")
        void should_createException_when_messageProvided() {
            // when
            FriendNotFoundException e = new FriendNotFoundException("친구 없음");

            // then
            assertThat(e.getMessage()).isEqualTo("친구 없음");
        }
    }

    @Nested
    @DisplayName("FriendRequestNotFoundException")
    class FriendRequestNotFoundExceptionTest {

        @Test
        @DisplayName("요청 ID로 예외를 생성한다")
        void should_createException_when_requestIdProvided() {
            // when
            FriendRequestNotFoundException e = new FriendRequestNotFoundException(123L);

            // then
            assertThat(e.getMessage()).contains("123");
        }
    }

    @Nested
    @DisplayName("FriendRequestAccessDeniedException")
    class FriendRequestAccessDeniedExceptionTest {

        @Test
        @DisplayName("메시지로 예외를 생성한다")
        void should_createException_when_messageProvided() {
            // when
            FriendRequestAccessDeniedException e = new FriendRequestAccessDeniedException("접근 거부");

            // then
            assertThat(e.getMessage()).isEqualTo("접근 거부");
        }
    }

    @Nested
    @DisplayName("InvalidFriendRequestException")
    class InvalidFriendRequestExceptionTest {

        @Test
        @DisplayName("메시지로 예외를 생성한다")
        void should_createException_when_messageProvided() {
            // when
            InvalidFriendRequestException e = new InvalidFriendRequestException("잘못된 요청");

            // then
            assertThat(e.getMessage()).isEqualTo("잘못된 요청");
        }
    }

    @Nested
    @DisplayName("BlockNotFoundException")
    class BlockNotFoundExceptionTest {

        @Test
        @DisplayName("메시지로 예외를 생성한다")
        void should_createException_when_messageProvided() {
            // when
            BlockNotFoundException e = new BlockNotFoundException("차단 없음");

            // then
            assertThat(e.getMessage()).isEqualTo("차단 없음");
        }
    }

    @Nested
    @DisplayName("InvalidBlockException")
    class InvalidBlockExceptionTest {

        @Test
        @DisplayName("메시지로 예외를 생성한다")
        void should_createException_when_messageProvided() {
            // when
            InvalidBlockException e = new InvalidBlockException("잘못된 차단");

            // then
            assertThat(e.getMessage()).isEqualTo("잘못된 차단");
        }
    }

    @Nested
    @DisplayName("ReportNotFoundException")
    class ReportNotFoundExceptionTest {

        @Test
        @DisplayName("신고 ID로 예외를 생성한다")
        void should_createException_when_reportIdProvided() {
            // when
            ReportNotFoundException e = new ReportNotFoundException(456L);

            // then
            assertThat(e.getMessage()).contains("456");
        }
    }

    @Nested
    @DisplayName("InvalidReportException")
    class InvalidReportExceptionTest {

        @Test
        @DisplayName("메시지로 예외를 생성한다")
        void should_createException_when_messageProvided() {
            // when
            InvalidReportException e = new InvalidReportException("잘못된 신고");

            // then
            assertThat(e.getMessage()).isEqualTo("잘못된 신고");
        }
    }

    @Nested
    @DisplayName("InvalidGroupChatException")
    class InvalidGroupChatExceptionTest {

        @Test
        @DisplayName("메시지로 예외를 생성한다")
        void should_createException_when_messageProvided() {
            // when
            InvalidGroupChatException e = new InvalidGroupChatException("잘못된 그룹 채팅");

            // then
            assertThat(e.getMessage()).isEqualTo("잘못된 그룹 채팅");
        }
    }

    @Nested
    @DisplayName("InvalidChatRoomException")
    class InvalidChatRoomExceptionTest {

        @Test
        @DisplayName("메시지로 예외를 생성한다")
        void should_createException_when_messageProvided() {
            // when
            InvalidChatRoomException e = new InvalidChatRoomException("잘못된 채팅방");

            // then
            assertThat(e.getMessage()).isEqualTo("잘못된 채팅방");
        }
    }

    @Nested
    @DisplayName("MessageAccessDeniedException")
    class MessageAccessDeniedExceptionTest {

        @Test
        @DisplayName("메시지로 예외를 생성한다")
        void should_createException_when_messageProvided() {
            // when
            MessageAccessDeniedException e = new MessageAccessDeniedException("접근 거부");

            // then
            assertThat(e.getMessage()).isEqualTo("접근 거부");
        }
    }

    @Nested
    @DisplayName("MessageReactionNotFoundException")
    class MessageReactionNotFoundExceptionTest {

        @Test
        @DisplayName("메시지 ID, 사용자 ID, 이모지로 예외를 생성한다")
        void should_createException_when_parametersProvided() {
            // when
            MessageReactionNotFoundException e = new MessageReactionNotFoundException(1L, 2L, "👍");

            // then
            assertThat(e.getMessage()).contains("1");
            assertThat(e.getMessage()).contains("2");
            assertThat(e.getMessage()).contains("👍");
        }
    }

    @Nested
    @DisplayName("InvalidPasswordResetTokenException")
    class InvalidPasswordResetTokenExceptionTest {

        @Test
        @DisplayName("메시지로 예외를 생성한다")
        void should_createException_when_messageProvided() {
            // when
            InvalidPasswordResetTokenException e = new InvalidPasswordResetTokenException("토큰 오류");

            // then
            assertThat(e.getMessage()).isEqualTo("토큰 오류");
        }

        @Test
        @DisplayName("expired 팩토리 메서드로 예외를 생성한다")
        void should_createExpiredException() {
            // when
            InvalidPasswordResetTokenException e = InvalidPasswordResetTokenException.expired();

            // then
            assertThat(e.getMessage()).contains("만료");
        }

        @Test
        @DisplayName("alreadyUsed 팩토리 메서드로 예외를 생성한다")
        void should_createAlreadyUsedException() {
            // when
            InvalidPasswordResetTokenException e = InvalidPasswordResetTokenException.alreadyUsed();

            // then
            assertThat(e.getMessage()).contains("사용");
        }

        @Test
        @DisplayName("notFound 팩토리 메서드로 예외를 생성한다")
        void should_createNotFoundException() {
            // when
            InvalidPasswordResetTokenException e = InvalidPasswordResetTokenException.notFound();

            // then
            assertThat(e.getMessage()).contains("유효하지 않");
        }
    }

    @Nested
    @DisplayName("SelfActionNotAllowedException")
    class SelfActionNotAllowedExceptionTest {

        @Test
        @DisplayName("액션 타입으로 예외를 생성한다")
        void should_createException_when_actionTypeProvided() {
            // when
            SelfActionNotAllowedException e = new SelfActionNotAllowedException("차단");

            // then
            assertThat(e.getMessage()).isEqualTo("자기 자신을 차단할 수 없습니다");
            assertThat(e.getActionType()).isEqualTo("차단");
        }

        @Test
        @DisplayName("팩토리 메서드로 차단 예외를 생성한다")
        void should_createBlockException() {
            // when
            SelfActionNotAllowedException e = SelfActionNotAllowedException.block();

            // then
            assertThat(e.getMessage()).contains("차단");
        }

        @Test
        @DisplayName("팩토리 메서드로 친구 요청 예외를 생성한다")
        void should_createFriendRequestException() {
            // when
            SelfActionNotAllowedException e = SelfActionNotAllowedException.friendRequest();

            // then
            assertThat(e.getMessage()).contains("친구 요청");
        }

        @Test
        @DisplayName("팩토리 메서드로 신고 예외를 생성한다")
        void should_createReportException() {
            // when
            SelfActionNotAllowedException e = SelfActionNotAllowedException.report();

            // then
            assertThat(e.getMessage()).contains("신고");
        }
    }

    @Nested
    @DisplayName("FileStorageException")
    class FileStorageExceptionTest {

        @Test
        @DisplayName("메시지로 예외를 생성한다")
        void should_createException_when_messageProvided() {
            // when
            FileStorageException e = new FileStorageException("파일 저장 실패");

            // then
            assertThat(e.getMessage()).isEqualTo("파일 저장 실패");
        }

        @Test
        @DisplayName("메시지와 원인으로 예외를 생성한다")
        void should_createException_when_messageAndCauseProvided() {
            // given
            Throwable cause = new RuntimeException("원인");

            // when
            FileStorageException e = new FileStorageException("파일 저장 실패", cause);

            // then
            assertThat(e.getMessage()).isEqualTo("파일 저장 실패");
            assertThat(e.getCause()).isEqualTo(cause);
        }
    }
}
