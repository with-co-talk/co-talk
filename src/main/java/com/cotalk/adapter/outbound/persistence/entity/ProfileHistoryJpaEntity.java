package com.cotalk.adapter.outbound.persistence.entity;

import com.cotalk.domain.entity.ProfileHistoryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 프로필 이력 JPA 엔티티.
 * persistence 계층 전용이며, 도메인 ProfileHistory와 매핑된다.
 *
 * @author seunggu.lee
 */
@Entity
@Table(name = "profile_history", indexes = {
    @Index(name = "idx_profile_history_user_type", columnList = "userId, type")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ProfileHistoryJpaEntity extends BaseJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProfileHistoryType type;

    @Column(length = 500)
    private String url;

    @Column(length = 60)
    private String content;

    @Column(nullable = false)
    @Builder.Default
    private boolean isPrivate = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean isCurrent = false;
}
