package com.cotalk.domain.service;

import com.cotalk.domain.exception.FileUploadException;
import com.cotalk.domain.port.outbound.FileStorage;
import com.cotalk.domain.port.outbound.FileStorage.StoredObjectMetadata;

import java.util.Optional;

/**
 * 불투명 식별자(object-id) 기반 파일 메시지 메타 재구성기.
 * <p>
 * 파일 메시지 전송 시 클라이언트가 임의의 {@code fileUrl}/{@code contentType}/{@code fileSize}를
 * 주입하는 대신, 업로드가 발급한 <b>불투명 저장 객체 키(object-id)</b>만 보내도록 하는 근본 해결책의
 * 서버 측 구성 요소다. object-id가 주어지면 다음을 수행한다.
 * </p>
 * <ol>
 *   <li><b>소유 검증</b>: object-id가 {@code uploads/{senderId}/{file}} 구조에 정확히 속하는지 확인하여
 *       타인 소유 객체를 메시지에 붙이지 못하게 한다. 경로 탈출({@code ..}) 세그먼트는 거부한다.</li>
 *   <li><b>존재 검증</b>: 저장소에 실제로 객체가 존재하는지 확인한다.</li>
 *   <li><b>메타 재구성</b>: 저장소가 기록한 메타데이터(또는 부재 시 클라이언트 힌트)로 contentType/size를
 *       정하고, 저장소 포트로 접근 URL을 다시 만든다. 이로써 클라이언트가 URL/타입을 위조할 여지를 없앤다.</li>
 * </ol>
 * <p>
 * contentType은 {@link UploadFileService#ALLOWED_CONTENT_TYPES} 허용 목록으로 재검증한다.
 * </p>
 *
 * @author seunggu.lee
 * @see FileStorage
 * @see UploadFileService
 */
public class FileObjectResolver {

    /**
     * 업로드 객체 키의 최상위 디렉터리.
     * {@code UploadFileService.generateStoragePath}가 생성하는 {@code uploads/{userId}/...} 규칙과 일치한다.
     */
    private static final String UPLOAD_ROOT = "uploads";

    private final FileStorage fileStorage;

    /**
     * FileObjectResolver를 생성한다.
     *
     * @param fileStorage 파일 저장소 포트
     */
    public FileObjectResolver(FileStorage fileStorage) {
        this.fileStorage = fileStorage;
    }

    /**
     * object-id로 파일 메타를 재구성한다(힌트 없음).
     *
     * @param senderId 발신자(현재 인증된 사용자) ID
     * @param objectId 업로드가 발급한 불투명 저장 객체 키
     * @return 재구성된 파일 객체(URL·contentType·size)
     * @throws FileUploadException 소유/존재/타입 검증에 실패한 경우
     */
    public ResolvedFileObject resolve(Long senderId, String objectId) {
        return resolve(senderId, objectId, null, null);
    }

    /**
     * object-id로 파일 메타를 재구성한다.
     * <p>
     * 저장소 메타데이터를 우선 사용하고, 저장소가 메타데이터를 제공하지 않는 경우에 한해
     * 클라이언트가 보낸 힌트({@code hintContentType}/{@code hintFileSize})로 폴백한다.
     * 어느 경우든 contentType은 허용 목록으로 재검증된다.
     * </p>
     *
     * @param senderId        발신자 ID
     * @param objectId        불투명 저장 객체 키
     * @param hintContentType 클라이언트가 보낸 contentType 힌트(저장소 메타 부재 시 폴백, null 허용)
     * @param hintFileSize    클라이언트가 보낸 size 힌트(저장소 메타 부재 시 폴백, null 허용)
     * @return 재구성된 파일 객체
     * @throws FileUploadException 소유/존재/타입 검증에 실패한 경우
     */
    public ResolvedFileObject resolve(Long senderId, String objectId, String hintContentType, Long hintFileSize) {
        String key = normalizeAndValidateOwnership(senderId, objectId);

        Optional<StoredObjectMetadata> metadata = fileStorage.getMetadata(key);

        // 메타데이터가 있으면 존재가 보장된다. 없으면 exists로 별도 확인한다.
        if (metadata.isEmpty() && !fileStorage.exists(key)) {
            throw new FileUploadException("업로드된 파일을 찾을 수 없습니다: " + objectId);
        }

        String contentType = resolveContentType(metadata.orElse(null), hintContentType);
        validateContentType(contentType);

        long fileSize = resolveFileSize(metadata.orElse(null), hintFileSize);
        String fileUrl = fileStorage.resolveUrl(key);

        return new ResolvedFileObject(fileUrl, contentType, fileSize);
    }

    /**
     * object-id가 발신자 소유 업로드 경로({@code uploads/{senderId}/{file}})에 정확히 속하는지 검증하고,
     * 선행 슬래시를 제거한 정규화 키를 반환한다.
     *
     * @param senderId 발신자 ID
     * @param objectId 검증할 object-id
     * @return 정규화된 저장 객체 키
     * @throws FileUploadException 구조가 맞지 않거나 타인 소유이거나 경로 탈출이 있는 경우
     */
    private String normalizeAndValidateOwnership(Long senderId, String objectId) {
        if (objectId == null || objectId.isBlank()) {
            throw invalidObjectId(objectId);
        }
        String key = stripLeadingSlash(objectId.trim());
        String[] segments = key.split("/");

        for (String segment : segments) {
            if ("..".equals(segment) || segment.isEmpty()) {
                throw invalidObjectId(objectId);
            }
        }

        // 정확히 uploads/{senderId}/{file...} 구조여야 한다.
        if (segments.length < 3
                || !UPLOAD_ROOT.equals(segments[0])
                || !String.valueOf(senderId).equals(segments[1])) {
            throw invalidObjectId(objectId);
        }
        return key;
    }

    /**
     * 저장소 메타데이터의 contentType을 우선 사용하고, 없으면 힌트로 폴백한다.
     */
    private String resolveContentType(StoredObjectMetadata metadata, String hintContentType) {
        if (metadata != null && metadata.contentType() != null && !metadata.contentType().isBlank()) {
            return metadata.contentType();
        }
        if (hintContentType != null && !hintContentType.isBlank()) {
            return hintContentType;
        }
        throw new FileUploadException("파일 형식을 확인할 수 없습니다.");
    }

    /**
     * 저장소 메타데이터의 size를 우선 사용하고, 없으면 힌트로 폴백한다(폴백도 없으면 0).
     */
    private long resolveFileSize(StoredObjectMetadata metadata, Long hintFileSize) {
        if (metadata != null && metadata.fileSize() >= 0) {
            return metadata.fileSize();
        }
        return hintFileSize != null ? hintFileSize : 0L;
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

    private String stripLeadingSlash(String path) {
        int i = 0;
        while (i < path.length() && path.charAt(i) == '/') {
            i++;
        }
        return path.substring(i);
    }

    private FileUploadException invalidObjectId(String objectId) {
        return new FileUploadException("신뢰할 수 없는 파일 식별자입니다: " + objectId);
    }

    /**
     * 재구성된 파일 객체.
     *
     * @param fileUrl     서버가 저장 객체 키로부터 재구성한 접근 URL
     * @param contentType 검증된 MIME 타입
     * @param fileSize    파일 크기(bytes)
     */
    public record ResolvedFileObject(String fileUrl, String contentType, long fileSize) {}
}
