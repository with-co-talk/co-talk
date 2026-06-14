package com.cotalk.adapter.outbound.persistence.message;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

/**
 * 메시지 검색 블라인드 인덱스 토큰 JPA 엔티티.
 *
 * <p>{@code (message_id, token)} 복합 키를 갖는 조인 테이블이다. 자체 surrogate key가
 * 무의미하여 {@link IdClass}로 복합 PK를 표현한다. 검색 조회는 {@link MessageJpaRepository}의
 * 토큰 조인 쿼리가 수행하며, 이 엔티티는 적재(write) 경로에서만 사용된다.</p>
 *
 * @author seunggu.lee
 */
@Entity
@Table(name = "message_search_tokens")
@IdClass(MessageSearchTokenJpaEntity.MessageSearchTokenId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class MessageSearchTokenJpaEntity {

    @Id
    @Column(name = "message_id", nullable = false)
    private Long messageId;

    @Id
    @Column(name = "token", nullable = false, length = 24)
    private String token;

    /**
     * 토큰 엔티티를 생성한다.
     *
     * @param messageId 메시지 ID
     * @param token     블라인드 인덱스 토큰
     * @return 생성된 엔티티
     */
    public static MessageSearchTokenJpaEntity of(Long messageId, String token) {
        return new MessageSearchTokenJpaEntity(messageId, token);
    }

    /**
     * {@link MessageSearchTokenJpaEntity}의 복합 식별자.
     *
     * @author seunggu.lee
     */
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MessageSearchTokenId implements Serializable {

        private Long messageId;
        private String token;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof MessageSearchTokenId that)) {
                return false;
            }
            return Objects.equals(messageId, that.messageId) && Objects.equals(token, that.token);
        }

        @Override
        public int hashCode() {
            return Objects.hash(messageId, token);
        }
    }
}
