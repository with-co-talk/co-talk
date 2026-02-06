package com.cotalk.adapter.outbound.persistence.user;

import com.cotalk.domain.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 사용자 JPA 리포지토리.
 * Spring Data JPA를 통해 사용자 데이터에 접근한다.
 *
 * @author seunggu.lee
 */
public interface UserJpaRepository extends JpaRepository<User, Long> {

    /**
     * 이메일로 사용자를 조회한다.
     *
     * @param email 이메일
     * @return 사용자 (Optional)
     */
    Optional<User> findByEmail(String email);

    /**
     * OAuth 제공자와 OAuth ID로 사용자를 조회한다.
     *
     * @param oauthProvider OAuth 제공자
     * @param oauthId OAuth ID
     * @return 사용자 (Optional)
     */
    Optional<User> findByOauthProviderAndOauthId(User.OAuthProvider oauthProvider, String oauthId);

    /**
     * 닉네임에 특정 문자열이 포함된 사용자 목록을 조회한다.
     *
     * @param nickname 검색할 닉네임
     * @return 사용자 목록
     */
    List<User> findByNicknameContaining(String nickname);

    /**
     * 닉네임에 특정 문자열이 포함된 사용자 목록을 조회한다. (DB-레벨 페이징)
     *
     * @param nickname 검색할 닉네임
     * @param pageable 페이징 정보 (limit, offset)
     * @return 사용자 목록
     */
    List<User> findByNicknameContaining(String nickname, Pageable pageable);

    /**
     * 해당 이메일을 가진 사용자가 존재하는지 확인한다.
     *
     * @param email 이메일
     * @return 존재 여부
     */
    boolean existsByEmail(String email);

    /**
     * 해당 닉네임을 가진 사용자가 존재하는지 확인한다.
     *
     * @param nickname 닉네임
     * @return 존재 여부
     */
    boolean existsByNickname(String nickname);

    /**
     * 특정 상태의 사용자 목록을 조회한다.
     *
     * @param status 사용자 상태
     * @return 사용자 목록
     */
    List<User> findByStatus(User.UserStatus status);

    /**
     * 특정 상태의 사용자 수를 조회한다.
     *
     * @param status 사용자 상태
     * @return 사용자 수
     */
    long countByStatus(User.UserStatus status);
}
