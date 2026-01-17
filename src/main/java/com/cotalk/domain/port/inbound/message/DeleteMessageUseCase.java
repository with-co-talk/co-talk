package com.cotalk.domain.port.inbound.message;

/**
 * 메시지 삭제 유스케이스.
 * 본인이 작성한 메시지를 소프트 삭제한다.
 *
 * @author seunggu.lee
 */
public interface DeleteMessageUseCase {

    /**
     * 메시지를 삭제한다. (소프트 삭제)
     *
     * @param messageId 메시지 ID
     * @param userId 사용자 ID (본인 확인용)
     */
    void deleteMessage(Long messageId, Long userId);
}
