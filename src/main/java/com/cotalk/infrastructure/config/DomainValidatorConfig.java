package com.cotalk.infrastructure.config;

import com.cotalk.domain.port.outbound.ChatRoomMemberRepository;
import com.cotalk.domain.port.outbound.UserRepository;
import com.cotalk.domain.validator.ChatRoomMemberValidator;
import com.cotalk.domain.validator.FileMessageValidator;
import com.cotalk.domain.validator.MessageValidator;
import com.cotalk.domain.validator.UserValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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

    @Bean
    public FileMessageValidator fileMessageValidator() {
        return new FileMessageValidator();
    }
}
