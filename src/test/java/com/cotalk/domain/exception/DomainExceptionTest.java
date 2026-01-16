package com.cotalk.domain.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Domain Exception")
class DomainExceptionTest {

    @Test
    @DisplayName("DomainException은 RuntimeException을 상속한다")
    void should_ExtendRuntimeException_when_Created() {
        // given & when
        DomainException exception = new DomainException("테스트 예외");

        // then
        assertThat(exception).isInstanceOf(RuntimeException.class);
        assertThat(exception.getMessage()).isEqualTo("테스트 예외");
    }

    @Test
    @DisplayName("UserNotFoundException은 메시지로 생성할 수 있다")
    void should_CreateUserNotFoundException_when_MessageProvided() {
        // given & when
        UserNotFoundException exception = new UserNotFoundException("사용자를 찾을 수 없습니다");

        // then
        assertThat(exception).isInstanceOf(DomainException.class);
        assertThat(exception.getMessage()).contains("사용자");
    }

    @Test
    @DisplayName("UserNotFoundException은 ID로 생성할 수 있다")
    void should_CreateUserNotFoundException_when_IdProvided() {
        // given
        Long userId = 123456789L;

        // when
        UserNotFoundException exception = new UserNotFoundException(userId);

        // then
        assertThat(exception.getMessage()).contains(userId.toString());
    }

    @Test
    @DisplayName("ChatRoomNotFoundException은 ID로 생성할 수 있다")
    void should_CreateChatRoomNotFoundException_when_IdProvided() {
        // given
        Long chatRoomId = 987654321L;

        // when
        ChatRoomNotFoundException exception = new ChatRoomNotFoundException(chatRoomId);

        // then
        assertThat(exception).isInstanceOf(DomainException.class);
        assertThat(exception.getMessage()).contains(chatRoomId.toString());
    }

    @Test
    @DisplayName("FriendNotFoundException은 기본 생성자로 생성할 수 있다")
    void should_CreateFriendNotFoundException_when_DefaultConstructor() {
        // given & when
        FriendNotFoundException exception = new FriendNotFoundException();

        // then
        assertThat(exception).isInstanceOf(DomainException.class);
        assertThat(exception.getMessage()).contains("친구");
    }

    @Test
    @DisplayName("InvalidFriendRequestException은 메시지로 생성할 수 있다")
    void should_CreateInvalidFriendRequestException_when_MessageProvided() {
        // given & when
        InvalidFriendRequestException exception = new InvalidFriendRequestException("잘못된 요청입니다");

        // then
        assertThat(exception).isInstanceOf(DomainException.class);
        assertThat(exception.getMessage()).isEqualTo("잘못된 요청입니다");
    }
}
