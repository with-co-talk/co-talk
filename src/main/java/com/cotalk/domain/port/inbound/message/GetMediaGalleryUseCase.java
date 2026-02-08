package com.cotalk.domain.port.inbound.message;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 미디어 갤러리 조회 유스케이스.
 * 채팅방의 사진, 파일, 링크를 타입별로 페이징하여 조회한다.
 *
 * @author seunggu.lee
 */
public interface GetMediaGalleryUseCase {

    /**
     * 채팅방의 미디어 갤러리를 조회한다.
     *
     * @param chatRoomId 채팅방 ID
     * @param userId 사용자 ID (권한 확인용)
     * @param type 미디어 유형 (PHOTO, FILE, LINK)
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @return 미디어 갤러리 조회 결과
     */
    MediaGalleryResult getMediaGallery(Long chatRoomId, Long userId, String type, int page, int size);

    /**
     * 미디어 갤러리 개별 아이템.
     *
     * @param messageId 메시지 ID
     * @param type 메시지 유형
     * @param fileUrl 파일 URL
     * @param fileName 파일 이름
     * @param fileSize 파일 크기
     * @param fileContentType 파일 MIME 타입
     * @param thumbnailUrl 썸네일 URL
     * @param linkPreviewUrl 링크 프리뷰 URL
     * @param linkPreviewTitle 링크 제목
     * @param linkPreviewDescription 링크 설명
     * @param linkPreviewImageUrl 링크 대표 이미지
     * @param createdAtMillis 생성 시간 (epoch millis)
     * @param senderId 발신자 ID
     * @param senderNickname 발신자 닉네임
     */
    record MediaGalleryItem(
            Long messageId,
            String type,
            String fileUrl,
            String fileName,
            Long fileSize,
            String fileContentType,
            String thumbnailUrl,
            String linkPreviewUrl,
            String linkPreviewTitle,
            String linkPreviewDescription,
            String linkPreviewImageUrl,
            Long createdAtMillis,
            Long senderId,
            String senderNickname
    ) {}

    /**
     * 미디어 갤러리 조회 결과.
     *
     * @param items 미디어 아이템 목록
     * @param nextCursor 다음 페이지 커서 (마지막 메시지 ID)
     * @param hasMore 다음 페이지 존재 여부
     */
    record MediaGalleryResult(
            List<MediaGalleryItem> items,
            Long nextCursor,
            boolean hasMore
    ) {}
}
