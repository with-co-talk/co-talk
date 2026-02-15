package com.cotalk.infrastructure.config;

import com.cotalk.domain.port.outbound.FileStorage;
import com.cotalk.domain.service.UploadFileService;
import com.cotalk.infrastructure.config.properties.FileUploadProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * UploadFileService 빈 설정.
 * Domain 서비스가 Infrastructure 설정에 직접 의존하지 않도록
 * Configuration 클래스에서 필요한 값을 주입한다.
 *
 * @author seunggu.lee
 */
@Configuration
public class UploadFileServiceConfig {

    /**
     * UploadFileService 빈을 생성한다.
     *
     * @param fileStorage          파일 저장소
     * @param fileUploadProperties 파일 업로드 설정
     * @return UploadFileService 인스턴스
     */
    @Bean
    public UploadFileService uploadFileService(FileStorage fileStorage,
                                                FileUploadProperties fileUploadProperties) {
        return new UploadFileService(fileStorage, fileUploadProperties.maxSize());
    }
}
