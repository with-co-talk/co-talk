package com.cotalk.domain.port.outbound;

import com.cotalk.domain.entity.User;

import java.util.List;
import java.util.Optional;


public interface UserRepository {
    User save(User user);
    Optional<User> findById(Long id);
    Optional<User> findByEmail(String email);
    List<User> findByNicknameContaining(String nickname);
    boolean existsByEmail(String email);
    boolean existsByNickname(String nickname);
}
