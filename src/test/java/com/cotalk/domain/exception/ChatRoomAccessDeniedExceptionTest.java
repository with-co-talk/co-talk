package com.cotalk.domain.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChatRoomAccessDeniedExceptionTest {

    @Test
    @DisplayName("메시지로 예외 생성")
    void should_createException_when_messageProvided() {
        // given
        String message = "접근이 거부되었습니다.";

        // when
        ChatRoomAccessDeniedException exception = new ChatRoomAccessDeniedException(message);

        // then
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception).isInstanceOf(DomainException.class);
    }

    @Test
    @DisplayName("채팅방 ID와 사용자 ID로 예외 생성")
    void should_createException_when_chatRoomIdAndUserIdProvided() {
        // given
        Long chatRoomId = 100L;
        Long userId = 1L;

        // when
        ChatRoomAccessDeniedException exception = new ChatRoomAccessDeniedException(chatRoomId, userId);

        // then
        assertThat(exception.getMessage()).contains("1");
        assertThat(exception.getMessage()).contains("100");
        assertThat(exception).isInstanceOf(DomainException.class);
    }
}
