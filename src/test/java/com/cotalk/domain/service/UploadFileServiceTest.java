package com.cotalk.domain.service;

import com.cotalk.domain.exception.FileUploadException;
import com.cotalk.domain.port.inbound.file.UploadFileUseCase;
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
        // 최대 파일 크기: 10MB, 허용 타입: 이미지, PDF
        uploadFileService = new UploadFileService(fileStorage, 10 * 1024 * 1024L);
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
    }
}
