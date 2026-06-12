package com.cotalk.domain.port.outbound;

import com.cotalk.domain.entity.Emoji;
import com.cotalk.domain.entity.MessageReaction;

import java.util.List;
import java.util.Optional;

/**
 * 메시지 반응 레포지토리 포트.
 * 메시지에 대한 이모지 반응 데이터 저장 및 조회를 위한 인터페이스를 정의한다.
 *
 * @author seunggu.lee
 */
public interface MessageReactionRepository {

    /**
     * 메시지 반응을 저장한다.
     *
     * @param reaction 저장할 메시지 반응
     * @return 저장된 메시지 반응
     */
    MessageReaction save(MessageReaction reaction);

    /**
     * 메시지 ID, 사용자 ID, 이모지로 메시지 반응을 조회한다.
     *
     * @param messageId 메시지 ID
     * @param userId    사용자 ID
     * @param emoji     이모지
     * @return 조회된 메시지 반응 (Optional)
     */
    Optional<MessageReaction> findByMessageIdAndUserIdAndEmoji(Long messageId, Long userId, Emoji emoji);

    /**
     * 특정 메시지의 모든 반응을 조회한다.
     *
     * @param messageId 메시지 ID
     * @return 메시지 반응 목록
     */
    List<MessageReaction> findByMessageId(Long messageId);

    /**
     * 메시지 반응을 삭제한다.
     *
     * @param reaction 삭제할 메시지 반응
     */
    void delete(MessageReaction reaction);

    /**
     * 특정 메시지의 모든 반응을 삭제한다.
     *
     * @param messageId 메시지 ID
     */
    void deleteByMessageId(Long messageId);

    /**
     * 특정 사용자가 남긴 모든 반응을 삭제한다.
     * 회원 탈퇴 시 사용자가 남긴 메시지 반응을 정리하는 데 사용한다.
     *
     * @param userId 사용자 ID
     */
    void deleteByUserId(Long userId);

    /**
     * 특정 발신자가 보낸 메시지에 달린 모든 반응을 삭제한다.
     * 회원 탈퇴로 해당 사용자의 메시지를 삭제하기 전에,
     * 타인이 그 메시지에 남긴 반응을 먼저 정리하는 데 사용한다.
     *
     * @param senderId 메시지 발신자(사용자) ID
     */
    void deleteByMessageSenderId(Long senderId);
}
