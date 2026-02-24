package com.cotalk.common.fixture;

import com.cotalk.domain.entity.Block;

/**
 * Block 엔티티 테스트 픽스처.
 * 테스트에서 반복적으로 사용되는 Block 객체 생성 메서드를 제공한다.
 *
 * @author seunggu.lee
 */
public class BlockTestFixture {

    private static final Long DEFAULT_ID = 1L;
    private static final Long DEFAULT_BLOCKER_ID = 1L;
    private static final Long DEFAULT_BLOCKED_ID = 2L;

    /**
     * 기본값으로 Block 객체를 생성한다.
     * (id=1, blockerId=1, blockedId=2)
     *
     * @return Block 엔티티
     */
    public static Block createBlock() {
        return createBlock(DEFAULT_ID, DEFAULT_BLOCKER_ID, DEFAULT_BLOCKED_ID);
    }

    /**
     * 지정된 차단자 ID와 피차단자 ID로 Block 객체를 생성한다.
     *
     * @param blockerId 차단자 ID
     * @param blockedId 피차단자 ID
     * @return Block 엔티티
     */
    public static Block createBlock(Long blockerId, Long blockedId) {
        return createBlock(DEFAULT_ID, blockerId, blockedId);
    }

    /**
     * 지정된 ID, 차단자 ID, 피차단자 ID로 Block 객체를 생성한다.
     *
     * @param id        차단 ID
     * @param blockerId 차단자 ID
     * @param blockedId 피차단자 ID
     * @return Block 엔티티
     */
    public static Block createBlock(Long id, Long blockerId, Long blockedId) {
        return Block.builder()
                .id(id)
                .blockerId(blockerId)
                .blockedId(blockedId)
                .build();
    }

    /**
     * 빌더 스타일로 Block 생성을 시작한다.
     *
     * @return BlockBuilder 인스턴스
     */
    public static BlockBuilder builder() {
        return new BlockBuilder();
    }

    /**
     * Block 테스트 빌더.
     */
    public static class BlockBuilder {
        private Long id = DEFAULT_ID;
        private Long blockerId = DEFAULT_BLOCKER_ID;
        private Long blockedId = DEFAULT_BLOCKED_ID;

        /**
         * 차단 ID를 설정한다.
         *
         * @param id 차단 ID
         * @return 빌더
         */
        public BlockBuilder id(Long id) {
            this.id = id;
            return this;
        }

        /**
         * 차단자 ID를 설정한다.
         *
         * @param blockerId 차단자 ID
         * @return 빌더
         */
        public BlockBuilder blockerId(Long blockerId) {
            this.blockerId = blockerId;
            return this;
        }

        /**
         * 피차단자 ID를 설정한다.
         *
         * @param blockedId 피차단자 ID
         * @return 빌더
         */
        public BlockBuilder blockedId(Long blockedId) {
            this.blockedId = blockedId;
            return this;
        }

        /**
         * Block 객체를 생성한다.
         *
         * @return 생성된 Block 엔티티
         */
        public Block build() {
            return Block.builder()
                    .id(id)
                    .blockerId(blockerId)
                    .blockedId(blockedId)
                    .build();
        }
    }
}
