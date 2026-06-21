package com.cotalk.adapter.outbound.persistence.mapper;

import com.cotalk.adapter.outbound.persistence.entity.BlockJpaEntity;
import com.cotalk.domain.entity.Block;
import org.springframework.stereotype.Component;

/**
 * Block 도메인과 BlockJpaEntity 간 매핑.
 *
 * @author seunggu.lee
 */
@Component
public class BlockMapper {

    /**
     * JPA 엔티티를 도메인 엔티티로 변환한다.
     *
     * @param jpa JPA 엔티티
     * @return 도메인 엔티티, jpa가 null이면 null
     */
    public Block toDomain(BlockJpaEntity jpa) {
        if (jpa == null) {
            return null;
        }
        return Block.builder()
                .id(jpa.getId())
                .blockerId(jpa.getBlockerId())
                .blockedId(jpa.getBlockedId())
                .createdAt(jpa.getCreatedAt())
                .updatedAt(jpa.getUpdatedAt())
                .build();
    }

    /**
     * 도메인 엔티티를 JPA 엔티티로 변환한다.
     *
     * @param domain 도메인 엔티티
     * @return JPA 엔티티, domain이 null이면 null
     */
    public BlockJpaEntity toJpa(Block domain) {
        if (domain == null) {
            return null;
        }
        BlockJpaEntity jpa = BlockJpaEntity.builder()
                .id(domain.getId())
                .blockerId(domain.getBlockerId())
                .blockedId(domain.getBlockedId())
                .build();
        if (domain.getCreatedAt() != null) {
            jpa.setCreatedAt(domain.getCreatedAt());
        }
        if (domain.getUpdatedAt() != null) {
            jpa.setUpdatedAt(domain.getUpdatedAt());
        }
        return jpa;
    }
}
