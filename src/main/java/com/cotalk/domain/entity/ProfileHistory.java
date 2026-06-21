package com.cotalk.domain.entity;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 프로필 이력 도메인 엔티티.
 * 사용자의 프로필 사진, 배경화면, 상태메시지 변경 이력을 관리한다.
 * 순수 도메인 모델이며 JPA 어노테이션은 persistence 계층에만 존재한다.
 *
 * @author seunggu.lee
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@SuperBuilder
public class ProfileHistory extends DomainBaseEntity {

    private Long id;

    private Long userId;

    private ProfileHistoryType type;

    private String url;

    private String content;

    @Builder.Default
    private boolean isPrivate = false;

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
