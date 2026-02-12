package com.cotalk.adapter.outbound.persistence.user;

import com.cotalk.domain.entity.User;
import com.cotalk.domain.port.outbound.UserRepository;
import com.cotalk.adapter.outbound.persistence.entity.UserJpaEntity;
import com.cotalk.adapter.outbound.persistence.mapper.UserMapper;
import com.cotalk.infrastructure.config.CacheConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * 사용자 영속성 어댑터.
 * JPA 엔티티와 도메인 간 매핑을 수행하며, 도메인 포트를 구현한다.
 *
 * @author seunggu.lee
 */
@Repository
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository userJpaRepository;
    private final UserMapper userMapper;

    @CacheEvict(value = CacheConfig.USER_CACHE, key = "#user.id", condition = "#user.id != null")
    @Override
    public User save(User user) {
        UserJpaEntity jpa = userMapper.toJpa(user);
        UserJpaEntity saved = userJpaRepository.save(jpa);
        return userMapper.toDomain(saved);
    }

    @Cacheable(value = CacheConfig.USER_CACHE, key = "#id")
    @Override
    public Optional<User> findById(Long id) {
        return userJpaRepository.findById(id).map(userMapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userJpaRepository.findByEmail(email).map(userMapper::toDomain);
    }

    @Override
    public Optional<User> findByOAuthProviderAndOAuthId(User.OAuthProvider provider, String oauthId) {
        return userJpaRepository.findByOauthProviderAndOauthId(provider, oauthId).map(userMapper::toDomain);
    }

    @Override
    public List<User> findByNicknameContaining(String nickname) {
        return userJpaRepository.findByNicknameContaining(nickname).stream()
                .map(userMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<User> findByNicknameContaining(String nickname, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return userJpaRepository.findByNicknameContaining(nickname, pageable).stream()
                .map(userMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByEmail(String email) {
        return userJpaRepository.existsByEmail(email);
    }

    @Override
    public boolean existsByNickname(String nickname) {
        return userJpaRepository.existsByNickname(nickname);
    }

    @CacheEvict(value = CacheConfig.USER_CACHE, key = "#user.id")
    @Override
    public void delete(User user) {
        UserJpaEntity jpa = userMapper.toJpa(user);
        userJpaRepository.delete(jpa);
    }

    @Override
    public List<User> findAll() {
        return userJpaRepository.findAll().stream()
                .map(userMapper::toDomain)
                .collect(Collectors.toList());
    }

    /**
     * 모든 사용자를 페이지네이션하여 조회한다.
     *
     * @param pageable 페이지네이션 정보
     * @return 페이지네이션된 사용자 목록
     */
    @Override
    public Page<User> findAll(Pageable pageable) {
        return userJpaRepository.findAll(pageable)
                .map(userMapper::toDomain);
    }

    @Override
    public List<User> findByStatus(User.UserStatus status) {
        return userJpaRepository.findByStatus(status).stream()
                .map(userMapper::toDomain)
                .collect(Collectors.toList());
    }

    /**
     * 특정 상태의 사용자를 페이지네이션하여 조회한다.
     *
     * @param status   사용자 상태
     * @param pageable 페이지네이션 정보
     * @return 페이지네이션된 사용자 목록
     */
    @Override
    public Page<User> findByStatus(User.UserStatus status, Pageable pageable) {
        return userJpaRepository.findByStatus(status, pageable)
                .map(userMapper::toDomain);
    }

    @Override
    public List<User> findAllById(Iterable<Long> ids) {
        return StreamSupport.stream(userJpaRepository.findAllById(ids).spliterator(), false)
                .map(userMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public long count() {
        return userJpaRepository.count();
    }

    @Override
    public long countByStatus(User.UserStatus status) {
        return userJpaRepository.countByStatus(status);
    }

    @Override
    public Optional<User> findByNicknameAndPhoneNumber(String nickname, String phoneNumber) {
        return userJpaRepository.findByNicknameAndPhoneNumber(nickname, phoneNumber)
                .map(userMapper::toDomain);
    }
}
