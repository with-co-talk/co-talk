package com.cotalk.application.service.user;

import com.cotalk.domain.entity.User;
import com.cotalk.domain.model.Email;
import com.cotalk.domain.port.inbound.user.FindEmailUseCase;
import com.cotalk.domain.port.outbound.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("FindEmailService")
class FindEmailServiceTest {

    @Mock
    private UserRepository userRepository;

    private FindEmailService findEmailService;

    @BeforeEach
    void setUp() {
        findEmailService = new FindEmailService(userRepository);
    }

    @Test
    @DisplayName("닉네임과 전화번호가 일치하면 마스킹된 이메일을 반환한다")
    void should_returnMaskedEmail_when_nicknameAndPhoneMatch() {
        // given
        User user = User.builder()
                .id(1L)
                .email(new Email("testuser@example.com"))
                .nickname("테스트")
                .phoneNumber("010-1234-5678")
                .build();
        given(userRepository.findByNicknameAndPhoneNumber("테스트", "010-1234-5678"))
                .willReturn(Optional.of(user));

        // when
        FindEmailUseCase.FindEmailResult result = findEmailService.findEmail("테스트", "010-1234-5678");

        // then
        assertThat(result.found()).isTrue();
        assertThat(result.maskedEmail()).isNotNull();
        assertThat(result.maskedEmail()).contains("@");
        assertThat(result.maskedEmail()).doesNotContain("testuser");
        assertThat(result.maskedEmail()).startsWith("te***");
        assertThat(result.message()).isEqualTo("이메일을 찾았습니다.");
    }

    @Test
    @DisplayName("닉네임과 전화번호가 일치하지 않으면 notFound를 반환한다")
    void should_returnNotFound_when_noMatch() {
        // given
        given(userRepository.findByNicknameAndPhoneNumber("없는유저", "010-0000-0000"))
                .willReturn(Optional.empty());

        // when
        FindEmailUseCase.FindEmailResult result = findEmailService.findEmail("없는유저", "010-0000-0000");

        // then
        assertThat(result.found()).isFalse();
        assertThat(result.maskedEmail()).isNull();
        assertThat(result.message()).isEqualTo("일치하는 계정을 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("이메일 마스킹이 올바르게 동작한다")
    void should_maskEmail_correctly() {
        // te***@exam***.com 형태
        String masked = findEmailService.maskEmail("test@example.com");
        assertThat(masked).startsWith("te***@");
        assertThat(masked).endsWith(".com");
        assertThat(masked).doesNotContain("test@example");
    }

    @Test
    @DisplayName("짧은 로컬 파트의 이메일도 마스킹된다")
    void should_maskShortEmail() {
        String masked = findEmailService.maskEmail("ab@c.com");
        assertThat(masked).contains("@");
        assertThat(masked).contains("***");
    }
}
