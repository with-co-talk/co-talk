package com.cotalk.domain.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 파일 관련 예외 단위 테스트.
 *
 * @author seunggu.lee
 */
@DisplayName("파일 관련 예외 테스트")
class FileExceptionTest {

    @Nested
    @DisplayName("FileStorageException")
    class FileStorageExceptionTest {

        @Test
        @DisplayName("메시지만으로 예외 생성")
        void should_createException_when_messageProvided() {
            // given
            String message = "파일 저장 실패";

            // when
            FileStorageException exception = new FileStorageException(message);

            // then
            assertThat(exception.getMessage()).isEqualTo(message);
            assertThat(exception.getCause()).isNull();
        }

        @Test
        @DisplayName("메시지와 원인으로 예외 생성")
        void should_createException_when_messageAndCauseProvided() {
            // given
            String message = "파일 저장 실패";
            Throwable cause = new RuntimeException("원인");

            // when
            FileStorageException exception = new FileStorageException(message, cause);

            // then
            assertThat(exception.getMessage()).isEqualTo(message);
            assertThat(exception.getCause()).isEqualTo(cause);
        }

        @Test
        @DisplayName("삭제 실패 팩토리 메서드")
        void should_createDeleteFailedException() {
            // given
            String fileName = "test.txt";
            Throwable cause = new RuntimeException("삭제 실패");

            // when
            FileStorageException exception = FileStorageException.deleteFailed(fileName, cause);

            // then
            assertThat(exception.getMessage()).contains(fileName);
            assertThat(exception.getMessage()).contains("삭제");
            assertThat(exception.getCause()).isEqualTo(cause);
        }

        @Test
        @DisplayName("파일 없음 팩토리 메서드")
        void should_createNotFoundException() {
            // given
            String fileName = "notfound.txt";

            // when
            FileStorageException exception = FileStorageException.notFound(fileName);

            // then
            assertThat(exception.getMessage()).contains(fileName);
            assertThat(exception.getMessage()).contains("찾을 수 없습니다");
        }
    }

    @Nested
    @DisplayName("FileUploadException")
    class FileUploadExceptionTest {

        @Test
        @DisplayName("메시지만으로 예외 생성")
        void should_createException_when_messageProvided() {
            // given
            String message = "파일 업로드 실패";

            // when
            FileUploadException exception = new FileUploadException(message);

            // then
            assertThat(exception.getMessage()).isEqualTo(message);
        }

        @Test
        @DisplayName("메시지와 원인으로 예외 생성")
        void should_createException_when_messageAndCauseProvided() {
            // given
            String message = "파일 업로드 실패";
            Throwable cause = new RuntimeException("원인");

            // when
            FileUploadException exception = new FileUploadException(message, cause);

            // then
            assertThat(exception.getMessage()).isEqualTo(message);
            assertThat(exception.getCause()).isEqualTo(cause);
        }

        @Test
        @DisplayName("잘못된 파일 타입 예외")
        void should_createInvalidFileTypeException() {
            // given
            String contentType = "application/exe";

            // when
            FileUploadException exception = FileUploadException.invalidFileType(contentType);

            // then
            assertThat(exception.getMessage()).contains(contentType);
            assertThat(exception.getMessage()).contains("지원하지 않는");
        }

        @Test
        @DisplayName("파일 크기 초과 예외")
        void should_createFileTooLargeException() {
            // given
            long maxSize = 10485760L; // 10MB

            // when
            FileUploadException exception = FileUploadException.fileTooLarge(maxSize);

            // then
            assertThat(exception.getMessage()).contains(String.valueOf(maxSize));
            assertThat(exception.getMessage()).contains("초과");
        }

        @Test
        @DisplayName("업로드 실패 예외 - 원인 포함")
        void should_createUploadFailedException_withCause() {
            // given
            Throwable cause = new RuntimeException("네트워크 오류");

            // when
            FileUploadException exception = FileUploadException.uploadFailed(cause);

            // then
            assertThat(exception.getMessage()).contains("업로드");
            assertThat(exception.getCause()).isEqualTo(cause);
        }

        @Test
        @DisplayName("업로드 실패 예외 - 메시지만")
        void should_createUploadFailedException_withMessage() {
            // given
            String message = "커스텀 업로드 실패 메시지";

            // when
            FileUploadException exception = FileUploadException.uploadFailed(message);

            // then
            assertThat(exception.getMessage()).isEqualTo(message);
        }

        @Test
        @DisplayName("잘못된 파일 시그니처 예외")
        void should_createInvalidFileSignatureException() {
            // given
            String contentType = "image/png";

            // when
            FileUploadException exception = FileUploadException.invalidFileSignature(contentType);

            // then
            assertThat(exception.getMessage()).contains(contentType);
            assertThat(exception.getMessage()).contains("시그니처");
        }
    }
}
