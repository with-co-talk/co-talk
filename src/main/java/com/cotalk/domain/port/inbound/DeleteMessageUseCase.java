package com.cotalk.domain.port.inbound;

/**
 * 메시지 삭제 유즈케이스
 */
public interface DeleteMessageUseCase {

    /**
     * 메시지 삭제 (소프트 삭제)
     *
     * @param messageId 메시지 ID
     * @param userId 사용자 ID (본인 확인용)
     */
    void deleteMessage(Long messageId, Long userId);
}
