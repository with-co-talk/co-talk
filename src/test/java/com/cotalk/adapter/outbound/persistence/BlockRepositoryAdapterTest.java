package com.cotalk.adapter.outbound.persistence;

import com.cotalk.adapter.outbound.persistence.friend.BlockRepositoryAdapter;
import com.cotalk.adapter.outbound.persistence.mapper.UserMapper;
import com.cotalk.adapter.outbound.persistence.user.UserRepositoryAdapter;
import com.cotalk.domain.entity.Block;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.model.Email;
import com.cotalk.infrastructure.config.JpaAuditingConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BlockRepositoryAdapter 테스트.
 *
 * @author seunggu.lee
 */
@DataJpaTest
@ActiveProfiles("test")
@Import({BlockRepositoryAdapter.class, UserRepositoryAdapter.class, UserMapper.class, JpaAuditingConfig.class})
@DisplayName("BlockRepositoryAdapter")
class BlockRepositoryAdapterTest {

    @Autowired
    private BlockRepositoryAdapter blockRepository;

    @Autowired
    private UserRepositoryAdapter userRepository;

    private User user1;
    private User user2;
    private User user3;

    @BeforeEach
    void setUp() {
        user1 = userRepository.save(User.builder()
                .id(1L)
                .email(new Email("user1@example.com"))
                .passwordHash("hash")
                .nickname("user1")
                .build());

        user2 = userRepository.save(User.builder()
                .id(2L)
                .email(new Email("user2@example.com"))
                .passwordHash("hash")
                .nickname("user2")
                .build());

        user3 = userRepository.save(User.builder()
                .id(3L)
                .email(new Email("user3@example.com"))
                .passwordHash("hash")
                .nickname("user3")
                .build());
    }

    @Nested
    @DisplayName("저장 시")
    class Save {

        @Test
        @DisplayName("차단 정보를 저장한다")
        void should_saveBlock_when_blockProvided() {
            // given
            Block block = Block.builder()
                    .id(100L)
                    .blockerId(user1.getId())
                    .blockedId(user2.getId())
                    .build();

            // when
            Block saved = blockRepository.save(block);

            // then
            assertThat(saved.getId()).isEqualTo(100L);
            assertThat(saved.getBlockerId()).isEqualTo(user1.getId());
            assertThat(saved.getBlockedId()).isEqualTo(user2.getId());
        }
    }

    @Nested
    @DisplayName("조회 시")
    class Find {

        @Test
        @DisplayName("ID로 차단 정보를 조회한다")
        void should_findBlock_when_idProvided() {
            // given
            blockRepository.save(Block.builder()
                    .id(100L)
                    .blockerId(user1.getId())
                    .blockedId(user2.getId())
                    .build());

            // when
            Optional<Block> found = blockRepository.findById(100L);

            // then
            assertThat(found).isPresent();
            assertThat(found.get().getBlockerId()).isEqualTo(user1.getId());
        }

        @Test
        @DisplayName("차단자 ID와 피차단자 ID로 차단 정보를 조회한다")
        void should_findBlock_when_blockerIdAndBlockedIdProvided() {
            // given
            blockRepository.save(Block.builder()
                    .id(100L)
                    .blockerId(user1.getId())
                    .blockedId(user2.getId())
                    .build());

            // when
            Optional<Block> found = blockRepository.findByBlockerIdAndBlockedId(
                    user1.getId(), user2.getId());

            // then
            assertThat(found).isPresent();
            assertThat(found.get().getId()).isEqualTo(100L);
        }

        @Test
        @DisplayName("차단자 ID로 차단 목록을 조회한다")
        void should_findBlocks_when_blockerIdProvided() {
            // given
            blockRepository.save(Block.builder()
                    .id(100L)
                    .blockerId(user1.getId())
                    .blockedId(user2.getId())
                    .build());
            blockRepository.save(Block.builder()
                    .id(101L)
                    .blockerId(user1.getId())
                    .blockedId(user3.getId())
                    .build());

            // when
            List<Block> blocks = blockRepository.findByBlockerId(user1.getId());

            // then
            assertThat(blocks).hasSize(2);
        }

        @Test
        @DisplayName("차단 목록이 없으면 빈 목록을 반환한다")
        void should_returnEmptyList_when_noBlocks() {
            // when
            List<Block> blocks = blockRepository.findByBlockerId(user1.getId());

            // then
            assertThat(blocks).isEmpty();
        }
    }

    @Nested
    @DisplayName("존재 여부 확인 시")
    class Exists {

        @Test
        @DisplayName("차단 관계가 존재하면 true를 반환한다")
        void should_returnTrue_when_blockExists() {
            // given
            blockRepository.save(Block.builder()
                    .id(100L)
                    .blockerId(user1.getId())
                    .blockedId(user2.getId())
                    .build());

            // when & then
            assertThat(blockRepository.existsByBlockerIdAndBlockedId(
                    user1.getId(), user2.getId())).isTrue();
        }

        @Test
        @DisplayName("차단 관계가 존재하지 않으면 false를 반환한다")
        void should_returnFalse_when_blockNotExists() {
            // when & then
            assertThat(blockRepository.existsByBlockerIdAndBlockedId(
                    user1.getId(), user2.getId())).isFalse();
        }

        @Test
        @DisplayName("반대 방향 차단은 별개로 취급한다")
        void should_distinguishDirection_when_checking() {
            // given
            blockRepository.save(Block.builder()
                    .id(100L)
                    .blockerId(user1.getId())
                    .blockedId(user2.getId())
                    .build());

            // when & then
            assertThat(blockRepository.existsByBlockerIdAndBlockedId(
                    user1.getId(), user2.getId())).isTrue();
            assertThat(blockRepository.existsByBlockerIdAndBlockedId(
                    user2.getId(), user1.getId())).isFalse();
        }
    }

    @Nested
    @DisplayName("삭제 시")
    class Delete {

        @Test
        @DisplayName("차단 정보를 삭제한다")
        void should_deleteBlock_when_blockProvided() {
            // given
            Block block = blockRepository.save(Block.builder()
                    .id(100L)
                    .blockerId(user1.getId())
                    .blockedId(user2.getId())
                    .build());

            // when
            blockRepository.delete(block);

            // then
            assertThat(blockRepository.findById(100L)).isEmpty();
        }
    }
}
