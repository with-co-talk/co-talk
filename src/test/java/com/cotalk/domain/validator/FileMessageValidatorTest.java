package com.cotalk.domain.validator;

import com.cotalk.domain.exception.FileUploadException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("FileMessageValidator 테스트")
class FileMessageValidatorTest {

    private static final Long SENDER_ID = 42L;
    private static final String CONTENT_TYPE = "image/jpeg";

    @Nested
    @DisplayName("host 화이트리스트가 설정된 경우")
    class WithHostWhitelist {

        // MinIO 공개 URL 베이스: {publicUrl}/{bucket}
        // InMemory 베이스: http://localhost:8080/files
        private final FileMessageValidator validator = new FileMessageValidator(List.of(
                "http://localhost:9000/cotalk",
                "http://localhost:8080/files"
        ));

        @Test
        @DisplayName("정상 MinIO 공개 URL은 통과한다")
        void should_pass_when_validMinioPublicUrl() {
            String url = "http://localhost:9000/cotalk/uploads/42/abc.jpg";
            assertThatCode(() -> validator.validate(SENDER_ID, CONTENT_TYPE, url, null))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("정상 MinIO presigned URL(쿼리 포함)도 통과한다")
        void should_pass_when_validPresignedUrl() {
            String url = "http://localhost:9000/cotalk/uploads/42/abc.jpg?X-Amz-Signature=deadbeef&X-Amz-Expires=604800";
            assertThatCode(() -> validator.validate(SENDER_ID, CONTENT_TYPE, url, null))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("정상 InMemory URL도 통과한다")
        void should_pass_when_validInMemoryUrl() {
            String url = "http://localhost:8080/files/uploads/42/abc.jpg";
            assertThatCode(() -> validator.validate(SENDER_ID, CONTENT_TYPE, url, null))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("외부 호스트가 동일 path를 흉내 내면 거부한다 (host 우회 방어)")
        void should_reject_when_externalHostMimicsPath() {
            String url = "http://evil.com/cotalk/uploads/42/f.jpg";
            assertThatThrownBy(() -> validator.validate(SENDER_ID, CONTENT_TYPE, url, null))
                    .isInstanceOf(FileUploadException.class);
        }

        @Test
        @DisplayName("허용 host에 버킷 없이 uploads만 붙인 경우 거부한다 (베이스 path 검증)")
        void should_reject_when_missingBucketSegment() {
            // localhost:9000 은 cotalk 버킷 뒤에 와야 함
            String url = "http://localhost:9000/uploads/42/f.jpg";
            assertThatThrownBy(() -> validator.validate(SENDER_ID, CONTENT_TYPE, url, null))
                    .isInstanceOf(FileUploadException.class);
        }

        @Test
        @DisplayName("임의 prefix를 끼워 넣은 경로는 거부한다 (위치 무관 매칭 우회 방어)")
        void should_reject_when_arbitraryPrefixInjected() {
            // 베이스(cotalk) 바로 뒤가 아니라 a/b 를 끼워 넣음
            String url = "http://localhost:9000/cotalk/a/b/uploads/42/f.jpg";
            assertThatThrownBy(() -> validator.validate(SENDER_ID, CONTENT_TYPE, url, null))
                    .isInstanceOf(FileUploadException.class);
        }

        @Test
        @DisplayName("타인 소유 경로는 거부한다")
        void should_reject_when_otherUserPath() {
            String url = "http://localhost:9000/cotalk/uploads/99/f.jpg";
            assertThatThrownBy(() -> validator.validate(SENDER_ID, CONTENT_TYPE, url, null))
                    .isInstanceOf(FileUploadException.class);
        }

        @Test
        @DisplayName("https scheme(허용 목록과 다름)은 거부한다")
        void should_reject_when_schemeMismatch() {
            String url = "https://localhost:9000/cotalk/uploads/42/f.jpg";
            assertThatThrownBy(() -> validator.validate(SENDER_ID, CONTENT_TYPE, url, null))
                    .isInstanceOf(FileUploadException.class);
        }

        @Test
        @DisplayName("경로 탈출(..) 세그먼트는 거부한다")
        void should_reject_when_pathTraversal() {
            String url = "http://localhost:9000/cotalk/uploads/42/../99/f.jpg";
            assertThatThrownBy(() -> validator.validate(SENDER_ID, CONTENT_TYPE, url, null))
                    .isInstanceOf(FileUploadException.class);
        }

        @Test
        @DisplayName("허용되지 않은 contentType은 거부한다")
        void should_reject_when_invalidContentType() {
            String url = "http://localhost:9000/cotalk/uploads/42/f.jpg";
            assertThatThrownBy(() -> validator.validate(SENDER_ID, "application/x-evil", url, null))
                    .isInstanceOf(FileUploadException.class);
        }

        @Test
        @DisplayName("썸네일 URL도 동일하게 검증한다")
        void should_validate_thumbnailUrl() {
            String fileUrl = "http://localhost:9000/cotalk/uploads/42/f.jpg";
            String evilThumb = "http://evil.com/cotalk/uploads/42/t.jpg";
            assertThatThrownBy(() -> validator.validate(SENDER_ID, CONTENT_TYPE, fileUrl, evilThumb))
                    .isInstanceOf(FileUploadException.class);
        }
    }

    @Nested
    @DisplayName("host 화이트리스트가 비어 있는 경우(하위 호환)")
    class WithoutHostWhitelist {

        private final FileMessageValidator validator = new FileMessageValidator();

        @Test
        @DisplayName("uploads/{senderId}/{file} 구조면 통과한다")
        void should_pass_when_validStructure() {
            String url = "http://localhost:9000/cotalk/uploads/42/abc.jpg";
            assertThatCode(() -> validator.validate(SENDER_ID, CONTENT_TYPE, url, null))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("경로 탈출(..) 세그먼트는 여전히 거부한다")
        void should_reject_when_pathTraversal() {
            String url = "http://localhost:9000/cotalk/uploads/42/../f.jpg";
            assertThatThrownBy(() -> validator.validate(SENDER_ID, CONTENT_TYPE, url, null))
                    .isInstanceOf(FileUploadException.class);
        }

        @Test
        @DisplayName("타인 소유 경로는 거부한다")
        void should_reject_when_otherUserPath() {
            String url = "http://localhost:9000/cotalk/uploads/99/f.jpg";
            assertThatThrownBy(() -> validator.validate(SENDER_ID, CONTENT_TYPE, url, null))
                    .isInstanceOf(FileUploadException.class);
        }
    }
}
