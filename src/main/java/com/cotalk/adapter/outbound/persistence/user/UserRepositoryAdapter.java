package com.cotalk.adapter.outbound.persistence.user;

import com.cotalk.domain.entity.User;
import com.cotalk.domain.port.outbound.UserRepository;
import com.cotalk.infrastructure.config.CacheConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 사용자 영속성 어댑터.
 * JPA를 통해 사용자 데이터를 저장하고 조회한다.
 *
 * @author seunggu.lee
 */
@Repository
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository userJpaRepository;

    /**
     * 사용자를 저장한다.
     * 저장 후 해당 사용자 캐시를 무효화한다.
     *
     * @param user 저장할 사용자 엔티티
     * @return 저장된 사용자 엔티티
     */
    @CacheEvict(value = CacheConfig.USER_CACHE, key = "#user.id", condition = "#user.id != null")
    @Override
    public User save(User user) {
        return userJpaRepository.save(user);
    }

    /**
     * ID로 사용자를 조회한다.
     * 결과는 캐시에 저장되어 반복 조회 시 DB 접근을 줄인다.
     *
     * @param id 사용자 ID
     * @return 사용자 (Optional)
     */
    @Cacheable(value = CacheConfig.USER_CACHE, key = "#id")
    @Override
    public Optional<User> findById(Long id) {
        return userJpaRepository.findById(id);
    }

    /**
     * 이메일로 사용자를 조회한다.
     *
     * @param email 이메일
     * @return 사용자 (Optional)
     */
    @Override
    public Optional<User> findByEmail(String email) {
        return userJpaRepository.findByEmail(email);
    }

    /**
     * OAuth 제공자와 OAuth ID로 사용자를 조회한다.
     *
     * @param provider OAuth 제공자
     * @param oauthId OAuth ID
     * @return 사용자 (Optional)
     */
    @Override
    public Optional<User> findByOAuthProviderAndOAuthId(User.OAuthProvider provider, String oauthId) {
        return userJpaRepository.findByOauthProviderAndOauthId(provider, oauthId);
    }

    /**
     * 닉네임에 특정 문자열이 포함된 사용자 목록을 조회한다.
     *
     * @param nickname 검색할 닉네임
     * @return 사용자 목록
     */
    @Override
    public List<User> findByNicknameContaining(String nickname) {
        return userJpaRepository.findByNicknameContaining(nickname);
    }

    /**
     * 해당 이메일을 가진 사용자가 존재하는지 확인한다.
     *
     * @param email 이메일
     * @return 존재 여부
     */
    @Override
    public boolean existsByEmail(String email) {
        return userJpaRepository.existsByEmail(email);
    }

    /**
     * 해당 닉네임을 가진 사용자가 존재하는지 확인한다.
     *
     * @param nickname 닉네임
     * @return 존재 여부
     */
    @Override
    public boolean existsByNickname(String nickname) {
        return userJpaRepository.existsByNickname(nickname);
    }

    /**
     * 사용자를 삭제한다.
     * 삭제 시 해당 사용자 캐시를 무효화한다.
     *
     * @param user 삭제할 사용자 엔티티
     */
    @CacheEvict(value = CacheConfig.USER_CACHE, key = "#user.id")
    @Override
    public void delete(User user) {
        userJpaRepository.delete(user);
    }

    /**
     * 모든 사용자 목록을 조회한다.
     *
     * @return 사용자 목록
     */
    @Override
    public List<User> findAll() {
        return userJpaRepository.findAll();
    }

    /**
     * 특정 상태의 사용자 목록을 조회한다.
     *
     * @param status 사용자 상태
     * @return 사용자 목록
     */
    @Override
    public List<User> findByStatus(User.UserStatus status) {
        return userJpaRepository.findByStatus(status);
    }

    /**
     * 여러 ID로 사용자 목록을 조회한다.
     *
     * @param ids 조회할 사용자 ID 목록
     * @return 조회된 사용자 목록
     */
    @Override
    public List<User> findAllById(Iterable<Long> ids) {
        return userJpaRepository.findAllById(ids);
    }

    /**
     * 전체 사용자 수를 조회한다.
     *
     * @return 사용자 수
     */
    @Override
    public long count() {
        return userJpaRepository.count();
    }

    /**
     * 특정 상태의 사용자 수를 조회한다.
     *
     * @param status 사용자 상태
     * @return 사용자 수
     */
    @Override
    public long countByStatus(User.UserStatus status) {
        return userJpaRepository.countByStatus(status);
    }
}
