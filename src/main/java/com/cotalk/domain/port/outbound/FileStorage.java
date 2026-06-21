package com.cotalk.domain.port.outbound;

import java.io.InputStream;
import java.util.Optional;

/**
 * 파일 저장소 아웃바운드 포트.
 * 파일 업로드, 삭제, 조회를 위한 인터페이스를 정의한다.
 * MinIO, AWS S3 등 다양한 저장소 구현체로 교체 가능하다.
 *
 * @author seunggu.lee
 */
public interface FileStorage {

    /**
     * 파일을 업로드한다.
     *
     * @param inputStream 파일 입력 스트림
     * @param fileName    저장할 파일명 (경로 포함 가능)
     * @param contentType 파일 MIME 타입
     * @param fileSize    파일 크기 (bytes)
     * @return 업로드된 파일의 URL
     */
    String upload(InputStream inputStream, String fileName, String contentType, long fileSize);

    /**
     * 파일을 삭제한다.
     *
     * @param fileName 삭제할 파일명 (경로 포함)
     */
    void delete(String fileName);

    /**
     * 파일 존재 여부를 확인한다.
     *
     * @param fileName 확인할 파일명
     * @return 존재 여부
     */
    boolean exists(String fileName);

    /**
     * 파일 다운로드를 위한 Pre-signed URL을 생성한다.
     *
     * @param fileName          파일명
     * @param expirationMinutes URL 유효 시간 (분)
     * @return Pre-signed URL
     */
    String generatePresignedUrl(String fileName, int expirationMinutes);

    /**
     * 저장된 객체 키(object id)로부터 접근 가능한 URL을 재구성한다.
     * <p>
     * 불투명 식별자(object-id) 기반 파일 메시지 전송에서, 클라이언트가 임의의 URL을
     * 주입하지 못하도록 서버가 저장 객체 키만으로 URL을 다시 만든다.
     * 업로드 시 {@link #upload(InputStream, String, String, long)}이 반환한 URL과 동일한
     * 규칙(공개 URL 또는 Pre-signed URL)으로 생성한다.
     * </p>
     *
     * @param objectKey 저장 객체 키 (예: {@code uploads/{userId}/{uuid}.ext})
     * @return 해당 객체에 접근할 수 있는 URL
     */
    String resolveUrl(String objectKey);

    /**
     * 저장된 첨부파일 URL을 짧은 만료시간의 Pre-signed GET URL로 재발급한다.
     * <p>
     * 메시지에 저장된 {@code fileUrl}/{@code thumbnailUrl}은 업로드 시점에 발급된 값(공개 직접 URL
     * 또는 만료된 Pre-signed URL)일 수 있다. 채팅방 멤버십이 검증된 읽기 경로에서 이 메서드로
     * 저장 URL을 다시 서명하여, 인증된 멤버에게만 짧은 시간 동안 유효한 접근 URL을 제공한다.
     * 멤버십 검증은 호출하는 애플리케이션 서비스의 책임이며, 이 어댑터는 검증을 수행하지 않는다.
     * </p>
     * <p>
     * 저장 URL에서 저장 객체 키를 추출할 수 없거나(외부 URL 등) 입력이 비어 있으면, 입력값을
     * 그대로 반환한다(링크 미리보기 이미지 등 첨부파일이 아닌 URL은 변형하지 않는다).
     * </p>
     *
     * @param storedUrl         메시지에 저장된 첨부파일 URL
     * @param expirationMinutes Pre-signed URL 유효 시간(분)
     * @return 짧은 만료시간의 Pre-signed URL. 객체 키를 추출할 수 없으면 {@code storedUrl} 원본
     */
    String presignAttachmentUrl(String storedUrl, int expirationMinutes);

    /**
     * 저장된 객체의 메타데이터(MIME 타입·크기)를 조회한다.
     * <p>
     * object-id 기반 전송 시 클라이언트가 보낸 contentType/size를 신뢰하지 않고
     * 저장소가 기록한 실제 메타데이터로 재구성하기 위해 사용한다. 메타데이터를 알 수 없는
     * 구현체(예: InMemory)는 {@link Optional#empty()}를 반환할 수 있다.
     * </p>
     *
     * @param objectKey 저장 객체 키
     * @return 객체 메타데이터. 존재하지 않거나 알 수 없으면 빈 Optional
     */
    Optional<StoredObjectMetadata> getMetadata(String objectKey);

    /**
     * 저장된 객체의 메타데이터.
     *
     * @param contentType 저장소가 기록한 MIME 타입 (없으면 null)
     * @param fileSize    저장소가 기록한 크기(bytes). 알 수 없으면 음수
     */
    record StoredObjectMetadata(String contentType, long fileSize) {}
}
