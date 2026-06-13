package com.cotalk.infrastructure.config;

import com.cotalk.domain.port.outbound.BlockRepository;
import com.cotalk.domain.port.outbound.ChatRoomMemberRepository;
import com.cotalk.domain.port.outbound.UserRepository;
import com.cotalk.domain.validator.BlockValidator;
import com.cotalk.domain.validator.ChatRoomMemberValidator;
import com.cotalk.domain.validator.FileMessageValidator;
import com.cotalk.domain.validator.MessageValidator;
import com.cotalk.domain.validator.UserValidator;
import com.cotalk.domain.port.outbound.FileStorage;
import com.cotalk.domain.service.FileObjectResolver;
import com.cotalk.infrastructure.config.properties.MinioProperties;
import com.cotalk.infrastructure.storage.InMemoryFileStorage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * 도메인 검증기 빈 설정.
 * 도메인 계층은 Spring 어노테이션을 사용하지 않으므로, 여기서 빈으로 등록한다.
 */
@Configuration
public class DomainValidatorConfig {

    @Bean
    public UserValidator userValidator(UserRepository userRepository) {
        return new UserValidator(userRepository);
    }

    @Bean
    public MessageValidator messageValidator() {
        return new MessageValidator();
    }

    @Bean
    public ChatRoomMemberValidator chatRoomMemberValidator(ChatRoomMemberRepository chatRoomMemberRepository) {
        return new ChatRoomMemberValidator(chatRoomMemberRepository);
    }

    /**
     * 차단 관계 검증기 빈.
     * 메시지 전송 · 친구 요청 · 1:1 채팅방 생성/재초대 시 양방향 차단 관계를 검증한다.
     *
     * @param blockRepository 차단 레포지토리 포트
     * @return 차단 관계 검증기
     */
    @Bean
    public BlockValidator blockValidator(BlockRepository blockRepository) {
        return new BlockValidator(blockRepository);
    }

    /**
     * 파일 메시지 검증기 빈.
     * <p>
     * 실제 파일 저장소가 업로드 응답으로 반환하는 URL의 베이스 주소를 화이트리스트로 주입한다.
     * MinIO 활성화 시 공개 URL({@code minio.public-url}) + 엔드포인트({@code minio.endpoint})에
     * 버킷 경로를 붙인 베이스를, 비활성화(InMemory) 시 인메모리 베이스 URL을 허용한다.
     * 이를 통해 외부 호스트가 동일한 path를 흉내 내더라도 host 불일치로 거부한다.
     * </p>
     *
     * @param minioProperties MinIO 설정(베이스 URL/버킷 추출용)
     * @return host 화이트리스트가 적용된 FileMessageValidator
     */
    @Bean
    public FileMessageValidator fileMessageValidator(MinioProperties minioProperties) {
        List<String> allowedBaseUrls = new ArrayList<>();
        if (minioProperties.enabled()) {
            String bucket = minioProperties.bucket();
            // MinIO URL: {publicUrl|endpoint}/{bucket}/uploads/{userId}/{file}
            addBaseWithBucket(allowedBaseUrls, minioProperties.publicUrl(), bucket);
            addBaseWithBucket(allowedBaseUrls, minioProperties.endpoint(), bucket);
        } else {
            // InMemory URL: {baseUrl}/uploads/{userId}/{file}
            allowedBaseUrls.add(InMemoryFileStorage.BASE_URL);
        }
        return new FileMessageValidator(allowedBaseUrls);
    }

    /**
     * 불투명 식별자(object-id) 기반 파일 메시지 메타 재구성기 빈.
     * <p>
     * 업로드가 발급한 저장 객체 키만으로 소유·존재를 검증하고 URL/메타를 재구성한다.
     * 파일 저장소 포트({@link FileStorage})에 의존한다.
     * </p>
     *
     * @param fileStorage 파일 저장소 포트(MinIO 또는 InMemory)
     * @return FileObjectResolver
     */
    @Bean
    public FileObjectResolver fileObjectResolver(FileStorage fileStorage) {
        return new FileObjectResolver(fileStorage);
    }

    private void addBaseWithBucket(List<String> target, String baseUrl, String bucket) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return;
        }
        String normalized = baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1)
                : baseUrl;
        target.add(normalized + "/" + bucket);
    }
}
