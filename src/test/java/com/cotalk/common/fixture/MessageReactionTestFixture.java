package com.cotalk.common.fixture;

import com.cotalk.domain.entity.Emoji;
import com.cotalk.domain.entity.MessageReaction;

/**
 * MessageReaction 엔티티 테스트 픽스처.
 * 테스트에서 반복적으로 사용되는 MessageReaction 객체 생성 메서드를 제공한다.
 *
 * @author seunggu.lee
 */
public class MessageReactionTestFixture {

    private static final Long DEFAULT_ID = 1L;
    private static final Long DEFAULT_MESSAGE_ID = 100L;
    private static final Long DEFAULT_USER_ID = 1L;
    private static final Emoji DEFAULT_EMOJI = Emoji.THUMBS_UP;

    /**
     * 기본값(엄지척)으로 MessageReaction 객체를 생성한다.
     *
     * @return MessageReaction 엔티티
     */
    public static MessageReaction createReaction() {
        return createReaction(DEFAULT_ID, DEFAULT_MESSAGE_ID, DEFAULT_USER_ID, DEFAULT_EMOJI);
    }

    /**
     * 지정된 파라미터로 MessageReaction 객체를 생성한다.
     *
     * @param messageId 메시지 ID
     * @param userId    사용자 ID
     * @param emoji     이모지
     * @return MessageReaction 엔티티
     */
    public static MessageReaction createReaction(Long messageId, Long userId, Emoji emoji) {
        return createReaction(DEFAULT_ID, messageId, userId, emoji);
    }

    /**
     * 모든 파라미터를 지정하여 MessageReaction 객체를 생성한다.
     *
     * @param id        반응 ID
     * @param messageId 메시지 ID
     * @param userId    사용자 ID
     * @param emoji     이모지
     * @return MessageReaction 엔티티
     */
    public static MessageReaction createReaction(Long id, Long messageId, Long userId, Emoji emoji) {
        return MessageReaction.builder()
                .id(id)
                .messageId(messageId)
                .userId(userId)
                .emoji(emoji)
                .build();
    }

    /**
     * 하트 반응을 생성한다.
     *
     * @param messageId 메시지 ID
     * @param userId    사용자 ID
     * @return 하트 이모지의 MessageReaction 엔티티
     */
    public static MessageReaction createHeartReaction(Long messageId, Long userId) {
        return createReaction(DEFAULT_ID, messageId, userId, Emoji.HEART);
    }

    /**
     * 웃음 반응을 생성한다.
     *
     * @param messageId 메시지 ID
     * @param userId    사용자 ID
     * @return 웃음 이모지의 MessageReaction 엔티티
     */
    public static MessageReaction createLaughingReaction(Long messageId, Long userId) {
        return createReaction(DEFAULT_ID, messageId, userId, Emoji.LAUGHING);
    }

    /**
     * 빌더 스타일로 MessageReaction 생성을 시작한다.
     *
     * @return MessageReactionBuilder 인스턴스
     */
    public static MessageReactionBuilder builder() {
        return new MessageReactionBuilder();
    }

    /**
     * MessageReaction 테스트 빌더.
     */
    public static class MessageReactionBuilder {
        private Long id = DEFAULT_ID;
        private Long messageId = DEFAULT_MESSAGE_ID;
        private Long userId = DEFAULT_USER_ID;
        private Emoji emoji = DEFAULT_EMOJI;

        /**
         * 반응 ID를 설정한다.
         *
         * @param id 반응 ID
         * @return 빌더
         */
        public MessageReactionBuilder id(Long id) {
            this.id = id;
            return this;
        }

        /**
         * 메시지 ID를 설정한다.
         *
         * @param messageId 메시지 ID
         * @return 빌더
         */
        public MessageReactionBuilder messageId(Long messageId) {
            this.messageId = messageId;
            return this;
        }

        /**
         * 사용자 ID를 설정한다.
         *
         * @param userId 사용자 ID
         * @return 빌더
         */
        public MessageReactionBuilder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        /**
         * 이모지를 설정한다.
         *
         * @param emoji 이모지
         * @return 빌더
         */
        public MessageReactionBuilder emoji(Emoji emoji) {
            this.emoji = emoji;
            return this;
        }

        /**
         * MessageReaction 객체를 생성한다.
         *
         * @return 생성된 MessageReaction 엔티티
         */
        public MessageReaction build() {
            return MessageReaction.builder()
                    .id(id)
                    .messageId(messageId)
                    .userId(userId)
                    .emoji(emoji)
                    .build();
        }
    }
}
