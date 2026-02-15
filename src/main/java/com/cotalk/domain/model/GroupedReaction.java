package com.cotalk.domain.model;

import com.cotalk.domain.entity.Emoji;

import java.util.List;

/**
 * 이모지별로 그룹핑된 메시지 반응 도메인 모델.
 * 같은 이모지를 누른 사용자들의 정보를 담는다.
 *
 * @param emoji              이모지 문자
 * @param emojiCharacter     이모지 문자 (동일)
 * @param emojiName          이모지 이름
 * @param count              반응한 사용자 수
 * @param userIds            반응한 사용자 ID 목록
 * @param currentUserReacted 현재 사용자가 이 이모지를 눌렀는지 여부
 * @author seunggu.lee
 */
public record GroupedReaction(
        String emoji,
        String emojiCharacter,
        String emojiName,
        int count,
        List<Long> userIds,
        boolean currentUserReacted
) {

    /**
     * Emoji enum과 사용자 목록으로부터 그룹핑된 반응을 생성한다.
     *
     * @param emoji         이모지 enum
     * @param userIds       반응한 사용자 ID 목록
     * @param currentUserId 현재 사용자 ID (null이면 currentUserReacted는 false)
     * @return GroupedReaction 인스턴스
     */
    public static GroupedReaction of(Emoji emoji, List<Long> userIds, Long currentUserId) {
        return new GroupedReaction(
                emoji.getCharacter(),
                emoji.getCharacter(),
                emoji.getName(),
                userIds.size(),
                userIds,
                currentUserId != null && userIds.contains(currentUserId)
        );
    }
}
