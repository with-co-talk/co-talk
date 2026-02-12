package com.cotalk.domain.service;

import com.cotalk.domain.exception.FileUploadException;
import com.cotalk.domain.port.inbound.file.UploadFileUseCase.FileUploadCommand;
import com.cotalk.domain.port.inbound.file.UploadFileUseCase.FileUploadResult;
import com.cotalk.domain.port.outbound.FileStorage;
import com.cotalk.infrastructure.config.properties.FileUploadProperties;
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
        // 최대 파일 크기: 10MB, 허용 타입: 이미지, PDF
        FileUploadProperties fileUploadProperties = new FileUploadProperties(10 * 1024 * 1024L);
        uploadFileService = new UploadFileService(fileStorage, fileUploadProperties);
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
}
