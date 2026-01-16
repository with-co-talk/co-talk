package com.cotalk.application.service.user;

import com.cotalk.domain.entity.User;
import com.cotalk.domain.port.inbound.user.SearchUsersUseCase;
import com.cotalk.domain.port.outbound.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 사용자 검색 Use Case 구현체.
 *
 * @author Co-Talk Team
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchUsersService implements SearchUsersUseCase {

    private final UserRepository userRepository;

    @Override
    public List<User> searchUsers(String keyword, UUID currentUserId) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return List.of();
        }
        return userRepository.searchByKeyword(keyword.trim(), currentUserId);
    }
}
