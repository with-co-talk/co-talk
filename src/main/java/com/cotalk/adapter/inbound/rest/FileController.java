package com.cotalk.adapter.inbound.rest;

import com.cotalk.adapter.inbound.rest.dto.auth.FileUploadResponse;
import com.cotalk.domain.port.inbound.file.UploadFileUseCase;
import com.cotalk.domain.port.inbound.file.UploadFileUseCase.FileUploadCommand;
import com.cotalk.domain.port.inbound.file.UploadFileUseCase.FileUploadResult;
import com.cotalk.infrastructure.security.SecurityContextHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 파일 업로드를 위한 REST 컨트롤러.
 * 이미지, PDF, 문서 등의 파일 업로드 기능을 제공한다.
 *
 * @author seunggu.lee
 */
@Tag(name = "File", description = "파일 업로드 API")
@RestController
@RequestMapping("/api/v1/files")
public class FileController {

    private final UploadFileUseCase uploadFileUseCase;
    private final SecurityContextHelper securityContextHelper;

    /**
     * FileController 생성자.
     *
     * @param uploadFileUseCase     파일 업로드 유스케이스
     * @param securityContextHelper 보안 컨텍스트 헬퍼
     */
    public FileController(UploadFileUseCase uploadFileUseCase,
                          SecurityContextHelper securityContextHelper) {
        this.uploadFileUseCase = uploadFileUseCase;
        this.securityContextHelper = securityContextHelper;
    }

    /**
     * 파일을 업로드한다.
     * 인증된 사용자만 파일을 업로드할 수 있으며, 사용자 ID는 SecurityContext에서 자동으로 추출된다.
     *
     * @param file 업로드할 파일
     * @return 업로드된 파일 정보 (URL, 파일명, 타입, 크기, 이미지 여부)
     * @throws IOException 파일 읽기 중 오류 발생 시
     */
    @Operation(summary = "파일 업로드", description = "이미지, PDF, 문서 등의 파일을 업로드합니다. 인증된 사용자만 업로드 가능합니다.")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileUploadResponse> uploadFile(
            @RequestParam("file") MultipartFile file) throws IOException {

        Long userId = securityContextHelper.getCurrentUserId();

        FileUploadCommand command = new FileUploadCommand(
                userId,
                file.getInputStream(),
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize()
        );

        FileUploadResult result = uploadFileUseCase.uploadFile(command);

        return ResponseEntity.ok(FileUploadResponse.of(
                result.fileUrl(),
                result.fileName(),
                result.contentType(),
                result.fileSize(),
                isImageType(result.contentType())
        ));
    }

    /**
     * 주어진 콘텐츠 타입이 이미지인지 확인한다.
     *
     * @param contentType 확인할 콘텐츠 타입
     * @return 이미지 여부
     */
    private boolean isImageType(String contentType) {
        return contentType != null && contentType.startsWith("image/");
    }
}
