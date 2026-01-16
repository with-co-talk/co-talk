package com.cotalk.application.service;

import com.cotalk.domain.entity.User;
import com.cotalk.domain.port.inbound.SearchUserUseCase;
import com.cotalk.domain.port.outbound.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchUserService implements SearchUserUseCase {

    private final UserRepository userRepository;

    @Override
    public List<User> searchByNickname(String nickname) {
        return userRepository.findByNicknameContaining(nickname);
    }
}
