package com.cotalk.domain.validator;

import com.cotalk.domain.exception.FileUploadException;
import com.cotalk.domain.service.UploadFileService;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * 파일 메시지 서버사이드 검증기.
 * <p>
 * 파일 메시지는 클라이언트가 보낸 {@code fileUrl}, {@code contentType} 등을
 * 그대로 신뢰하지 않고, 서버에서 다시 검증한다.
 * </p>
 * <ul>
 *   <li>{@code contentType}: {@link UploadFileService#ALLOWED_CONTENT_TYPES} 허용 목록 재검증</li>
 *   <li>{@code fileUrl}/{@code thumbnailUrl}: 본 서버가 업로드 시 생성하는
 *       스토리지 객체 경로({@code uploads/{senderId}/...})에 속하는지 검증하여
 *       외부 URL·경로 탈출(traversal)·타인 소유 경로를 거부한다.</li>
 * </ul>
 * <p>
 * 검증 기준은 {@code UploadFileService}가 생성하는 실제 저장 경로 규칙
 * ({@code uploads/{userId}/{uuid}.{ext}})에 근거한다. 저장소 백엔드(MinIO 공개 URL,
 * MinIO presigned URL, InMemory)마다 호스트는 다르지만, URL 경로에는 항상 이
 * 객체 키가 포함되므로 경로 세그먼트 기준으로 검증한다.
 * </p>
 *
 * @author seunggu.lee
 * @see UploadFileService
 */
public class FileMessageValidator {

    /**
     * 업로드 객체 키의 최상위 디렉터리.
     * {@code UploadFileService.generateStoragePath}가 생성하는 {@code uploads/{userId}/...} 규칙과 일치한다.
     */
    private static final String UPLOAD_ROOT = "uploads";

    /**
     * 파일 메시지의 contentType과 URL들을 서버사이드에서 검증한다.
     *
     * @param senderId     발신자(현재 인증된 사용자) ID
     * @param contentType  클라이언트가 보낸 MIME 타입
     * @param fileUrl      클라이언트가 보낸 파일 URL
     * @param thumbnailUrl 클라이언트가 보낸 썸네일 URL (선택, null 허용)
     * @throws FileUploadException 허용되지 않은 contentType이거나 신뢰할 수 없는 URL인 경우
     */
    public void validate(Long senderId, String contentType, String fileUrl, String thumbnailUrl) {
        validateContentType(contentType);
        validateOwnedUploadUrl(senderId, fileUrl);
        if (thumbnailUrl != null && !thumbnailUrl.isBlank()) {
            validateOwnedUploadUrl(senderId, thumbnailUrl);
        }
    }

    /**
     * contentType이 업로드 허용 목록에 포함되는지 검증한다.
     *
     * @param contentType 검증할 MIME 타입
     * @throws FileUploadException 허용 목록에 없는 경우
     */
    private void validateContentType(String contentType) {
        if (contentType == null || !UploadFileService.ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw FileUploadException.invalidFileType(contentType);
        }
    }

    /**
     * URL이 본 서버 업로드 객체 경로({@code uploads/{senderId}/...})에 속하는지 검증한다.
     * <p>
     * URL을 파싱하여 경로 세그먼트를 추출하고, {@code uploads/{senderId}} 로 시작하며
     * 그 하위에 실제 파일명이 존재하는지 확인한다. 경로 탈출({@code ..}) 세그먼트는 거부한다.
     * </p>
     *
     * @param senderId 발신자 ID
     * @param url      검증할 URL
     * @throws FileUploadException URL이 본 서버 업로드 경로 규칙에 맞지 않는 경우
     */
    private void validateOwnedUploadUrl(Long senderId, String url) {
        String path = extractPath(url);

        String[] segments = path.split("/");
        // uploads / {senderId} / {fileName} 최소 3개 세그먼트 필요
        int rootIndex = -1;
        for (int i = 0; i < segments.length; i++) {
            if (segments[i].isEmpty()) {
                continue;
            }
            if (UPLOAD_ROOT.equals(segments[i])) {
                rootIndex = i;
                break;
            }
        }

        if (rootIndex == -1) {
            throw invalidFileUrl(url);
        }

        // 경로 탈출 세그먼트 거부
        for (String segment : segments) {
            if ("..".equals(segment)) {
                throw invalidFileUrl(url);
            }
        }

        // uploads 다음 세그먼트는 발신자 ID, 그 다음에 파일명이 있어야 한다
        if (rootIndex + 2 >= segments.length) {
            throw invalidFileUrl(url);
        }
        String ownerSegment = segments[rootIndex + 1];
        String fileSegment = segments[rootIndex + 2];
        if (!String.valueOf(senderId).equals(ownerSegment) || fileSegment.isEmpty()) {
            throw invalidFileUrl(url);
        }
    }

    /**
     * URL 문자열에서 경로 부분을 추출한다.
     * 절대 URL이면 호스트 이후의 path를, 상대 경로면 입력 그대로를 사용한다.
     *
     * @param url URL 문자열
     * @return 경로 부분
     * @throws FileUploadException URL 형식이 올바르지 않은 경우
     */
    private String extractPath(String url) {
        if (url == null || url.isBlank()) {
            throw invalidFileUrl(url);
        }
        try {
            URI uri = new URI(url);
            String path = uri.getPath();
            if (path == null || path.isBlank()) {
                throw invalidFileUrl(url);
            }
            return path;
        } catch (URISyntaxException e) {
            throw invalidFileUrl(url);
        }
    }

    /**
     * 신뢰할 수 없는 파일 URL 예외를 생성한다.
     *
     * @param url 문제가 된 URL
     * @return FileUploadException
     */
    private FileUploadException invalidFileUrl(String url) {
        return new FileUploadException("신뢰할 수 없는 파일 URL입니다: " + url);
    }
}
