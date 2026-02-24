package com.cotalk.application.service.friend;

import com.cotalk.common.fixture.BlockTestFixture;
import com.cotalk.domain.entity.Block;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.exception.InvalidBlockException;
import com.cotalk.domain.exception.SelfActionNotAllowedException;
import com.cotalk.domain.exception.UserNotFoundException;
import com.cotalk.domain.port.outbound.BlockRepository;
import com.cotalk.domain.validator.UserValidator;
import com.cotalk.infrastructure.id.SnowflakeIdGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.cotalk.common.fixture.UserTestFixture.createUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BlockUserServiceTest {

    @Mock
    private BlockRepository blockRepository;

    @Mock
    private UserValidator userValidator;

    @Mock
    private SnowflakeIdGenerator idGenerator;

    @InjectMocks
    private BlockUserService blockUserService;

    @Test
    @DisplayName("사용자 차단 성공")
    void should_blockUser_when_validInput() {
        // given
        Long blockerId = 1L;
        Long blockedId = 2L;
        Long blockId = 100L;

        doNothing().when(userValidator).validateNotSelfAction(blockerId, blockedId, "차단");
        given(userValidator.validateUserExists(blockerId)).willReturn(createUser(blockerId));
        given(userValidator.validateUserExists(blockedId)).willReturn(createUser(blockedId));
        given(blockRepository.findByBlockerIdAndBlockedId(blockerId, blockedId)).willReturn(Optional.empty());
        given(idGenerator.nextId()).willReturn(blockId);
        given(blockRepository.save(any(Block.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        blockUserService.blockUser(blockerId, blockedId);

        // then
        ArgumentCaptor<Block> blockCaptor = ArgumentCaptor.forClass(Block.class);
        verify(blockRepository).save(blockCaptor.capture());
        assertThat(blockCaptor.getValue().getBlockerId()).isEqualTo(blockerId);
        assertThat(blockCaptor.getValue().getBlockedId()).isEqualTo(blockedId);
    }

    @Test
    @DisplayName("자기 자신을 차단하면 예외 발생")
    void should_throwException_when_blockingSelf() {
        // given
        Long userId = 1L;
        willThrow(new SelfActionNotAllowedException("차단"))
                .given(userValidator).validateNotSelfAction(userId, userId, "차단");

        // when & then
        assertThatThrownBy(() -> blockUserService.blockUser(userId, userId))
                .isInstanceOf(SelfActionNotAllowedException.class)
                .hasMessageContaining("자기 자신을 차단할 수 없습니다");
    }

    @Test
    @DisplayName("존재하지 않는 사용자를 차단하면 예외 발생")
    void should_throwException_when_blockedUserNotFound() {
        // given
        Long blockerId = 1L;
        Long blockedId = 999L;

        doNothing().when(userValidator).validateNotSelfAction(blockerId, blockedId, "차단");
        given(userValidator.validateUserExists(blockerId)).willReturn(createUser(blockerId));
        willThrow(new UserNotFoundException(blockedId))
                .given(userValidator).validateUserExists(blockedId);

        // when & then
        assertThatThrownBy(() -> blockUserService.blockUser(blockerId, blockedId))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("이미 차단한 사용자면 예외 발생")
    void should_throwException_when_alreadyBlocked() {
        // given
        Long blockerId = 1L;
        Long blockedId = 2L;

        Block existingBlock = BlockTestFixture.createBlock(100L, blockerId, blockedId);

        doNothing().when(userValidator).validateNotSelfAction(blockerId, blockedId, "차단");
        given(userValidator.validateUserExists(blockerId)).willReturn(createUser(blockerId));
        given(userValidator.validateUserExists(blockedId)).willReturn(createUser(blockedId));
        given(blockRepository.findByBlockerIdAndBlockedId(blockerId, blockedId)).willReturn(Optional.of(existingBlock));

        // when & then
        assertThatThrownBy(() -> blockUserService.blockUser(blockerId, blockedId))
                .isInstanceOf(InvalidBlockException.class)
                .hasMessageContaining("이미 차단한 사용자입니다");
    }
}
