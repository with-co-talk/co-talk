package com.cotalk.domain.service;

import com.cotalk.domain.exception.FileUploadException;
import com.cotalk.domain.port.inbound.file.UploadFileUseCase.FileUploadCommand;
import com.cotalk.domain.port.inbound.file.UploadFileUseCase.FileUploadResult;
import com.cotalk.domain.port.outbound.FileStorage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UploadFileServiceTest {

    @Mock
    private FileStorage fileStorage;

    private UploadFileService uploadFileService;

    @BeforeEach
    void setUp() {
        // 최대 파일 크기: 10MB
        long maxFileSize = 10 * 1024 * 1024L;
        uploadFileService = new UploadFileService(fileStorage, maxFileSize);
    }

    @Nested
    @DisplayName("파일 업로드")
    class UploadFile {

        @Test
        @DisplayName("이미지 파일 업로드 성공")
        void should_uploadImage_when_validImageFile() {
            // given
            Long userId = 1L;
            // JPEG magic bytes: FF D8 FF
            byte[] jpegMagic = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10};
            byte[] content = new byte[100];
            System.arraycopy(jpegMagic, 0, content, 0, jpegMagic.length);
            InputStream inputStream = new ByteArrayInputStream(content);
            String originalFileName = "test-image.jpg";
            String contentType = "image/jpeg";
            long fileSize = content.length;

            String expectedUrl = "https://storage.example.com/uploads/1/abc123.jpg";
            given(fileStorage.upload(any(InputStream.class), anyString(), eq(contentType), eq(fileSize)))
                    .willReturn(expectedUrl);

            FileUploadCommand command = new FileUploadCommand(
                    userId, inputStream, originalFileName, contentType, fileSize
            );

            // when
            FileUploadResult result = uploadFileService.uploadFile(command);

            // then
            assertThat(result.fileUrl()).isEqualTo(expectedUrl);
            assertThat(result.contentType()).isEqualTo(contentType);
            assertThat(result.fileSize()).isEqualTo(fileSize);
            verify(fileStorage).upload(any(InputStream.class), anyString(), eq(contentType), eq(fileSize));
        }

        @Test
        @DisplayName("PNG 이미지 업로드 성공")
        void should_uploadPng_when_validPngFile() {
            // given
            Long userId = 1L;
            // PNG magic bytes: 89 50 4E 47 0D 0A 1A 0A
            byte[] pngMagic = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
            byte[] content = new byte[100];
            System.arraycopy(pngMagic, 0, content, 0, pngMagic.length);
            InputStream inputStream = new ByteArrayInputStream(content);
            String originalFileName = "test.png";
            String contentType = "image/png";
            long fileSize = content.length;

            String expectedUrl = "https://storage.example.com/uploads/1/abc123.png";
            given(fileStorage.upload(any(InputStream.class), anyString(), eq(contentType), eq(fileSize)))
                    .willReturn(expectedUrl);

            FileUploadCommand command = new FileUploadCommand(
                    userId, inputStream, originalFileName, contentType, fileSize
            );

            // when
            FileUploadResult result = uploadFileService.uploadFile(command);

            // then
            assertThat(result.fileUrl()).isEqualTo(expectedUrl);
            assertThat(result.contentType()).isEqualTo(contentType);
        }

        @Test
        @DisplayName("PDF 파일 업로드 성공")
        void should_uploadPdf_when_validPdfFile() {
            // given
            Long userId = 1L;
            // PDF magic bytes: 25 50 44 46 2D (%PDF-)
            byte[] pdfMagic = new byte[]{0x25, 0x50, 0x44, 0x46, 0x2D};
            byte[] content = new byte[100];
            System.arraycopy(pdfMagic, 0, content, 0, pdfMagic.length);
            InputStream inputStream = new ByteArrayInputStream(content);
            String originalFileName = "document.pdf";
            String contentType = "application/pdf";
            long fileSize = content.length;

            String expectedUrl = "https://storage.example.com/uploads/1/abc123.pdf";
            given(fileStorage.upload(any(InputStream.class), anyString(), eq(contentType), eq(fileSize)))
                    .willReturn(expectedUrl);

            FileUploadCommand command = new FileUploadCommand(
                    userId, inputStream, originalFileName, contentType, fileSize
            );

            // when
            FileUploadResult result = uploadFileService.uploadFile(command);

            // then
            assertThat(result.fileUrl()).isEqualTo(expectedUrl);
            assertThat(result.contentType()).isEqualTo(contentType);
        }

        @Test
        @DisplayName("지원하지 않는 파일 형식 업로드 실패")
        void should_throwException_when_unsupportedFileType() {
            // given
            Long userId = 1L;
            byte[] content = "fake exe content".getBytes();
            InputStream inputStream = new ByteArrayInputStream(content);
            String originalFileName = "malware.exe";
            String contentType = "application/x-msdownload";
            long fileSize = content.length;

            FileUploadCommand command = new FileUploadCommand(
                    userId, inputStream, originalFileName, contentType, fileSize
            );

            // when & then
            assertThatThrownBy(() -> uploadFileService.uploadFile(command))
                    .isInstanceOf(FileUploadException.class)
                    .hasMessageContaining("지원하지 않는 파일 형식");
        }

        @Test
        @DisplayName("파일 크기 초과 시 업로드 실패")
        void should_throwException_when_fileTooLarge() {
            // given
            Long userId = 1L;
            byte[] content = "small".getBytes();
            InputStream inputStream = new ByteArrayInputStream(content);
            String originalFileName = "large-file.jpg";
            String contentType = "image/jpeg";
            long fileSize = 20 * 1024 * 1024L; // 20MB (초과)

            FileUploadCommand command = new FileUploadCommand(
                    userId, inputStream, originalFileName, contentType, fileSize
            );

            // when & then
            assertThatThrownBy(() -> uploadFileService.uploadFile(command))
                    .isInstanceOf(FileUploadException.class)
                    .hasMessageContaining("최대 허용 크기를 초과");
        }

        @Test
        @DisplayName("GIF 이미지 업로드 성공")
        void should_uploadGif_when_validGifFile() {
            // given
            Long userId = 1L;
            // GIF magic bytes: 47 49 46 38 (GIF8)
            byte[] gifMagic = new byte[]{0x47, 0x49, 0x46, 0x38, 0x39, 0x61};
            byte[] content = new byte[100];
            System.arraycopy(gifMagic, 0, content, 0, gifMagic.length);
            InputStream inputStream = new ByteArrayInputStream(content);
            String originalFileName = "animation.gif";
            String contentType = "image/gif";
            long fileSize = content.length;

            String expectedUrl = "https://storage.example.com/uploads/1/abc123.gif";
            given(fileStorage.upload(any(InputStream.class), anyString(), eq(contentType), eq(fileSize)))
                    .willReturn(expectedUrl);

            FileUploadCommand command = new FileUploadCommand(
                    userId, inputStream, originalFileName, contentType, fileSize
            );

            // when
            FileUploadResult result = uploadFileService.uploadFile(command);

            // then
            assertThat(result.fileUrl()).isEqualTo(expectedUrl);
        }

        @Test
        @DisplayName("MP4 Content-Type이지만 실제로는 랜덤 바이트인 파일은 거부되어야 함")
        void should_rejectFile_when_mp4ContentTypeButInvalidBytes() {
            // Given: MP4 Content-Type이지만 실제로는 랜덤 바이트 (ftyp 없음)
            byte[] randomBytes = new byte[]{0x12, 0x34, 0x56, 0x78, (byte) 0x9A, (byte) 0xBC, (byte) 0xDE, (byte) 0xF0};
            InputStream inputStream = new ByteArrayInputStream(randomBytes);

            FileUploadCommand command = new FileUploadCommand(
                    1L,
                    inputStream,
                    "video.mp4",
                    "video/mp4",
                    randomBytes.length
            );

            // When & Then: 매직넘버 검증 실패로 예외 발생해야 함
            assertThatThrownBy(() -> uploadFileService.uploadFile(command))
                    .isInstanceOf(FileUploadException.class)
                    .hasMessageContaining("시그니처");
        }

        @Test
        @DisplayName("QuickTime Content-Type이지만 실제로는 랜덤 바이트인 파일은 거부되어야 함")
        void should_rejectFile_when_quicktimeContentTypeButInvalidBytes() {
            // Given: QuickTime Content-Type이지만 실제로는 랜덤 바이트
            byte[] randomBytes = new byte[]{0x00, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77};
            InputStream inputStream = new ByteArrayInputStream(randomBytes);

            FileUploadCommand command = new FileUploadCommand(
                    1L,
                    inputStream,
                    "video.mov",
                    "video/quicktime",
                    randomBytes.length
            );

            // When & Then: 매직넘버 검증 실패로 예외 발생해야 함
            assertThatThrownBy(() -> uploadFileService.uploadFile(command))
                    .isInstanceOf(FileUploadException.class)
                    .hasMessageContaining("시그니처");
        }

        @Test
        @DisplayName("HEIC Content-Type이지만 실제로는 랜덤 바이트인 파일은 거부되어야 함")
        void should_rejectFile_when_heicContentTypeButInvalidBytes() {
            // Given: HEIC Content-Type이지만 실제로는 JPEG 바이트
            byte[] randomBytes = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10, 0x4A, 0x46};
            InputStream inputStream = new ByteArrayInputStream(randomBytes);

            FileUploadCommand command = new FileUploadCommand(
                    1L,
                    inputStream,
                    "image.heic",
                    "image/heic",
                    randomBytes.length
            );

            // When & Then: 매직넘버 검증 실패로 예외 발생해야 함
            assertThatThrownBy(() -> uploadFileService.uploadFile(command))
                    .isInstanceOf(FileUploadException.class)
                    .hasMessageContaining("시그니처");
        }

        @Test
        @DisplayName("HEIF Content-Type이지만 실제로는 랜덤 바이트인 파일은 거부되어야 함")
        void should_rejectFile_when_heifContentTypeButInvalidBytes() {
            // Given: HEIF Content-Type이지만 실제로는 모두 0인 바이트
            byte[] randomBytes = new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
            InputStream inputStream = new ByteArrayInputStream(randomBytes);

            FileUploadCommand command = new FileUploadCommand(
                    1L,
                    inputStream,
                    "image.heif",
                    "image/heif",
                    randomBytes.length
            );

            // When & Then: 매직넘버 검증 실패로 예외 발생해야 함
            assertThatThrownBy(() -> uploadFileService.uploadFile(command))
                    .isInstanceOf(FileUploadException.class)
                    .hasMessageContaining("시그니처");
        }

        @Test
        @DisplayName("올바른 MP4 매직넘버를 가진 파일은 허용되어야 함")
        void should_acceptFile_when_validMp4MagicNumber() {
            // Given: 올바른 MP4 매직넘버 (ftyp at offset 4)
            // 00 00 00 1C 66 74 79 70 69 73 6F 6D ... (ftyp box with 'isom' brand)
            byte[] validMp4Bytes = new byte[]{
                    0x00, 0x00, 0x00, 0x1C, // box size (28 bytes)
                    0x66, 0x74, 0x79, 0x70, // 'ftyp'
                    0x69, 0x73, 0x6F, 0x6D, // major brand 'isom'
                    0x00, 0x00, 0x00, 0x01, // minor version
                    0x69, 0x73, 0x6F, 0x6D, // compatible brand 'isom'
                    0x00, 0x00, 0x00, 0x00  // padding
            };
            InputStream inputStream = new ByteArrayInputStream(validMp4Bytes);

            FileUploadCommand command = new FileUploadCommand(
                    1L,
                    inputStream,
                    "video.mp4",
                    "video/mp4",
                    validMp4Bytes.length
            );

            given(fileStorage.upload(any(), anyString(), anyString(), anyLong()))
                    .willReturn("http://example.com/uploads/1/video.mp4");

            // When & Then: 예외 없이 정상 처리되어야 함
            FileUploadResult result = uploadFileService.uploadFile(command);
            assertThat(result.fileUrl()).isNotNull();
        }

        @Test
        @DisplayName("올바른 WebP 매직넘버(RIFF....WEBP)를 가진 파일은 허용되어야 함")
        void should_acceptFile_when_validWebpMagicNumber() {
            // Given: RIFF + 파일크기(4바이트, 와일드카드) + WEBP
            byte[] validWebpBytes = new byte[]{
                    0x52, 0x49, 0x46, 0x46, // 'RIFF'
                    0x1A, 0x00, 0x00, 0x00, // file size (와일드카드)
                    0x57, 0x45, 0x42, 0x50  // 'WEBP'
            };
            InputStream inputStream = new ByteArrayInputStream(validWebpBytes);

            FileUploadCommand command = new FileUploadCommand(
                    1L, inputStream, "image.webp", "image/webp", validWebpBytes.length
            );

            given(fileStorage.upload(any(), anyString(), anyString(), anyLong()))
                    .willReturn("http://example.com/uploads/1/image.webp");

            FileUploadResult result = uploadFileService.uploadFile(command);
            assertThat(result.fileUrl()).isNotNull();
        }

        @Test
        @DisplayName("WAV(RIFF....WAVE) 파일을 image/webp로 위조 시 거부되어야 함")
        void should_rejectFile_when_wavForgedAsWebp() {
            // Given: RIFF 컨테이너이지만 offset 8-11이 'WAVE' (WebP 아님)
            byte[] wavBytes = new byte[]{
                    0x52, 0x49, 0x46, 0x46, // 'RIFF'
                    0x24, 0x00, 0x00, 0x00, // chunk size
                    0x57, 0x41, 0x56, 0x45  // 'WAVE'
            };
            InputStream inputStream = new ByteArrayInputStream(wavBytes);

            FileUploadCommand command = new FileUploadCommand(
                    1L, inputStream, "fake.webp", "image/webp", wavBytes.length
            );

            assertThatThrownBy(() -> uploadFileService.uploadFile(command))
                    .isInstanceOf(FileUploadException.class)
                    .hasMessageContaining("시그니처");
        }

        @Test
        @DisplayName("AVI(RIFF....AVI ) 파일을 image/webp로 위조 시 거부되어야 함")
        void should_rejectFile_when_aviForgedAsWebp() {
            // Given: RIFF 컨테이너이지만 offset 8-11이 'AVI ' (WebP 아님)
            byte[] aviBytes = new byte[]{
                    0x52, 0x49, 0x46, 0x46, // 'RIFF'
                    0x00, 0x10, 0x00, 0x00, // chunk size
                    0x41, 0x56, 0x49, 0x20  // 'AVI '
            };
            InputStream inputStream = new ByteArrayInputStream(aviBytes);

            FileUploadCommand command = new FileUploadCommand(
                    1L, inputStream, "fake.webp", "image/webp", aviBytes.length
            );

            assertThatThrownBy(() -> uploadFileService.uploadFile(command))
                    .isInstanceOf(FileUploadException.class)
                    .hasMessageContaining("시그니처");
        }

        @Test
        @DisplayName("RIFF만 있고 길이가 12 미만이면 거부되어야 함 (WEBP 마커 확인 불가)")
        void should_rejectFile_when_riffOnlyTooShort() {
            // Given: RIFF 4바이트만 (WEBP 마커 검증 불가능)
            byte[] riffOnly = new byte[]{0x52, 0x49, 0x46, 0x46, 0x00, 0x00};
            InputStream inputStream = new ByteArrayInputStream(riffOnly);

            FileUploadCommand command = new FileUploadCommand(
                    1L, inputStream, "short.webp", "image/webp", riffOnly.length
            );

            assertThatThrownBy(() -> uploadFileService.uploadFile(command))
                    .isInstanceOf(FileUploadException.class)
                    .hasMessageContaining("시그니처");
        }

        @Test
        @DisplayName("올바른 HEIC 매직넘버를 가진 파일은 허용되어야 함")
        void should_acceptFile_when_validHeicMagicNumber() {
            // Given: 올바른 HEIC 매직넘버 (ftyp at offset 4 with 'heic' brand)
            // 00 00 00 18 66 74 79 70 68 65 69 63 ... (ftyp box with 'heic' brand)
            byte[] validHeicBytes = new byte[]{
                    0x00, 0x00, 0x00, 0x18, // box size (24 bytes)
                    0x66, 0x74, 0x79, 0x70, // 'ftyp'
                    0x68, 0x65, 0x69, 0x63, // major brand 'heic'
                    0x00, 0x00, 0x00, 0x00, // minor version
                    0x6D, 0x69, 0x66, 0x31, // compatible brand 'mif1'
                    0x00, 0x00, 0x00, 0x00  // padding
            };
            InputStream inputStream = new ByteArrayInputStream(validHeicBytes);

            FileUploadCommand command = new FileUploadCommand(
                    1L,
                    inputStream,
                    "image.heic",
                    "image/heic",
                    validHeicBytes.length
            );

            given(fileStorage.upload(any(), anyString(), anyString(), anyLong()))
                    .willReturn("http://example.com/uploads/1/image.heic");

            // When & Then: 예외 없이 정상 처리되어야 함
            FileUploadResult result = uploadFileService.uploadFile(command);
            assertThat(result.fileUrl()).isNotNull();
        }
    }

    @Nested
    @DisplayName("저장 경로(object key) 위생")
    class StoragePathSanitization {

        private final org.mockito.ArgumentCaptor<String> pathCaptor =
                org.mockito.ArgumentCaptor.forClass(String.class);

        private String uploadAndCaptureKey(String originalFileName) {
            byte[] pngMagic = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
            byte[] content = new byte[100];
            System.arraycopy(pngMagic, 0, content, 0, pngMagic.length);
            InputStream inputStream = new ByteArrayInputStream(content);

            given(fileStorage.upload(any(InputStream.class), anyString(), anyString(), anyLong()))
                    .willReturn("http://example.com/x");

            FileUploadCommand command = new FileUploadCommand(
                    1L, inputStream, originalFileName, "image/png", content.length
            );
            uploadFileService.uploadFile(command);

            verify(fileStorage).upload(any(InputStream.class), pathCaptor.capture(), anyString(), anyLong());
            return pathCaptor.getValue();
        }

        @Test
        @DisplayName("정상 확장자는 object key에 보존된다")
        void should_keepExtension_when_normalFileName() {
            String key = uploadAndCaptureKey("photo.png");
            assertThat(key).startsWith("uploads/1/");
            assertThat(key).endsWith(".png");
        }

        @Test
        @DisplayName("확장자에 슬래시가 포함되면 무시되어 key prefix 오염이 없어야 한다")
        void should_ignoreExtension_when_containsSlash() {
            String key = uploadAndCaptureKey("a.png/../../x");
            // 키는 정확히 uploads/1/<uuid> 형태 (추가 슬래시·.. 없음)
            assertThat(key).matches("uploads/1/[0-9a-fA-F-]+");
            assertThat(key).doesNotContain("..");
        }

        @Test
        @DisplayName("확장자에 .. 이 포함되면 무시되어야 한다")
        void should_ignoreExtension_when_containsDotDot() {
            String key = uploadAndCaptureKey("evil.pn..g");
            // 비정상 문자 포함 시 확장자 미부여 (uuid만)
            assertThat(key).matches("uploads/1/[0-9a-fA-F-]+(\\.[A-Za-z0-9]+)?");
            assertThat(key).doesNotContain("..");
            assertThat(key.substring("uploads/1/".length())).doesNotContain("/");
        }

        @Test
        @DisplayName("확장자에 비영숫자 문자가 있으면 무시되어야 한다")
        void should_ignoreExtension_when_nonAlphanumeric() {
            String key = uploadAndCaptureKey("file.p ng");
            assertThat(key).matches("uploads/1/[0-9a-fA-F-]+");
        }
    }
}
