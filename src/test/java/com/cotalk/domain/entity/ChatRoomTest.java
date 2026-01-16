package com.cotalk.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ChatRoom 엔티티")
class ChatRoomTest {

    @Nested
    @DisplayName("생성 시")
    class Creation {

        @Test
        @DisplayName("1:1 채팅방을 생성할 수 있다")
        void should_CreateDirectChatRoom_when_DirectTypeProvided() {
            // given & when
            ChatRoom chatRoom = ChatRoom.builder()
                    .type(ChatRoom.ChatRoomType.DIRECT)
                    .build();

            // then
            assertThat(chatRoom.getType()).isEqualTo(ChatRoom.ChatRoomType.DIRECT);
        }

        @Test
        @DisplayName("그룹 채팅방을 이름과 함께 생성할 수 있다")
        void should_CreateGroupChatRoom_when_GroupTypeAndNameProvided() {
            // given
            String roomName = "개발팀 채팅방";

            // when
            ChatRoom chatRoom = ChatRoom.builder()
                    .type(ChatRoom.ChatRoomType.GROUP)
                    .name(roomName)
                    .build();

            // then
            assertThat(chatRoom.getType()).isEqualTo(ChatRoom.ChatRoomType.GROUP);
            assertThat(chatRoom.getName()).isEqualTo(roomName);
        }
    }
}
