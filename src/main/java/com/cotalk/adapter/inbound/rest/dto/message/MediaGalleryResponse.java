package com.cotalk.adapter.inbound.rest.dto.message;

import java.util.List;

/**
 * 미디어 갤러리 조회 응답 DTO.
 * 채팅방의 사진, 파일, 링크 목록을 페이징하여 반환한다.
 *
 * @param items 미디어 아이템 목록
 * @param nextCursor 다음 페이지 커서 (마지막 메시지 ID)
 * @param hasMore 다음 페이지 존재 여부
 * @author seunggu.lee
 */
public record MediaGalleryResponse(
        List<MediaGalleryItem> items,
        Long nextCursor,
        boolean hasMore
) {
    /**
     * 미디어 갤러리 개별 아이템.
     *
     * @param messageId 메시지 ID
     * @param type 메시지 유형 (IMAGE, FILE, TEXT)
     * @param fileUrl 파일 URL
     * @param fileName 파일 이름
     * @param fileSize 파일 크기
     * @param contentType 파일 MIME 타입
     * @param thumbnailUrl 썸네일 URL
     * @param linkPreviewUrl 링크 프리뷰 URL (링크 탭용)
     * @param linkPreviewTitle 링크 제목
     * @param linkPreviewDescription 링크 설명
     * @param linkPreviewImageUrl 링크 대표 이미지
     * @param createdAt 생성 시간 (epoch millis)
     * @param senderId 발신자 ID
     * @param senderNickname 발신자 닉네임
     */
    public record MediaGalleryItem(
            Long messageId,
            String type,
            String fileUrl,
            String fileName,
            Long fileSize,
            String contentType,
            String thumbnailUrl,
            String linkPreviewUrl,
            String linkPreviewTitle,
            String linkPreviewDescription,
            String linkPreviewImageUrl,
            Long createdAt,
            Long senderId,
            String senderNickname
    ) {}
}
