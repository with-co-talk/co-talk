package com.cotalk.domain.service;

import com.cotalk.domain.exception.FileUploadException;
import com.cotalk.domain.port.outbound.FileStorage;
import com.cotalk.domain.port.outbound.FileStorage.StoredObjectMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("FileObjectResolver")
class FileObjectResolverTest {

    private FakeFileStorage fileStorage;
    private FileObjectResolver resolver;

    @BeforeEach
    void setUp() {
        fileStorage = new FakeFileStorage();
        resolver = new FileObjectResolver(fileStorage);
    }

    @Nested
    @DisplayName("소유한 object-id로 메타 재구성 시")
    class ResolveOwnedObject {

        @Test
        @DisplayName("URL·contentType·size를 서버 저장 메타로 재구성한다")
        void should_ReconstructMeta_when_ObjectOwnedAndExists() {
            String objectId = "uploads/42/abc.png";
            fileStorage.put(objectId, "image/png", 1234L);

            FileObjectResolver.ResolvedFileObject resolved = resolver.resolve(42L, objectId);

            assertThat(resolved.fileUrl()).isEqualTo("http://localhost:8080/files/" + objectId);
            assertThat(resolved.contentType()).isEqualTo("image/png");
            assertThat(resolved.fileSize()).isEqualTo(1234L);
        }
    }

    @Nested
    @DisplayName("소유하지 않은 object-id 거부")
    class RejectNotOwned {

        @Test
        @DisplayName("다른 사용자의 object-id면 예외를 던진다")
        void should_Throw_when_ObjectOwnedByAnotherUser() {
            String objectId = "uploads/99/secret.png";
            fileStorage.put(objectId, "image/png", 10L);

            assertThatThrownBy(() -> resolver.resolve(42L, objectId))
                    .isInstanceOf(FileUploadException.class);
        }

        @Test
        @DisplayName("uploads 루트가 아닌 키는 거부한다")
        void should_Throw_when_KeyNotUnderUploadsRoot() {
            String objectId = "etc/passwd";
            fileStorage.put(objectId, "image/png", 10L);

            assertThatThrownBy(() -> resolver.resolve(42L, objectId))
                    .isInstanceOf(FileUploadException.class);
        }

        @Test
        @DisplayName("경로 탈출(..) 세그먼트를 거부한다")
        void should_Throw_when_PathTraversalSegment() {
            assertThatThrownBy(() -> resolver.resolve(42L, "uploads/42/../99/x.png"))
                    .isInstanceOf(FileUploadException.class);
        }
    }

    @Nested
    @DisplayName("존재하지 않는 object-id 거부")
    class RejectNotExists {

        @Test
        @DisplayName("저장소에 객체가 없으면 예외를 던진다")
        void should_Throw_when_ObjectDoesNotExist() {
            assertThatThrownBy(() -> resolver.resolve(42L, "uploads/42/missing.png"))
                    .isInstanceOf(FileUploadException.class);
        }
    }

    @Nested
    @DisplayName("contentType 검증")
    class ContentTypeValidation {

        @Test
        @DisplayName("허용되지 않은 저장 contentType이면 예외를 던진다")
        void should_Throw_when_StoredContentTypeNotAllowed() {
            String objectId = "uploads/42/evil.exe";
            fileStorage.put(objectId, "application/x-msdownload", 10L);

            assertThatThrownBy(() -> resolver.resolve(42L, objectId))
                    .isInstanceOf(FileUploadException.class);
        }

        @Test
        @DisplayName("메타데이터가 없으면 클라이언트가 보낸 힌트 contentType으로 폴백 검증한다")
        void should_FallbackToHint_when_MetadataMissing() {
            String objectId = "uploads/42/no-meta.png";
            fileStorage.putWithoutMetadata(objectId);

            FileObjectResolver.ResolvedFileObject resolved =
                    resolver.resolve(42L, objectId, "image/png", 555L);

            assertThat(resolved.contentType()).isEqualTo("image/png");
            assertThat(resolved.fileSize()).isEqualTo(555L);
            assertThat(resolved.fileUrl()).isEqualTo("http://localhost:8080/files/" + objectId);
        }

        @Test
        @DisplayName("메타데이터도 힌트도 없으면 예외를 던진다")
        void should_Throw_when_NoMetadataAndNoHint() {
            String objectId = "uploads/42/no-meta.png";
            fileStorage.putWithoutMetadata(objectId);

            assertThatThrownBy(() -> resolver.resolve(42L, objectId, null, null))
                    .isInstanceOf(FileUploadException.class);
        }

        @Test
        @DisplayName("메타데이터가 없고 contentType 힌트는 있으나 size 힌트가 없으면 예외를 던진다")
        void should_Throw_when_NoMetadataAndNoSizeHint() {
            // size를 확정할 수 없을 때 조용히 0L로 두지 않고 contentType과 동일하게 거부한다.
            String objectId = "uploads/42/no-meta.png";
            fileStorage.putWithoutMetadata(objectId);

            assertThatThrownBy(() -> resolver.resolve(42L, objectId, "image/png", null))
                    .isInstanceOf(FileUploadException.class);
        }
    }

    /**
     * 테스트용 FileStorage 페이크. 메모리에 키→메타데이터를 보관한다.
     */
    private static final class FakeFileStorage implements FileStorage {

        private final Map<String, StoredObjectMetadata> objects = new HashMap<>();
        private final Map<String, Boolean> existsOnly = new HashMap<>();

        void put(String key, String contentType, long size) {
            objects.put(key, new StoredObjectMetadata(contentType, size));
        }

        void putWithoutMetadata(String key) {
            existsOnly.put(key, true);
        }

        @Override
        public String upload(InputStream inputStream, String fileName, String contentType, long fileSize) {
            return resolveUrl(fileName);
        }

        @Override
        public void delete(String fileName) {
            objects.remove(fileName);
            existsOnly.remove(fileName);
        }

        @Override
        public boolean exists(String fileName) {
            return objects.containsKey(fileName) || existsOnly.containsKey(fileName);
        }

        @Override
        public String generatePresignedUrl(String fileName, int expirationMinutes) {
            return resolveUrl(fileName);
        }

        @Override
        public String resolveUrl(String objectKey) {
            return "http://localhost:8080/files/" + objectKey;
        }

        @Override
        public Optional<StoredObjectMetadata> getMetadata(String objectKey) {
            return Optional.ofNullable(objects.get(objectKey));
        }
    }
}
