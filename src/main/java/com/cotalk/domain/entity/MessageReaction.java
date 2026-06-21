package com.cotalk.domain.entity;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 메시지 반응 도메인 엔티티.
 * 메시지에 대한 이모지 반응 정보를 나타낸다.
 * 순수 도메인 모델이며 JPA 어노테이션은 persistence 계층에만 존재한다.
 *
 * @author seunggu.lee
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@SuperBuilder
public class MessageReaction extends DomainBaseEntity {

    private Long id;

    private Long messageId;

    private Long userId;

    private Emoji emoji;

    /**
     * 메시지 반응을 생성한다.
     *
     * @param messageId 메시지 ID
     * @param userId 사용자 ID
     * @param emoji 이모지
     * @return 생성된 MessageReaction 인스턴스
     */
    public static MessageReaction create(Long messageId, Long userId, Emoji emoji) {
        return MessageReaction.builder()
                .messageId(messageId)
                .userId(userId)
                .emoji(emoji)
                .build();
    }

    /**
     * 지정된 사용자가 남긴 반응인지 확인한다.
     *
     * @param userId 확인할 사용자 ID
     * @return 해당 사용자의 반응이면 true, 그렇지 않으면 false
     */
    public boolean isFromUser(Long userId) {
        return this.userId.equals(userId);
    }
}
