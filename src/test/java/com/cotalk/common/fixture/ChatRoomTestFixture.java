package com.cotalk.common.fixture;

import com.cotalk.domain.entity.ChatRoom;
import com.cotalk.domain.entity.ChatRoomMember;

import java.time.LocalDateTime;

/**
 * ChatRoom 및 ChatRoomMember 엔티티 테스트 픽스처
 * 테스트에서 반복적으로 사용되는 ChatRoom 관련 객체 생성 메서드를 제공합니다.
 */
public class ChatRoomTestFixture {

    private static final String DEFAULT_ROOM_NAME = "테스트 채팅방";

    /**
     * 기본값으로 DIRECT 타입 ChatRoom 객체를 생성합니다.
     */
    public static ChatRoom createChatRoom() {
        return createChatRoom(1L, ChatRoom.ChatRoomType.DIRECT);
    }

    /**
     * 지정된 ID로 DIRECT 타입 ChatRoom 객체를 생성합니다.
     */
    public static ChatRoom createChatRoom(Long roomId) {
        return createChatRoom(roomId, ChatRoom.ChatRoomType.DIRECT);
    }

    /**
     * ID와 타입을 지정하여 ChatRoom 객체를 생성합니다.
     */
    public static ChatRoom createChatRoom(Long roomId, ChatRoom.ChatRoomType type) {
        return ChatRoom.builder()
                .id(roomId)
                .name(type == ChatRoom.ChatRoomType.DIRECT ? null : DEFAULT_ROOM_NAME)
                .type(type)
                .build();
    }

    /**
     * 1:1 채팅방을 생성합니다.
     */
    public static ChatRoom createDirectChatRoom(Long roomId) {
        return createChatRoom(roomId, ChatRoom.ChatRoomType.DIRECT);
    }

    /**
     * 그룹 채팅방을 생성합니다.
     */
    public static ChatRoom createGroupChatRoom(Long roomId, String roomName) {
        return ChatRoom.builder()
                .id(roomId)
                .name(roomName)
                .type(ChatRoom.ChatRoomType.GROUP)
                .build();
    }

    /**
     * ChatRoomMember 객체를 생성합니다.
     */
    public static ChatRoomMember createChatRoomMember(Long memberId, Long roomId, Long userId) {
        return ChatRoomMember.builder()
                .id(memberId)
                .chatRoomId(roomId)
                .userId(userId)
                .lastReadAt(LocalDateTime.now().minusMinutes(5))
                .build();
    }

    /**
     * 여러 ChatRoomMember 객체를 생성합니다.
     */
    public static ChatRoomMember[] createChatRoomMembers(Long roomId, Long... userIds) {
        ChatRoomMember[] members = new ChatRoomMember[userIds.length];
        for (int i = 0; i < userIds.length; i++) {
            members[i] = createChatRoomMember((long) (i + 1), roomId, userIds[i]);
        }
        return members;
    }

    /**
     * 빌더 스타일로 ChatRoom 생성을 시작합니다.
     */
    public static ChatRoomBuilder builder() {
        return new ChatRoomBuilder();
    }

    public static class ChatRoomBuilder {
        private Long id = 1L;
        private String name = DEFAULT_ROOM_NAME;
        private ChatRoom.ChatRoomType type = ChatRoom.ChatRoomType.DIRECT;

        public ChatRoomBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public ChatRoomBuilder name(String name) {
            this.name = name;
            return this;
        }

        public ChatRoomBuilder type(ChatRoom.ChatRoomType type) {
            this.type = type;
            return this;
        }

        public ChatRoom build() {
            return ChatRoom.builder()
                    .id(id)
                    .name(type == ChatRoom.ChatRoomType.DIRECT ? null : name)
                    .type(type)
                    .build();
        }
    }

    /**
     * 빌더 스타일로 ChatRoomMember 생성을 시작합니다.
     */
    public static ChatRoomMemberBuilder memberBuilder() {
        return new ChatRoomMemberBuilder();
    }

    public static class ChatRoomMemberBuilder {
        private Long id = 1L;
        private Long chatRoomId = 1L;
        private Long userId = 1L;
        private LocalDateTime lastReadAt = LocalDateTime.now().minusMinutes(5);

        public ChatRoomMemberBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public ChatRoomMemberBuilder chatRoomId(Long chatRoomId) {
            this.chatRoomId = chatRoomId;
            return this;
        }

        public ChatRoomMemberBuilder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public ChatRoomMemberBuilder lastReadAt(LocalDateTime lastReadAt) {
            this.lastReadAt = lastReadAt;
            return this;
        }

        public ChatRoomMember build() {
            return ChatRoomMember.builder()
                    .id(id)
                    .chatRoomId(chatRoomId)
                    .userId(userId)
                    .lastReadAt(lastReadAt)
                    .build();
        }
    }
}
