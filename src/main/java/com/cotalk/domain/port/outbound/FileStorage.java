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
