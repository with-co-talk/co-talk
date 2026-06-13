package com.cotalk.domain.validator;

import com.cotalk.domain.exception.BlockedRelationshipException;
import com.cotalk.domain.port.outbound.BlockRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("BlockValidator 단위 테스트")
class BlockValidatorTest {

    @Mock
    private BlockRepository blockRepository;

    @InjectMocks
    private BlockValidator blockValidator;

    @Test
    @DisplayName("차단 관계가 없으면 예외가 발생하지 않는다")
    void should_notThrow_when_noBlockRelationship() {
        // given
        Long userId1 = 1L;
        Long userId2 = 2L;
        given(blockRepository.existsByBlockerIdAndBlockedId(userId1, userId2)).willReturn(false);
        given(blockRepository.existsByBlockerIdAndBlockedId(userId2, userId1)).willReturn(false);

        // when & then
        assertThatCode(() -> blockValidator.validateNotBlocked(userId1, userId2))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("내가 상대를 차단한 경우 예외가 발생한다")
    void should_throw_when_iBlockedTheOther() {
        // given
        Long userId1 = 1L;
        Long userId2 = 2L;
        given(blockRepository.existsByBlockerIdAndBlockedId(userId1, userId2)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> blockValidator.validateNotBlocked(userId1, userId2))
                .isInstanceOf(BlockedRelationshipException.class);
    }

    @Test
    @DisplayName("상대가 나를 차단한 경우 예외가 발생한다 (양방향)")
    void should_throw_when_theOtherBlockedMe() {
        // given
        Long userId1 = 1L;
        Long userId2 = 2L;
        given(blockRepository.existsByBlockerIdAndBlockedId(userId1, userId2)).willReturn(false);
        given(blockRepository.existsByBlockerIdAndBlockedId(userId2, userId1)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> blockValidator.validateNotBlocked(userId1, userId2))
                .isInstanceOf(BlockedRelationshipException.class);
    }

    @Test
    @DisplayName("내가 차단했으면 상대 방향은 조회하지 않는다 (단축 평가)")
    void should_shortCircuit_when_iBlockedTheOther() {
        // given
        Long userId1 = 1L;
        Long userId2 = 2L;
        given(blockRepository.existsByBlockerIdAndBlockedId(userId1, userId2)).willReturn(true);
        lenient().when(blockRepository.existsByBlockerIdAndBlockedId(userId2, userId1)).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> blockValidator.validateNotBlocked(userId1, userId2))
                .isInstanceOf(BlockedRelationshipException.class);
        verify(blockRepository, never()).existsByBlockerIdAndBlockedId(userId2, userId1);
    }
}
