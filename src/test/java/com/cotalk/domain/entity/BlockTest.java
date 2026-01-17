package com.cotalk.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Block 엔티티")
class BlockTest {

    @Nested
    @DisplayName("생성 시")
    class Creation {

        @Test
        @DisplayName("차단자 ID와 차단 대상 ID로 차단 관계를 생성할 수 있다")
        void should_createBlock_when_validInputsProvided() {
            // given
            Long blockerId = 1L;
            Long blockedId = 2L;

            // when
            Block block = Block.builder()
                    .id(1L)
                    .blockerId(blockerId)
                    .blockedId(blockedId)
                    .build();

            // then
            assertThat(block.getBlockerId()).isEqualTo(blockerId);
            assertThat(block.getBlockedId()).isEqualTo(blockedId);
        }
    }

    @Nested
    @DisplayName("isBlockedBy 메서드")
    class IsBlockedBy {

        @Test
        @DisplayName("차단자 ID가 일치하면 true를 반환한다")
        void should_returnTrue_when_userIdMatchesBlockerId() {
            // given
            Long blockerId = 1L;
            Long blockedId = 2L;
            Block block = Block.builder()
                    .id(1L)
                    .blockerId(blockerId)
                    .blockedId(blockedId)
                    .build();

            // when
            boolean result = block.isBlockedBy(blockerId);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("차단자 ID가 일치하지 않으면 false를 반환한다")
        void should_returnFalse_when_userIdDoesNotMatchBlockerId() {
            // given
            Long blockerId = 1L;
            Long blockedId = 2L;
            Block block = Block.builder()
                    .id(1L)
                    .blockerId(blockerId)
                    .blockedId(blockedId)
                    .build();

            // when
            boolean result = block.isBlockedBy(blockedId);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("다른 사용자 ID로 확인하면 false를 반환한다")
        void should_returnFalse_when_differentUserId() {
            // given
            Long blockerId = 1L;
            Long blockedId = 2L;
            Long otherUserId = 3L;
            Block block = Block.builder()
                    .id(1L)
                    .blockerId(blockerId)
                    .blockedId(blockedId)
                    .build();

            // when
            boolean result = block.isBlockedBy(otherUserId);

            // then
            assertThat(result).isFalse();
        }
    }
}
