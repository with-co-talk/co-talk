package com.cotalk.domain.port.inbound;



public interface SignUpUseCase {
    Long signUp(String email, String password, String nickname);
}
