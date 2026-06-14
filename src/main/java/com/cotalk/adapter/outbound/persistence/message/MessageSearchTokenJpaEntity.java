package com.cotalk.adapter.outbound.persistence.message;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Persistable;

import java.io.Serializable;
import java.util.Objects;

/**
 * 메시지 검색 블라인드 인덱스 토큰 JPA 엔티티.
 *
 * <p>{@code (message_id, token)} 복합 키를 갖는 조인 테이블이다. 자체 surrogate key가
 * 무의미하여 {@link IdClass}로 복합 PK를 표현한다. 검색 조회는 {@link MessageJpaRepository}의
 * 토큰 조인 쿼리가 수행하며, 이 엔티티는 적재(write) 경로에서만 사용된다.</p>
 *
 * <p><b>성능:</b> 복합 PK에 {@code @GeneratedValue}가 없어 Spring Data가 엔티티를 detached로
 * 간주하면 {@code saveAll}이 행마다 {@code merge}(=SELECT 후 INSERT)를 실행한다. 긴 메시지는
 * 토큰이 수천 개라 SELECT 폭증으로 이어진다. {@link Persistable#isNew()}를 항상 {@code true}로
 * 반환해 {@code persist}(순수 INSERT) 경로를 강제하고, JDBC batch insert와 결합해 적재 부하를
 * 낮춘다. 토큰은 항상 신규 적재만 하므로(수정 시 {@code deleteByMessageId} 후 재적재) 안전하다.</p>
 *
 * @author seunggu.lee
 */
@Entity
@Table(name = "message_search_tokens")
@IdClass(MessageSearchTokenJpaEntity.MessageSearchTokenId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class MessageSearchTokenJpaEntity implements Persistable<MessageSearchTokenJpaEntity.MessageSearchTokenId> {

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
     * {@inheritDoc}
     *
     * <p>복합 식별자를 반환한다. (Spring Data {@link Persistable} 계약)</p>
     */
    @Override
    @Transient
    public MessageSearchTokenId getId() {
        return new MessageSearchTokenId(messageId, token);
    }

    /**
     * {@inheritDoc}
     *
     * <p>항상 신규 엔티티로 취급한다. 토큰은 적재(INSERT) 경로에서만 생성되며, 수정 시에는
     * {@code deleteByMessageId}로 전량 삭제 후 재적재하므로 {@code merge}의 사전 SELECT가
     * 불필요하다. {@code true}를 반환해 {@code persist}(순수 INSERT)와 JDBC batch insert를
     * 활성화한다.</p>
     *
     * @return 항상 {@code true}
     */
    @Override
    @Transient
    public boolean isNew() {
        return true;
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
