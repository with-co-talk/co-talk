package com.cotalk.adapter.outbound.persistence.user;

import com.cotalk.adapter.outbound.persistence.entity.UserJpaEntity;
import com.cotalk.domain.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 사용자 JPA 리포지토리.
 * Spring Data JPA를 통해 사용자 데이터에 접근한다.
 * persistence 계층 전용이며, 도메인 반환은 Adapter에서 매핑한다.
 *
 * @author seunggu.lee
 */
public interface UserJpaRepository extends JpaRepository<UserJpaEntity, Long> {

    Optional<UserJpaEntity> findByEmail(String email);

    Optional<UserJpaEntity> findByOauthProviderAndOauthId(User.OAuthProvider oauthProvider, String oauthId);

    List<UserJpaEntity> findByNicknameContaining(String nickname);

    List<UserJpaEntity> findByNicknameContaining(String nickname, Pageable pageable);

    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);

    List<UserJpaEntity> findByStatus(User.UserStatus status);

    long countByStatus(User.UserStatus status);
}
