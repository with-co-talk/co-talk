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

    @Nested
    @DisplayName("채팅방 이름 변경 시")
    class UpdateName {

        @Test
        @DisplayName("새 이름으로 변경할 수 있다")
        void should_UpdateName_when_ValidNameProvided() {
            // given
            ChatRoom chatRoom = ChatRoom.builder()
                    .type(ChatRoom.ChatRoomType.GROUP)
                    .name("기존 이름")
                    .build();
            String newName = "새 이름";

            // when
            chatRoom.updateName(newName);

            // then
            assertThat(chatRoom.getName()).isEqualTo(newName);
        }
    }

    @Nested
    @DisplayName("공지사항 관리 시")
    class Announcement {

        @Test
        @DisplayName("공지사항을 설정할 수 있다")
        void should_SetAnnouncement_when_ValidAnnouncementProvided() {
            // given
            ChatRoom chatRoom = ChatRoom.builder()
                    .type(ChatRoom.ChatRoomType.GROUP)
                    .name("채팅방")
                    .build();
            String announcement = "새 공지사항입니다.";

            // when
            chatRoom.setAnnouncement(announcement);

            // then
            assertThat(chatRoom.getAnnouncement()).isEqualTo(announcement);
        }

        @Test
        @DisplayName("공지사항을 삭제할 수 있다")
        void should_ClearAnnouncement_when_ClearAnnouncementCalled() {
            // given
            ChatRoom chatRoom = ChatRoom.builder()
                    .type(ChatRoom.ChatRoomType.GROUP)
                    .name("채팅방")
                    .announcement("기존 공지사항")
                    .build();

            // when
            chatRoom.clearAnnouncement();

            // then
            assertThat(chatRoom.getAnnouncement()).isNull();
        }
    }

    @Nested
    @DisplayName("채팅방 타입 확인 시")
    class ChatRoomTypeCheck {

        @Test
        @DisplayName("1:1 채팅방인지 확인할 수 있다")
        void should_ReturnTrue_when_ChatRoomIsDirect() {
            // given
            ChatRoom directChatRoom = ChatRoom.builder()
                    .type(ChatRoom.ChatRoomType.DIRECT)
                    .build();

            ChatRoom groupChatRoom = ChatRoom.builder()
                    .type(ChatRoom.ChatRoomType.GROUP)
                    .name("그룹 채팅방")
                    .build();

            // when & then
            assertThat(directChatRoom.isDirectChat()).isTrue();
            assertThat(groupChatRoom.isDirectChat()).isFalse();
        }

        @Test
        @DisplayName("그룹 채팅방인지 확인할 수 있다")
        void should_ReturnTrue_when_ChatRoomIsGroup() {
            // given
            ChatRoom groupChatRoom = ChatRoom.builder()
                    .type(ChatRoom.ChatRoomType.GROUP)
                    .name("그룹 채팅방")
                    .build();

            ChatRoom directChatRoom = ChatRoom.builder()
                    .type(ChatRoom.ChatRoomType.DIRECT)
                    .build();

            // when & then
            assertThat(groupChatRoom.isGroupChat()).isTrue();
            assertThat(directChatRoom.isGroupChat()).isFalse();
        }
    }

    @Nested
    @DisplayName("이름 필수 여부 확인 시")
    class RequiresName {

        @Test
        @DisplayName("그룹 채팅방은 이름이 필수이다")
        void should_ReturnTrue_when_GroupChatRoom() {
            // given
            ChatRoom groupChatRoom = ChatRoom.builder()
                    .type(ChatRoom.ChatRoomType.GROUP)
                    .name("그룹 채팅방")
                    .build();

            ChatRoom directChatRoom = ChatRoom.builder()
                    .type(ChatRoom.ChatRoomType.DIRECT)
                    .build();

            // when & then
            assertThat(groupChatRoom.requiresName()).isTrue();
            assertThat(directChatRoom.requiresName()).isFalse();
        }
    }

    @Nested
    @DisplayName("표시 이름 반환 시")
    class GetDisplayName {

        @Test
        @DisplayName("채팅방 이름을 반환한다")
        void should_ReturnName_when_NameExists() {
            // given
            String roomName = "개발팀 채팅방";
            ChatRoom chatRoom = ChatRoom.builder()
                    .type(ChatRoom.ChatRoomType.GROUP)
                    .name(roomName)
                    .build();

            // when & then
            assertThat(chatRoom.getDisplayName()).isEqualTo(roomName);
        }

        @Test
        @DisplayName("이름이 없으면 null을 반환한다")
        void should_ReturnNull_when_NameNotExists() {
            // given
            ChatRoom chatRoom = ChatRoom.builder()
                    .type(ChatRoom.ChatRoomType.DIRECT)
                    .build();

            // when & then
            assertThat(chatRoom.getDisplayName()).isNull();
        }
    }

    @Nested
    @DisplayName("이름 유효성 검증 시")
    class ValidateName {

        @Test
        @DisplayName("1:1 채팅방은 이름이 없어도 유효하다")
        void should_ReturnTrue_when_DirectChatRoomWithoutName() {
            // given
            ChatRoom directChatRoom = ChatRoom.builder()
                    .type(ChatRoom.ChatRoomType.DIRECT)
                    .build();

            // when & then
            assertThat(directChatRoom.isValidName()).isTrue();
        }

        @Test
        @DisplayName("그룹 채팅방은 이름이 있으면 유효하다")
        void should_ReturnTrue_when_GroupChatRoomWithName() {
            // given
            ChatRoom groupChatRoom = ChatRoom.builder()
                    .type(ChatRoom.ChatRoomType.GROUP)
                    .name("그룹 채팅방")
                    .build();

            // when & then
            assertThat(groupChatRoom.isValidName()).isTrue();
        }

        @Test
        @DisplayName("그룹 채팅방은 이름이 없으면 유효하지 않다")
        void should_ReturnFalse_when_GroupChatRoomWithoutName() {
            // given
            ChatRoom groupChatRoom = ChatRoom.builder()
                    .type(ChatRoom.ChatRoomType.GROUP)
                    .name(null)
                    .build();

            // when & then
            assertThat(groupChatRoom.isValidName()).isFalse();
        }

        @Test
        @DisplayName("그룹 채팅방은 빈 이름이면 유효하지 않다")
        void should_ReturnFalse_when_GroupChatRoomWithEmptyName() {
            // given
            ChatRoom groupChatRoom = ChatRoom.builder()
                    .type(ChatRoom.ChatRoomType.GROUP)
                    .name("   ")
                    .build();

            // when & then
            assertThat(groupChatRoom.isValidName()).isFalse();
        }
    }
}
