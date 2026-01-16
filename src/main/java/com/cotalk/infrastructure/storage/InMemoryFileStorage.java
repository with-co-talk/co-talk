package com.cotalk.infrastructure.storage;

import com.cotalk.domain.port.outbound.FileStorage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 개발/테스트용 인메모리 파일 저장소
 * MinIO가 비활성화되었을 때 사용
 */
@Component
@ConditionalOnProperty(name = "minio.enabled", havingValue = "false", matchIfMissing = true)
public class InMemoryFileStorage implements FileStorage {

    private final Map<String, byte[]> storage = new ConcurrentHashMap<>();
    private final String baseUrl;

    public InMemoryFileStorage() {
        this.baseUrl = "http://localhost:8080/files";
    }

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

    @Override
    public void delete(String fileName) {
        storage.remove(fileName);
    }

    @Override
    public boolean exists(String fileName) {
        return storage.containsKey(fileName);
    }

    @Override
    public String generatePresignedUrl(String fileName, int expirationMinutes) {
        return baseUrl + "/" + fileName + "?expires=" + expirationMinutes;
    }

    // 테스트용 메서드
    public byte[] getFile(String fileName) {
        return storage.get(fileName);
    }

    public void clear() {
        storage.clear();
    }
}
