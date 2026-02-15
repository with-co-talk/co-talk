package com.cotalk.adapter.inbound.rest.dto.message;

import com.cotalk.domain.model.GroupedReaction;

import java.util.List;

/**
 * 그룹핑된 메시지 반응 응답 DTO.
 * 같은 이모지를 누른 사용자들을 그룹핑하여 반환한다.
 *
 * @param emoji              이모지
 * @param emojiCharacter     이모지 문자
 * @param emojiName          이모지 이름
 * @param count              반응한 사용자 수
 * @param userIds            반응한 사용자 ID 목록
 * @param currentUserReacted 현재 사용자가 이 이모지를 눌렀는지 여부
 * @author seunggu.lee
 */
public record GroupedReactionResponse(
        String emoji,
        String emojiCharacter,
        String emojiName,
        int count,
        List<Long> userIds,
        boolean currentUserReacted
) {
    /**
     * 도메인 모델로부터 응답 DTO를 생성한다.
     *
     * @param reaction 그룹핑된 반응 도메인 모델
     * @return GroupedReactionResponse 인스턴스
     */
    public static GroupedReactionResponse from(GroupedReaction reaction) {
        return new GroupedReactionResponse(
                reaction.emoji(),
                reaction.emojiCharacter(),
                reaction.emojiName(),
                reaction.count(),
                reaction.userIds(),
                reaction.currentUserReacted()
        );
    }
}
