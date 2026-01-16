package com.cotalk.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ChatRoomMember 엔티티")
class ChatRoomMemberTest {

    @Nested
    @DisplayName("생성 시")
    class Creation {

        @Test
        @DisplayName("채팅방 ID와 사용자 ID로 멤버를 생성할 수 있다")
        void should_CreateMember_when_ValidInputsProvided() {
            // given
            Long chatRoomId = 1L;
            Long userId = 2L;

            // when
            ChatRoomMember member = ChatRoomMember.builder()
                    .chatRoomId(chatRoomId)
                    .userId(userId)
                    .build();

            // then
            assertThat(member.getChatRoomId()).isEqualTo(chatRoomId);
            assertThat(member.getUserId()).isEqualTo(userId);
        }
    }

    @Nested
    @DisplayName("마지막 읽은 시간 갱신 시")
    class UpdateLastReadAt {

        @Test
        @DisplayName("마지막 읽은 시간을 갱신할 수 있다")
        void should_UpdateLastReadAt_when_Called() {
            // given
            ChatRoomMember member = ChatRoomMember.builder()
                    .chatRoomId(1L)
                    .userId(2L)
                    .build();
            LocalDateTime newTime = LocalDateTime.now();

            // when
            member.updateLastReadAt(newTime);

            // then
            assertThat(member.getLastReadAt()).isEqualTo(newTime);
        }
    }
}
