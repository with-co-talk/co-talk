package com.cotalk.domain.port.inbound;

import com.cotalk.domain.entity.User;

import java.util.List;

public interface SearchUserUseCase {
    List<User> searchByNickname(String nickname);
}
