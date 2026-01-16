package com.cotalk.domain.port.inbound;

import com.cotalk.domain.entity.Message;

/**
 * 메시지 수정 유즈케이스
 */
public interface UpdateMessageUseCase {

    /**
     * 메시지 수정
     *
     * @param messageId 메시지 ID
     * @param userId 사용자 ID (본인 확인용)
     * @param newContent 새 내용
     * @return 수정된 메시지
     */
    Message updateMessage(Long messageId, Long userId, String newContent);
}
