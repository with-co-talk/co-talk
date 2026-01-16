package com.cotalk.adapter.inbound.rest;

import com.cotalk.domain.port.inbound.UploadFileUseCase;
import com.cotalk.domain.port.inbound.UploadFileUseCase.FileUploadCommand;
import com.cotalk.domain.port.inbound.UploadFileUseCase.FileUploadResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Tag(name = "File", description = "파일 업로드 API")
@RestController
@RequestMapping("/api/v1/files")
public class FileController {

    private final UploadFileUseCase uploadFileUseCase;

    public FileController(UploadFileUseCase uploadFileUseCase) {
        this.uploadFileUseCase = uploadFileUseCase;
    }

    @Operation(summary = "파일 업로드", description = "이미지, PDF, 문서 등의 파일을 업로드합니다.")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileUploadResponse> uploadFile(
            @RequestParam Long userId,
            @RequestParam("file") MultipartFile file) throws IOException {

        FileUploadCommand command = new FileUploadCommand(
                userId,
                file.getInputStream(),
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize()
        );

        FileUploadResult result = uploadFileUseCase.uploadFile(command);

        return ResponseEntity.ok(new FileUploadResponse(
                result.fileUrl(),
                result.fileName(),
                result.contentType(),
                result.fileSize(),
                isImageType(result.contentType())
        ));
    }

    private boolean isImageType(String contentType) {
        return contentType != null && contentType.startsWith("image/");
    }

    public record FileUploadResponse(
            String fileUrl,
            String fileName,
            String contentType,
            long fileSize,
            boolean isImage
    ) {}
}
