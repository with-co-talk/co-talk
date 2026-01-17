package com.cotalk.common.fixture;

import com.cotalk.domain.entity.Message;

import java.time.LocalDateTime;

/**
 * Message 엔티티 테스트 픽스처
 * 테스트에서 반복적으로 사용되는 Message 객체 생성 메서드를 제공합니다.
 */
public class MessageTestFixture {

    private static final String DEFAULT_CONTENT = "테스트 메시지 내용";
    private static final Long DEFAULT_CHAT_ROOM_ID = 100L;
    private static final Long DEFAULT_SENDER_ID = 1L;

    /**
     * 기본값으로 TEXT 타입 Message 객체를 생성합니다.
     */
    public static Message createMessage() {
        return createMessage(1L);
    }

    /**
     * 지정된 ID로 TEXT 타입 Message 객체를 생성합니다.
     */
    public static Message createMessage(Long messageId) {
        return createMessage(messageId, DEFAULT_CHAT_ROOM_ID, DEFAULT_SENDER_ID, DEFAULT_CONTENT);
    }

    /**
     * ID, 채팅방 ID, 발신자 ID, 내용을 지정하여 Message 객체를 생성합니다.
     */
    public static Message createMessage(Long messageId, Long chatRoomId, Long senderId, String content) {
        return Message.builder()
                .id(messageId)
                .chatRoomId(chatRoomId)
                .senderId(senderId)
                .content(content)
                .type(Message.MessageType.TEXT)
                .build();
    }

    /**
     * TEXT 타입 메시지를 생성합니다.
     */
    public static Message createTextMessage(Long messageId, Long chatRoomId, Long senderId) {
        return createMessage(messageId, chatRoomId, senderId, DEFAULT_CONTENT);
    }

    /**
     * 이미지 타입 메시지를 생성합니다.
     */
    public static Message createImageMessage(Long messageId, Long chatRoomId, Long senderId, String fileUrl) {
        return Message.builder()
                .id(messageId)
                .chatRoomId(chatRoomId)
                .senderId(senderId)
                .content("[이미지]")
                .type(Message.MessageType.IMAGE)
                .fileUrl(fileUrl)
                .fileName("image.jpg")
                .fileSize(102400L)
                .fileContentType("image/jpeg")
                .thumbnailUrl(fileUrl + "/thumbnail")
                .build();
    }

    /**
     * 파일 타입 메시지를 생성합니다.
     */
    public static Message createFileMessage(Long messageId, Long chatRoomId, Long senderId, String fileUrl, String fileName) {
        return Message.builder()
                .id(messageId)
                .chatRoomId(chatRoomId)
                .senderId(senderId)
                .content("[파일]")
                .type(Message.MessageType.FILE)
                .fileUrl(fileUrl)
                .fileName(fileName)
                .fileSize(204800L)
                .fileContentType("application/pdf")
                .build();
    }

    /**
     * 삭제된 메시지를 생성합니다.
     */
    public static Message createDeletedMessage(Long messageId, Long chatRoomId, Long senderId) {
        return Message.builder()
                .id(messageId)
                .chatRoomId(chatRoomId)
                .senderId(senderId)
                .content("삭제된 메시지입니다.")
                .type(Message.MessageType.TEXT)
                .deleted(true)
                .build();
    }

    /**
     * 여러 Message 객체를 생성합니다.
     */
    public static Message[] createMessages(int count, Long chatRoomId, Long senderId) {
        Message[] messages = new Message[count];
        for (int i = 0; i < count; i++) {
            messages[i] = createMessage(
                    (long) (i + 1),
                    chatRoomId,
                    senderId,
                    "메시지 " + (i + 1)
            );
        }
        return messages;
    }

    /**
     * 빌더 스타일로 Message 생성을 시작합니다.
     */
    public static MessageBuilder builder() {
        return new MessageBuilder();
    }

    public static class MessageBuilder {
        private Long id = 1L;
        private Long chatRoomId = DEFAULT_CHAT_ROOM_ID;
        private Long senderId = DEFAULT_SENDER_ID;
        private String content = DEFAULT_CONTENT;
        private Message.MessageType type = Message.MessageType.TEXT;
        private String fileUrl = null;
        private String fileName = null;
        private Long fileSize = null;
        private String fileContentType = null;
        private String thumbnailUrl = null;
        private boolean deleted = false;

        public MessageBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public MessageBuilder chatRoomId(Long chatRoomId) {
            this.chatRoomId = chatRoomId;
            return this;
        }

        public MessageBuilder senderId(Long senderId) {
            this.senderId = senderId;
            return this;
        }

        public MessageBuilder content(String content) {
            this.content = content;
            return this;
        }

        public MessageBuilder type(Message.MessageType type) {
            this.type = type;
            return this;
        }

        public MessageBuilder fileUrl(String fileUrl) {
            this.fileUrl = fileUrl;
            return this;
        }

        public MessageBuilder fileName(String fileName) {
            this.fileName = fileName;
            return this;
        }

        public MessageBuilder fileSize(Long fileSize) {
            this.fileSize = fileSize;
            return this;
        }

        public MessageBuilder fileContentType(String fileContentType) {
            this.fileContentType = fileContentType;
            return this;
        }

        public MessageBuilder thumbnailUrl(String thumbnailUrl) {
            this.thumbnailUrl = thumbnailUrl;
            return this;
        }

        public MessageBuilder deleted(boolean deleted) {
            this.deleted = deleted;
            return this;
        }

        public Message build() {
            return Message.builder()
                    .id(id)
                    .chatRoomId(chatRoomId)
                    .senderId(senderId)
                    .content(content)
                    .type(type)
                    .fileUrl(fileUrl)
                    .fileName(fileName)
                    .fileSize(fileSize)
                    .fileContentType(fileContentType)
                    .thumbnailUrl(thumbnailUrl)
                    .deleted(deleted)
                    .build();
        }
    }
}
