package com.cotalk.adapter.inbound.websocket.dto;

/**
 * 파일 첨부 메시지 전송 요청 DTO.
 * WebSocket을 통해 클라이언트로부터 수신되는 파일 메시지 요청입니다.
 * <p>
 * 하위호환을 위해 두 방식을 모두 수용한다. {@code objectId}(업로드가 발급한 불투명 저장 객체 키)를
 * 보내면 서버가 소유·존재를 검증하고 URL/메타를 재구성한다. {@code objectId}가 없으면 {@code fileUrl}을
 * 직접 받아 서버사이드 화이트리스트 검증을 수행한다.
 * </p>
 *
 * @param roomId            채팅방 ID
 * @param objectId          불투명 저장 객체 키(신규 방식, 선택)
 * @param thumbnailObjectId 썸네일 불투명 저장 객체 키(신규 방식, 선택)
 * @param fileUrl           업로드된 파일의 URL(기존 방식, 선택)
 * @param fileName          파일명
 * @param fileSize          파일 크기 (바이트)
 * @param contentType       파일의 MIME 타입
 * @param thumbnailUrl      썸네일 이미지 URL (이미지/동영상 파일인 경우; 기존 방식)
 * @author seunggu.lee
 */
public record FileMessageRequest(
        Long roomId,
        String objectId,
        String thumbnailObjectId,
        String fileUrl,
        String fileName,
        Long fileSize,
        String contentType,
        String thumbnailUrl
) {

    /**
     * 불투명 식별자(object-id) 방식 여부.
     *
     * @return {@code objectId}가 존재하면 true
     */
    public boolean usesObjectId() {
        return hasText(objectId);
    }

    /**
     * 문자열이 null/공백이 아닌 실제 값을 가지는지 확인한다.
     *
     * @param value 검사할 문자열
     * @return null도 공백도 아니면 true
     */
    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
