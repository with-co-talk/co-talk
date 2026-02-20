package com.cotalk.application.service.friend;

import com.cotalk.common.fixture.BlockTestFixture;
import com.cotalk.domain.entity.Block;
import com.cotalk.domain.exception.BlockNotFoundException;
import com.cotalk.domain.port.outbound.BlockRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UnblockUserServiceTest {

    @Mock
    private BlockRepository blockRepository;

    @InjectMocks
    private UnblockUserService unblockUserService;

    @Test
    @DisplayName("차단 해제 성공")
    void should_unblockUser_when_validInput() {
        // given
        Long blockerId = 1L;
        Long blockedId = 2L;

        Block block = BlockTestFixture.createBlock(100L, blockerId, blockedId);

        given(blockRepository.findByBlockerIdAndBlockedId(blockerId, blockedId))
                .willReturn(Optional.of(block));

        // when
        unblockUserService.unblockUser(blockerId, blockedId);

        // then
        verify(blockRepository).delete(block);
    }

    @Test
    @DisplayName("차단하지 않은 사용자를 해제하면 예외 발생")
    void should_throwException_when_notBlocked() {
        // given
        Long blockerId = 1L;
        Long blockedId = 2L;

        given(blockRepository.findByBlockerIdAndBlockedId(blockerId, blockedId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> unblockUserService.unblockUser(blockerId, blockedId))
                .isInstanceOf(BlockNotFoundException.class);
    }
}
