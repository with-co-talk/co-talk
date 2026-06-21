package com.cotalk.adapter.inbound.rest;

import com.cotalk.adapter.inbound.rest.dto.auth.OAuthLoginRequest;
import com.cotalk.adapter.inbound.rest.dto.auth.OAuthLoginResponse;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.port.inbound.auth.OAuthLoginUseCase;
import com.cotalk.domain.port.inbound.auth.OAuthLoginUseCase.OAuthLoginResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * OAuth 소셜 로그인을 위한 REST 컨트롤러.
 * 카카오, 구글, 애플 등의 OAuth 제공자를 통한 로그인 기능을 제공한다.
 *
 * @author seunggu.lee
 */
@RestController
@RequestMapping("/api/v1/auth/oauth")
@RequiredArgsConstructor
@Tag(name = "OAuth 인증", description = "소셜 로그인 API")
public class OAuthController {

    private final OAuthLoginUseCase oAuthLoginService;

    /**
     * OAuth 제공자를 통해 로그인한다.
     * 클라이언트가 보낸 제공자 토큰을 서버가 검증하여 식별 정보를 도출하며,
     * 신규 사용자는 자동으로 회원가입 처리된다.
     *
     * @param request OAuth 로그인 요청 정보 (제공자, 제공자 토큰)
     * @return JWT 토큰 정보 및 신규 사용자 여부
     */
    @Operation(summary = "소셜 로그인", description = "OAuth 제공자(카카오, 구글, 애플) 토큰을 서버에서 검증해 로그인합니다. 신규 사용자는 자동으로 회원가입됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "제공자 토큰 검증 실패")
    })
    @PostMapping("/login")
    public ResponseEntity<OAuthLoginResponse> loginWithOAuth(@Valid @RequestBody OAuthLoginRequest request) {
        User.OAuthProvider provider = User.OAuthProvider.valueOf(request.provider());

        OAuthLoginResult result = oAuthLoginService.loginWithOAuth(provider, request.token());

        return ResponseEntity.ok(OAuthLoginResponse.of(
                result.token(),
                result.isNewUser(),
                result.userId()
        ));
    }
}
