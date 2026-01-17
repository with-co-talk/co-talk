package com.cotalk.adapter.inbound.rest;

import com.cotalk.adapter.inbound.rest.dto.auth.LoginRequest;
import com.cotalk.adapter.inbound.rest.dto.auth.LoginResponse;
import com.cotalk.adapter.inbound.rest.dto.auth.SignUpRequest;
import com.cotalk.adapter.inbound.rest.dto.auth.SignUpResponse;
import com.cotalk.domain.port.inbound.auth.LoginUseCase;
import com.cotalk.domain.port.inbound.auth.SignUpUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증 관련 REST 컨트롤러.
 * 회원가입, 로그인 등의 인증 기능을 제공한다.
 *
 * @author seunggu.lee
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "인증", description = "회원가입, 로그인 API")
public class AuthController {

    private final SignUpUseCase signUpUseCase;
    private final LoginUseCase loginUseCase;

    /**
     * 새로운 사용자를 등록한다.
     *
     * @param request 회원가입 요청 정보 (이메일, 비밀번호, 닉네임)
     * @return 생성된 사용자 ID와 성공 메시지
     */
    @Operation(summary = "회원가입", description = "새로운 사용자를 등록합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "회원가입 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    @PostMapping("/signup")
    public ResponseEntity<SignUpResponse> signUp(@Valid @RequestBody SignUpRequest request) {
        Long userId = signUpUseCase.signUp(request.email(), request.password(), request.nickname());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SignUpResponse.of(userId, "회원가입이 완료되었습니다."));
    }

    /**
     * 이메일과 비밀번호로 로그인하여 JWT 토큰을 발급받는다.
     *
     * @param request 로그인 요청 정보 (이메일, 비밀번호)
     * @return JWT 토큰 정보
     */
    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인하여 JWT 토큰을 발급받습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        String token = loginUseCase.login(request.email(), request.password());
        return ResponseEntity.ok(LoginResponse.of(token));
    }
}
