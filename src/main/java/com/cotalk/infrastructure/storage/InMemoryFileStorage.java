package com.cotalk.infrastructure.storage;

import com.cotalk.domain.port.outbound.FileStorage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 개발/테스트용 인메모리 파일 저장소 구현체.
 * {@link FileStorage} 포트를 구현하여 메모리에 파일을 저장한다.
 *
 * <p>MinIO가 비활성화되었을 때({@code minio.enabled=false}) 자동으로 활성화된다.
 * 실제 파일 시스템이나 외부 스토리지를 사용하지 않으므로 테스트 및 로컬 개발 환경에 적합하다.
 *
 * <p>주의: 애플리케이션 재시작 시 모든 저장된 파일이 손실된다.
 *
 * @author seunggu.lee
 * @see FileStorage
 * @see MinioFileStorage
 */
@Component
@ConditionalOnProperty(name = "minio.enabled", havingValue = "false", matchIfMissing = true)
public class InMemoryFileStorage implements FileStorage {

    private final Map<String, byte[]> storage = new ConcurrentHashMap<>();
    private final String baseUrl;

    /**
     * 기본 생성자.
     * 기본 URL을 {@code http://localhost:8080/files}로 설정한다.
     */
    public InMemoryFileStorage() {
        this.baseUrl = "http://localhost:8080/files";
    }

    /**
     * 파일을 메모리에 업로드한다.
     *
     * @param inputStream 업로드할 파일의 입력 스트림
     * @param fileName    저장될 파일명
     * @param contentType 파일의 MIME 타입 (인메모리 저장소에서는 사용되지 않음)
     * @param fileSize    파일 크기 (인메모리 저장소에서는 사용되지 않음)
     * @return 업로드된 파일에 접근할 수 있는 URL
     * @throws RuntimeException 파일 읽기에 실패한 경우
     */
    @Override
    public String upload(InputStream inputStream, String fileName, String contentType, long fileSize) {
        try {
            byte[] data = inputStream.readAllBytes();
            storage.put(fileName, data);
            return baseUrl + "/" + fileName;
        } catch (Exception e) {
            throw new RuntimeException("Failed to read file", e);
        }
    }

    /**
     * 메모리에서 파일을 삭제한다.
     *
     * @param fileName 삭제할 파일명
     */
    @Override
    public void delete(String fileName) {
        storage.remove(fileName);
    }

    /**
     * 파일이 메모리에 존재하는지 확인한다.
     *
     * @param fileName 확인할 파일명
     * @return 파일이 존재하면 {@code true}, 그렇지 않으면 {@code false}
     */
    @Override
    public boolean exists(String fileName) {
        return storage.containsKey(fileName);
    }

    /**
     * 파일에 대한 Pre-signed URL을 생성한다.
     * 인메모리 저장소에서는 단순히 만료 시간 파라미터가 포함된 URL을 반환한다.
     *
     * @param fileName          파일명
     * @param expirationMinutes URL 만료 시간(분)
     * @return 생성된 URL 문자열
     */
    @Override
    public String generatePresignedUrl(String fileName, int expirationMinutes) {
        return baseUrl + "/" + fileName + "?expires=" + expirationMinutes;
    }

    /**
     * 저장된 파일의 바이트 배열을 반환한다.
     * 테스트 목적으로 제공되는 메서드이다.
     *
     * @param fileName 조회할 파일명
     * @return 파일의 바이트 배열, 파일이 없으면 {@code null}
     */
    public byte[] getFile(String fileName) {
        return storage.get(fileName);
    }

    /**
     * 모든 저장된 파일을 삭제한다.
     * 테스트 목적으로 제공되는 메서드이다.
     */
    public void clear() {
        storage.clear();
    }
}
