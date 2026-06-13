package com.cotalk.adapter.inbound.rest.dto.message;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 파일 메시지 전송 요청 DTO.
 * <p>
 * 두 가지 방식을 모두 수용한다(하위호환).
 * <ul>
 *   <li><b>신규(권장)</b>: {@code objectId}(업로드 응답이 발급한 불투명 저장 객체 키)를 보낸다.
 *       서버가 소유·존재를 검증하고 {@code fileUrl}/{@code contentType}/{@code fileSize}를 재구성한다.</li>
 *   <li><b>기존</b>: {@code fileUrl}/{@code contentType}을 직접 보낸다(서버사이드 화이트리스트 검증).</li>
 * </ul>
 * {@code objectId} 또는 {@code fileUrl} 중 적어도 하나는 반드시 있어야 한다.
 * </p>
 *
 * @param chatRoomId        채팅방 ID
 * @param objectId          불투명 저장 객체 키(신규 방식, 선택)
 * @param thumbnailObjectId 썸네일 불투명 저장 객체 키(신규 방식, 선택)
 * @param fileUrl           파일 URL(기존 방식, 선택)
 * @param fileName          파일명
 * @param fileSize          파일 크기 (bytes)
 * @param contentType       파일 MIME 타입 (기존 방식 필수, 신규 방식에서는 저장소 메타 부재 시 폴백 힌트)
 * @param thumbnailUrl      썸네일 URL (이미지인 경우; 기존 방식)
 * @author seunggu.lee
 */
public record SendFileMessageRequest(
        @NotNull(message = "채팅방 ID는 필수입니다.")
        Long chatRoomId,

        String objectId,

        String thumbnailObjectId,

        String fileUrl,

        @NotBlank(message = "파일명은 필수입니다.")
        String fileName,

        @NotNull(message = "파일 크기는 필수입니다.")
        Long fileSize,

        String contentType,

        String thumbnailUrl  // 선택 (이미지인 경우)
) {

    /**
     * {@code objectId} 또는 {@code fileUrl} 중 하나는 반드시 제공되어야 한다.
     *
     * @return 둘 중 하나라도 존재하면 true
     */
    @AssertTrue(message = "objectId 또는 fileUrl 중 하나는 필수입니다.")
    public boolean isFileSourceProvided() {
        return (objectId != null && !objectId.isBlank())
                || (fileUrl != null && !fileUrl.isBlank());
    }

    /**
     * 기존 방식(fileUrl) 파일 메시지 전송 요청을 생성합니다(하위호환 팩토리).
     *
     * @param chatRoomId   채팅방 ID
     * @param fileUrl      파일 URL
     * @param fileName     파일명
     * @param fileSize     파일 크기
     * @param contentType  파일 MIME 타입
     * @param thumbnailUrl 썸네일 URL
     * @return SendFileMessageRequest 인스턴스
     */
    public static SendFileMessageRequest of(Long chatRoomId, String fileUrl,
                                             String fileName, Long fileSize, String contentType,
                                             String thumbnailUrl) {
        return new SendFileMessageRequest(chatRoomId, null, null, fileUrl, fileName, fileSize, contentType, thumbnailUrl);
    }
}
