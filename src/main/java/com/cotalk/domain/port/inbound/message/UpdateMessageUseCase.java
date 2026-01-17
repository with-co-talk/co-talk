package com.cotalk.domain.port.inbound.message;

import com.cotalk.domain.entity.Message;

/**
 * 메시지 수정 유스케이스.
 * 본인이 작성한 메시지를 수정한다.
 *
 * @author seunggu.lee
 */
public interface UpdateMessageUseCase {

    /**
     * 메시지를 수정한다.
     *
     * @param messageId 메시지 ID
     * @param userId 사용자 ID (본인 확인용)
     * @param newContent 새 내용
     * @return 수정된 메시지
     */
    Message updateMessage(Long messageId, Long userId, String newContent);
}
