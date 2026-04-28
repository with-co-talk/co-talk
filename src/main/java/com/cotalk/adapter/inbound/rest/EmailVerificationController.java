package com.cotalk.adapter.inbound.rest;

import com.cotalk.adapter.inbound.rest.dto.auth.ResendVerificationRequest;
import com.cotalk.adapter.inbound.rest.dto.common.MessageResponse;
import com.cotalk.domain.exception.InvalidEmailVerificationTokenException;
import com.cotalk.domain.port.inbound.auth.ResendVerificationUseCase;
import com.cotalk.domain.port.inbound.auth.VerifyEmailUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

/**
 * 이메일 인증 REST 컨트롤러.
 * 이메일 인증 및 인증 이메일 재발송 기능을 제공한다.
 *
 * @author seunggu.lee
 */
@Tag(name = "Email Verification", description = "이메일 인증 API")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class EmailVerificationController {

    private final VerifyEmailUseCase verifyEmailUseCase;
    private final ResendVerificationUseCase resendVerificationUseCase;

    /**
     * 토큰을 사용하여 이메일 인증을 완료한다.
     *
     * @param token 이메일 인증 토큰
     * @return 브라우저에서 볼 수 있는 인증 결과 HTML
     */
    @Operation(summary = "이메일 인증", description = "토큰을 사용하여 이메일 인증을 완료합니다.")
    @GetMapping(value = "/verify-email", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> verifyEmail(@RequestParam String token) {
        try {
            verifyEmailUseCase.verifyEmail(token);
            return html(HttpStatus.OK, "이메일 인증 완료", "이메일 인증이 완료되었습니다.", "이제 Co-Talk에 로그인할 수 있습니다.", "success");
        } catch (InvalidEmailVerificationTokenException e) {
            HttpStatus status = e.getMessage().contains("이미 인증") ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
            return html(status, "이메일 인증 안내", e.getMessage(), "앱으로 돌아가 다시 로그인하거나 인증 메일을 재요청해주세요.", "notice");
        }
    }

    /**
     * 이메일 인증 이메일을 재발송한다.
     * 보안상 이메일 존재 여부와 관계없이 동일한 응답을 반환한다.
     *
     * @param request 재발송 요청 정보 (이메일)
     * @return 발송 완료 메시지
     */
    @Operation(summary = "인증 이메일 재발송", description = "이메일 인증 이메일을 재발송합니다.")
    @PostMapping("/resend-verification")
    public ResponseEntity<MessageResponse> resendVerification(
            @Valid @RequestBody ResendVerificationRequest request) {
        resendVerificationUseCase.resendVerification(request.email());
        return ResponseEntity.ok(MessageResponse.of("인증 이메일이 발송되었습니다. 이메일을 확인해주세요."));
    }

    private ResponseEntity<String> html(HttpStatus status, String title, String heading, String message, String tone) {
        return ResponseEntity.status(status)
                .contentType(new MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8))
                .body(buildResultPage(title, heading, message, tone));
    }

    private String buildResultPage(String title, String heading, String message, String tone) {
        String accent = "success".equals(tone) ? "#16a34a" : "#4f46e5";
        return """
                <!DOCTYPE html>
                <html lang="ko">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>%s</title>
                  <style>
                    :root { color-scheme: light; }
                    body {
                      margin: 0;
                      min-height: 100vh;
                      display: grid;
                      place-items: center;
                      background: #f6f7fb;
                      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
                      color: #111827;
                    }
                    main {
                      width: min(92vw, 420px);
                      box-sizing: border-box;
                      padding: 32px 28px;
                      border: 1px solid #e5e7eb;
                      border-radius: 8px;
                      background: #fff;
                      text-align: center;
                      box-shadow: 0 14px 40px rgba(15, 23, 42, 0.08);
                    }
                    .mark {
                      width: 56px;
                      height: 56px;
                      display: grid;
                      place-items: center;
                      margin: 0 auto 18px;
                      border-radius: 50%%;
                      background: %s;
                      color: #fff;
                      font-size: 30px;
                      font-weight: 700;
                    }
                    h1 { margin: 0 0 10px; font-size: 24px; line-height: 1.25; }
                    p { margin: 0; color: #4b5563; font-size: 15px; line-height: 1.65; }
                  </style>
                </head>
                <body>
                  <main>
                    <div class="mark">%s</div>
                    <h1>%s</h1>
                    <p>%s</p>
                  </main>
                </body>
                </html>
                """.formatted(
                escapeHtml(title),
                accent,
                "success".equals(tone) ? "✓" : "!",
                escapeHtml(heading),
                escapeHtml(message)
        );
    }

    private String escapeHtml(String text) {
        return text == null ? "" : text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
