package com.cotalk.adapter.inbound.rest;

import com.cotalk.domain.exception.FileUploadException;
import com.cotalk.domain.port.inbound.file.UploadFileUseCase;
import com.cotalk.domain.port.inbound.file.UploadFileUseCase.FileUploadCommand;
import com.cotalk.domain.port.inbound.file.UploadFileUseCase.FileUploadResult;
import com.cotalk.infrastructure.exception.GlobalExceptionHandler;
import com.cotalk.infrastructure.ratelimit.RateLimitTestConfiguration;
import com.cotalk.infrastructure.security.JwtAuthenticationFilter;
import com.cotalk.infrastructure.security.JwtTokenProvider;
import com.cotalk.infrastructure.security.WithMockCustomUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FileController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({RateLimitTestConfiguration.class, GlobalExceptionHandler.class})
class FileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UploadFileUseCase uploadFileUseCase;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Nested
    @DisplayName("파일 업로드 API")
    class UploadFileApi {

        @Test
        @DisplayName("이미지 파일 업로드 성공")
        @WithMockCustomUser(userId = 1L)
        void should_uploadImage_when_validImageFile() throws Exception {
            // given
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "test-image.jpg",
                    "image/jpeg",
                    "fake image content".getBytes()
            );

            FileUploadResult result = new FileUploadResult(
                    "https://storage.example.com/uploads/1/abc123.jpg",
                    "abc123.jpg",
                    "image/jpeg",
                    file.getSize()
            );

            given(uploadFileUseCase.uploadFile(any(FileUploadCommand.class))).willReturn(result);

            // when & then
            mockMvc.perform(multipart("/api/v1/files/upload")
                            .file(file))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.fileUrl").value(result.fileUrl()))
                    .andExpect(jsonPath("$.fileName").value(result.fileName()))
                    .andExpect(jsonPath("$.contentType").value("image/jpeg"))
                    .andExpect(jsonPath("$.fileSize").value(file.getSize()))
                    .andExpect(jsonPath("$.isImage").value(true));
        }

        @Test
        @DisplayName("PDF 파일 업로드 성공")
        @WithMockCustomUser(userId = 1L)
        void should_uploadPdf_when_validPdfFile() throws Exception {
            // given
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "document.pdf",
                    "application/pdf",
                    "fake pdf content".getBytes()
            );

            FileUploadResult result = new FileUploadResult(
                    "https://storage.example.com/uploads/1/abc123.pdf",
                    "abc123.pdf",
                    "application/pdf",
                    file.getSize()
            );

            given(uploadFileUseCase.uploadFile(any(FileUploadCommand.class))).willReturn(result);

            // when & then
            mockMvc.perform(multipart("/api/v1/files/upload")
                            .file(file))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.contentType").value("application/pdf"))
                    .andExpect(jsonPath("$.isImage").value(false));
        }

        @Test
        @DisplayName("지원하지 않는 파일 형식 업로드 시 400 에러")
        @WithMockCustomUser(userId = 1L)
        void should_returnBadRequest_when_unsupportedFileType() throws Exception {
            // given
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "malware.exe",
                    "application/x-msdownload",
                    "fake exe content".getBytes()
            );

            given(uploadFileUseCase.uploadFile(any(FileUploadCommand.class)))
                    .willThrow(FileUploadException.invalidFileType("application/x-msdownload"));

            // when & then
            mockMvc.perform(multipart("/api/v1/files/upload")
                            .file(file))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("파일 크기 초과 시 400 에러")
        @WithMockCustomUser(userId = 1L)
        void should_returnBadRequest_when_fileTooLarge() throws Exception {
            // given
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "large-file.jpg",
                    "image/jpeg",
                    new byte[1024] // actual content doesn't matter
            );

            given(uploadFileUseCase.uploadFile(any(FileUploadCommand.class)))
                    .willThrow(FileUploadException.fileTooLarge(10485760L));

            // when & then
            mockMvc.perform(multipart("/api/v1/files/upload")
                            .file(file))
                    .andExpect(status().isBadRequest());
        }
    }
}
