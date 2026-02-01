package com.cotalk.domain.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 프로필 이력 엔티티.
 * 사용자의 프로필 사진, 배경화면, 상태메시지 변경 이력을 관리한다.
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
public class ProfileHistory extends BaseEntity {

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

    /**
     * 나만보기 설정을 변경한다.
     *
     * @param isPrivate 나만보기 여부
     */
    public void updatePrivacy(boolean isPrivate) {
        this.isPrivate = isPrivate;
    }

    /**
     * 현재 사용 중 상태를 설정한다.
     *
     * @param isCurrent 현재 사용 중 여부
     */
    public void setCurrent(boolean isCurrent) {
        this.isCurrent = isCurrent;
    }

    /**
     * URL을 변경한다.
     *
     * @param url 새 URL
     */
    public void updateUrl(String url) {
        this.url = url;
    }

    /**
     * 내용을 변경한다.
     *
     * @param content 새 내용
     */
    public void updateContent(String content) {
        this.content = content;
    }
}
