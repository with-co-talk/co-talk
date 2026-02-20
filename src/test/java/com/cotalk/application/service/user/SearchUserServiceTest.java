package com.cotalk.application.service.user;

import com.cotalk.domain.entity.User;
import com.cotalk.domain.model.Email;
import com.cotalk.domain.port.outbound.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SearchUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SearchUserService searchUserService;

    @Test
    @DisplayName("닉네임으로 사용자 검색 성공")
    void should_returnUsers_when_searchByNickname() {
        // given
        String nickname = "테스트";
        int limit = 50;
        List<User> expectedUsers = List.of(
                User.builder()
                        .id(1L)
                        .email(new Email("user1@example.com"))
                        .nickname("테스트유저1")
                        .passwordHash("hash")
                        .build(),
                User.builder()
                        .id(2L)
                        .email(new Email("user2@example.com"))
                        .nickname("테스트유저2")
                        .passwordHash("hash")
                        .build()
        );

        given(userRepository.findByNicknameContaining(nickname, limit)).willReturn(expectedUsers);

        // when
        List<User> result = searchUserService.searchByNickname(nickname);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getNickname()).isEqualTo("테스트유저1");
        assertThat(result.get(1).getNickname()).isEqualTo("테스트유저2");
        verify(userRepository).findByNicknameContaining(nickname, limit);
    }

    @Test
    @DisplayName("검색 결과가 없을 때 빈 리스트 반환")
    void should_returnEmptyList_when_noResults() {
        // given
        String nickname = "없는유저";
        int limit = 50;
        given(userRepository.findByNicknameContaining(nickname, limit)).willReturn(List.of());

        // when
        List<User> result = searchUserService.searchByNickname(nickname);

        // then
        assertThat(result).isEmpty();
        verify(userRepository).findByNicknameContaining(nickname, limit);
    }
}
