package com.cotalk.adapter.outbound.persistence;

import com.cotalk.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserJpaRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    List<User> findByNicknameContaining(String nickname);
    boolean existsByEmail(String email);
    boolean existsByNickname(String nickname);
}
