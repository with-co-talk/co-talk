package com.cotalk.common.fixture;

import com.cotalk.domain.entity.ChatRoomMember;

import java.time.LocalDateTime;

/**
 * ChatRoomMember 엔티티 테스트 픽스처.
 * 테스트에서 반복적으로 사용되는 ChatRoomMember 객체 생성 메서드를 제공한다.
 *
 * @author seunggu.lee
 */
public class ChatRoomMemberTestFixture {

    private static final Long DEFAULT_ID = 1L;
    private static final Long DEFAULT_CHAT_ROOM_ID = 1L;
    private static final Long DEFAULT_USER_ID = 1L;

    /**
     * 기본값(MEMBER 역할)으로 ChatRoomMember 객체를 생성한다.
     *
     * @return 일반 멤버 역할의 ChatRoomMember 엔티티
     */
    public static ChatRoomMember createMember() {
        return createMember(DEFAULT_ID, DEFAULT_CHAT_ROOM_ID, DEFAULT_USER_ID);
    }

    /**
     * 지정된 파라미터로 일반 멤버 ChatRoomMember 객체를 생성한다.
     *
     * @param id         멤버 ID
     * @param chatRoomId 채팅방 ID
     * @param userId     사용자 ID
     * @return 일반 멤버 역할의 ChatRoomMember 엔티티
     */
    public static ChatRoomMember createMember(Long id, Long chatRoomId, Long userId) {
        return ChatRoomMember.builder()
                .id(id)
                .chatRoomId(chatRoomId)
                .userId(userId)
                .lastReadAt(LocalDateTime.now().minusMinutes(5))
                .build();
    }

    /**
     * 관리자(ADMIN) 역할의 ChatRoomMember 객체를 생성한다.
     *
     * @param id         멤버 ID
     * @param chatRoomId 채팅방 ID
     * @param userId     사용자 ID
     * @return 관리자 역할의 ChatRoomMember 엔티티
     */
    public static ChatRoomMember createAdmin(Long id, Long chatRoomId, Long userId) {
        return ChatRoomMember.builder()
                .id(id)
                .chatRoomId(chatRoomId)
                .userId(userId)
                .role(ChatRoomMember.MemberRole.ADMIN)
                .lastReadAt(LocalDateTime.now().minusMinutes(5))
                .build();
    }

    /**
     * 채팅방의 멤버 여러 명을 생성한다.
     *
     * @param chatRoomId 채팅방 ID
     * @param userIds    사용자 ID 목록
     * @return ChatRoomMember 배열
     */
    public static ChatRoomMember[] createMembers(Long chatRoomId, Long... userIds) {
        ChatRoomMember[] members = new ChatRoomMember[userIds.length];
        for (int i = 0; i < userIds.length; i++) {
            members[i] = createMember((long) (i + 1), chatRoomId, userIds[i]);
        }
        return members;
    }

    /**
     * 빌더 스타일로 ChatRoomMember 생성을 시작한다.
     *
     * @return ChatRoomMemberBuilder 인스턴스
     */
    public static ChatRoomMemberBuilder builder() {
        return new ChatRoomMemberBuilder();
    }

    /**
     * ChatRoomMember 테스트 빌더.
     */
    public static class ChatRoomMemberBuilder {
        private Long id = DEFAULT_ID;
        private Long chatRoomId = DEFAULT_CHAT_ROOM_ID;
        private Long userId = DEFAULT_USER_ID;
        private ChatRoomMember.MemberRole role = ChatRoomMember.MemberRole.MEMBER;
        private LocalDateTime lastReadAt = LocalDateTime.now().minusMinutes(5);
        private Long lastReadMessageId = null;

        /**
         * 멤버 ID를 설정한다.
         *
         * @param id 멤버 ID
         * @return 빌더
         */
        public ChatRoomMemberBuilder id(Long id) {
            this.id = id;
            return this;
        }

        /**
         * 채팅방 ID를 설정한다.
         *
         * @param chatRoomId 채팅방 ID
         * @return 빌더
         */
        public ChatRoomMemberBuilder chatRoomId(Long chatRoomId) {
            this.chatRoomId = chatRoomId;
            return this;
        }

        /**
         * 사용자 ID를 설정한다.
         *
         * @param userId 사용자 ID
         * @return 빌더
         */
        public ChatRoomMemberBuilder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        /**
         * 멤버 역할을 설정한다.
         *
         * @param role 멤버 역할
         * @return 빌더
         */
        public ChatRoomMemberBuilder role(ChatRoomMember.MemberRole role) {
            this.role = role;
            return this;
        }

        /**
         * 마지막 읽은 시간을 설정한다.
         *
         * @param lastReadAt 마지막 읽은 시간
         * @return 빌더
         */
        public ChatRoomMemberBuilder lastReadAt(LocalDateTime lastReadAt) {
            this.lastReadAt = lastReadAt;
            return this;
        }

        /**
         * 마지막 읽은 메시지 ID를 설정한다.
         *
         * @param lastReadMessageId 마지막 읽은 메시지 ID
         * @return 빌더
         */
        public ChatRoomMemberBuilder lastReadMessageId(Long lastReadMessageId) {
            this.lastReadMessageId = lastReadMessageId;
            return this;
        }

        /**
         * ChatRoomMember 객체를 생성한다.
         *
         * @return 생성된 ChatRoomMember 엔티티
         */
        public ChatRoomMember build() {
            return ChatRoomMember.builder()
                    .id(id)
                    .chatRoomId(chatRoomId)
                    .userId(userId)
                    .role(role)
                    .lastReadAt(lastReadAt)
                    .lastReadMessageId(lastReadMessageId)
                    .build();
        }
    }
}
