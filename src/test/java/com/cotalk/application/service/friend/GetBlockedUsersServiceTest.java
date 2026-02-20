package com.cotalk.application.service.friend;

import com.cotalk.common.fixture.BlockTestFixture;
import com.cotalk.domain.entity.Block;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.port.outbound.BlockRepository;
import com.cotalk.domain.port.outbound.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static com.cotalk.common.fixture.UserTestFixture.createUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class GetBlockedUsersServiceTest {

    @Mock
    private BlockRepository blockRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private GetBlockedUsersService getBlockedUsersService;

    @Test
    @DisplayName("차단한 사용자 목록 조회 성공")
    void should_returnBlockedUsers_when_validBlockerId() {
        // given
        Long blockerId = 1L;
        Long blockedId1 = 2L;
        Long blockedId2 = 3L;

        Block block1 = BlockTestFixture.createBlock(100L, blockerId, blockedId1);
        Block block2 = BlockTestFixture.createBlock(101L, blockerId, blockedId2);

        User blockedUser1 = createUser(blockedId1, "blocked1@test.com", "차단유저1");
        User blockedUser2 = createUser(blockedId2, "blocked2@test.com", "차단유저2");

        given(blockRepository.findByBlockerId(blockerId)).willReturn(List.of(block1, block2));
        given(userRepository.findById(blockedId1)).willReturn(Optional.of(blockedUser1));
        given(userRepository.findById(blockedId2)).willReturn(Optional.of(blockedUser2));

        // when
        List<User> result = getBlockedUsersService.getBlockedUsers(blockerId);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getNickname()).isEqualTo("차단유저1");
        assertThat(result.get(1).getNickname()).isEqualTo("차단유저2");
    }

    @Test
    @DisplayName("차단한 사용자가 없으면 빈 목록 반환")
    void should_returnEmptyList_when_noBlockedUsers() {
        // given
        Long blockerId = 1L;

        given(blockRepository.findByBlockerId(blockerId)).willReturn(List.of());

        // when
        List<User> result = getBlockedUsersService.getBlockedUsers(blockerId);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("차단한 사용자가 삭제된 경우 필터링됨")
    void should_filterDeletedUsers_when_blockedUserNotFound() {
        // given
        Long blockerId = 1L;
        Long blockedId1 = 2L;
        Long blockedId2 = 3L; // 삭제된 사용자

        Block block1 = BlockTestFixture.createBlock(100L, blockerId, blockedId1);
        Block block2 = BlockTestFixture.createBlock(101L, blockerId, blockedId2);

        User blockedUser1 = createUser(blockedId1, "blocked1@test.com", "차단유저1");

        given(blockRepository.findByBlockerId(blockerId)).willReturn(List.of(block1, block2));
        given(userRepository.findById(blockedId1)).willReturn(Optional.of(blockedUser1));
        given(userRepository.findById(blockedId2)).willReturn(Optional.empty()); // 삭제된 사용자

        // when
        List<User> result = getBlockedUsersService.getBlockedUsers(blockerId);

        // then - 삭제된 사용자는 필터링되어 결과에 포함되지 않음
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getNickname()).isEqualTo("차단유저1");
    }
}
