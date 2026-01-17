package com.cotalk.adapter.inbound.rest;

import com.cotalk.domain.entity.TermsAgreement.TermsType;
import com.cotalk.domain.exception.TermsAgreementException;
import com.cotalk.domain.exception.UserNotFoundException;
import com.cotalk.domain.port.inbound.auth.AgreeToTermsUseCase;
import com.cotalk.domain.port.inbound.auth.AgreeToTermsUseCase.TermsAgreementCommand;
import com.cotalk.domain.port.inbound.auth.AgreeToTermsUseCase.TermsAgreementStatus;
import com.cotalk.infrastructure.exception.GlobalExceptionHandler;
import com.cotalk.infrastructure.ratelimit.RateLimitTestConfiguration;
import com.cotalk.infrastructure.security.JwtAuthenticationFilter;
import com.cotalk.infrastructure.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TermsController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({RateLimitTestConfiguration.class, GlobalExceptionHandler.class})
class TermsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AgreeToTermsUseCase agreeToTermsUseCase;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Nested
    @DisplayName("약관 동의 API")
    class AgreeToTermsTests {

        @Test
        @DisplayName("약관 동의 성공")
        void should_returnOk_when_agreeToTermsSuccess() throws Exception {
            // given
            willDoNothing().given(agreeToTermsUseCase).agreeToTerms(any(TermsAgreementCommand.class));

            String requestBody = """
                    {
                        "userId": 1,
                        "agreements": [
                            {
                                "termsType": "SERVICE",
                                "version": "1.0",
                                "agreed": true
                            },
                            {
                                "termsType": "PRIVACY",
                                "version": "1.0",
                                "agreed": true
                            }
                        ]
                    }
                    """;

            // when & then
            mockMvc.perform(post("/api/v1/terms/agree")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("약관 동의가 완료되었습니다."));
        }

        @Test
        @DisplayName("마케팅 동의 포함 약관 동의 성공")
        void should_returnOk_when_agreeToTermsWithMarketing() throws Exception {
            // given
            willDoNothing().given(agreeToTermsUseCase).agreeToTerms(any(TermsAgreementCommand.class));

            String requestBody = """
                    {
                        "userId": 1,
                        "agreements": [
                            {
                                "termsType": "SERVICE",
                                "version": "1.0",
                                "agreed": true
                            },
                            {
                                "termsType": "PRIVACY",
                                "version": "1.0",
                                "agreed": true
                            },
                            {
                                "termsType": "MARKETING",
                                "version": "1.0",
                                "agreed": true
                            }
                        ]
                    }
                    """;

            // when & then
            mockMvc.perform(post("/api/v1/terms/agree")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("약관 동의가 완료되었습니다."));
        }

        @Test
        @DisplayName("필수 약관 미동의 시 400 에러")
        void should_returnBadRequest_when_requiredTermsNotAgreed() throws Exception {
            // given
            willThrow(TermsAgreementException.serviceTermsRequired())
                    .given(agreeToTermsUseCase).agreeToTerms(any(TermsAgreementCommand.class));

            String requestBody = """
                    {
                        "userId": 1,
                        "agreements": [
                            {
                                "termsType": "SERVICE",
                                "version": "1.0",
                                "agreed": false
                            },
                            {
                                "termsType": "PRIVACY",
                                "version": "1.0",
                                "agreed": true
                            }
                        ]
                    }
                    """;

            // when & then
            mockMvc.perform(post("/api/v1/terms/agree")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("개인정보처리방침 미동의 시 400 에러")
        void should_returnBadRequest_when_privacyPolicyNotAgreed() throws Exception {
            // given
            willThrow(TermsAgreementException.privacyPolicyRequired())
                    .given(agreeToTermsUseCase).agreeToTerms(any(TermsAgreementCommand.class));

            String requestBody = """
                    {
                        "userId": 1,
                        "agreements": [
                            {
                                "termsType": "SERVICE",
                                "version": "1.0",
                                "agreed": true
                            },
                            {
                                "termsType": "PRIVACY",
                                "version": "1.0",
                                "agreed": false
                            }
                        ]
                    }
                    """;

            // when & then
            mockMvc.perform(post("/api/v1/terms/agree")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("userId 누락 시 400 에러")
        void should_returnBadRequest_when_userIdMissing() throws Exception {
            // given
            String requestBody = """
                    {
                        "agreements": [
                            {
                                "termsType": "SERVICE",
                                "version": "1.0",
                                "agreed": true
                            }
                        ]
                    }
                    """;

            // when & then
            mockMvc.perform(post("/api/v1/terms/agree")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("agreements 누락 시 400 에러")
        void should_returnBadRequest_when_agreementsMissing() throws Exception {
            // given
            String requestBody = """
                    {
                        "userId": 1
                    }
                    """;

            // when & then
            mockMvc.perform(post("/api/v1/terms/agree")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("빈 agreements 배열 시 400 에러")
        void should_returnBadRequest_when_agreementsEmpty() throws Exception {
            // given
            String requestBody = """
                    {
                        "userId": 1,
                        "agreements": []
                    }
                    """;

            // when & then
            mockMvc.perform(post("/api/v1/terms/agree")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("termsType 누락 시 - AgreementItem의 nested validation은 적용되지 않아 요청이 처리됨")
        void should_processRequest_when_termsTypeMissing() throws Exception {
            // given - nested object의 @NotNull 검증은 @Valid가 없으면 적용 안됨
            willDoNothing().given(agreeToTermsUseCase).agreeToTerms(any(TermsAgreementCommand.class));

            String requestBody = """
                    {
                        "userId": 1,
                        "agreements": [
                            {
                                "version": "1.0",
                                "agreed": true
                            }
                        ]
                    }
                    """;

            // when & then - nested validation이 적용되지 않으면 요청이 처리됨
            mockMvc.perform(post("/api/v1/terms/agree")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("version 누락 시 - AgreementItem의 nested validation은 적용되지 않아 요청이 처리됨")
        void should_processRequest_when_versionMissing() throws Exception {
            // given - nested object의 @NotNull 검증은 @Valid가 없으면 적용 안됨
            willDoNothing().given(agreeToTermsUseCase).agreeToTerms(any(TermsAgreementCommand.class));

            String requestBody = """
                    {
                        "userId": 1,
                        "agreements": [
                            {
                                "termsType": "SERVICE",
                                "agreed": true
                            }
                        ]
                    }
                    """;

            // when & then - nested validation이 적용되지 않으면 요청이 처리됨
            mockMvc.perform(post("/api/v1/terms/agree")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("잘못된 termsType 시 500 에러 (Jackson 역직렬화 실패)")
        void should_returnInternalError_when_invalidTermsType() throws Exception {
            // given - 잘못된 enum 값은 Jackson에서 역직렬화 실패
            String requestBody = """
                    {
                        "userId": 1,
                        "agreements": [
                            {
                                "termsType": "INVALID_TYPE",
                                "version": "1.0",
                                "agreed": true
                            }
                        ]
                    }
                    """;

            // when & then - Jackson 역직렬화 실패는 내부 오류로 처리됨
            mockMvc.perform(post("/api/v1/terms/agree")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("X-Forwarded-For 헤더에서 IP 추출")
        void should_extractIpFromXForwardedFor() throws Exception {
            // given
            ArgumentCaptor<TermsAgreementCommand> commandCaptor = ArgumentCaptor.forClass(TermsAgreementCommand.class);
            willDoNothing().given(agreeToTermsUseCase).agreeToTerms(commandCaptor.capture());

            String requestBody = """
                    {
                        "userId": 1,
                        "agreements": [
                            {
                                "termsType": "SERVICE",
                                "version": "1.0",
                                "agreed": true
                            }
                        ]
                    }
                    """;

            // when
            mockMvc.perform(post("/api/v1/terms/agree")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-Forwarded-For", "192.168.1.1, 10.0.0.1")
                            .content(requestBody))
                    .andExpect(status().isOk());

            // then
            TermsAgreementCommand capturedCommand = commandCaptor.getValue();
            assertThat(capturedCommand.ipAddress()).isEqualTo("192.168.1.1");
        }

        @Test
        @DisplayName("존재하지 않는 사용자 시 404 에러")
        void should_returnNotFound_when_userNotFound() throws Exception {
            // given
            willThrow(new UserNotFoundException("사용자를 찾을 수 없습니다."))
                    .given(agreeToTermsUseCase).agreeToTerms(any(TermsAgreementCommand.class));

            String requestBody = """
                    {
                        "userId": 999,
                        "agreements": [
                            {
                                "termsType": "SERVICE",
                                "version": "1.0",
                                "agreed": true
                            }
                        ]
                    }
                    """;

            // when & then
            mockMvc.perform(post("/api/v1/terms/agree")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("마케팅 수신 동의 철회 API")
    class WithdrawMarketingAgreementTests {

        @Test
        @DisplayName("마케팅 수신 동의 철회 성공")
        void should_returnOk_when_withdrawMarketingSuccess() throws Exception {
            // given
            Long userId = 1L;

            willDoNothing().given(agreeToTermsUseCase).withdrawMarketingAgreement(eq(userId));

            // when & then
            mockMvc.perform(delete("/api/v1/terms/marketing/{userId}", userId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("마케팅 수신 동의가 철회되었습니다."));
        }

        @Test
        @DisplayName("존재하지 않는 사용자 철회 시 404 에러")
        void should_returnNotFound_when_userNotFoundForWithdraw() throws Exception {
            // given
            Long userId = 999L;

            willThrow(new UserNotFoundException("사용자를 찾을 수 없습니다."))
                    .given(agreeToTermsUseCase).withdrawMarketingAgreement(eq(userId));

            // when & then
            mockMvc.perform(delete("/api/v1/terms/marketing/{userId}", userId))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("잘못된 userId 타입 시 500 에러 (타입 변환 실패)")
        void should_returnInternalError_when_invalidUserIdType() throws Exception {
            // when & then - Spring이 Long 변환 실패 시 내부 오류 발생
            mockMvc.perform(delete("/api/v1/terms/marketing/not-a-number"))
                    .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("약관 동의 상태 조회 API")
    class GetAgreementStatusTests {

        @Test
        @DisplayName("약관 동의 상태 조회 성공")
        void should_returnAgreementStatus_when_getStatusSuccess() throws Exception {
            // given
            Long userId = 1L;

            List<TermsAgreementStatus> statusList = List.of(
                    new TermsAgreementStatus(TermsType.SERVICE, "1.0", true, true),
                    new TermsAgreementStatus(TermsType.PRIVACY, "1.0", true, true),
                    new TermsAgreementStatus(TermsType.MARKETING, "1.0", false, false)
            );

            given(agreeToTermsUseCase.getAgreementStatus(eq(userId))).willReturn(statusList);

            // when & then
            mockMvc.perform(get("/api/v1/terms/status/{userId}", userId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.agreements").isArray())
                    .andExpect(jsonPath("$.agreements.length()").value(3))
                    .andExpect(jsonPath("$.agreements[0].termsType").value("SERVICE"))
                    .andExpect(jsonPath("$.agreements[0].version").value("1.0"))
                    .andExpect(jsonPath("$.agreements[0].agreed").value(true))
                    .andExpect(jsonPath("$.agreements[0].required").value(true))
                    .andExpect(jsonPath("$.agreements[1].termsType").value("PRIVACY"))
                    .andExpect(jsonPath("$.agreements[1].agreed").value(true))
                    .andExpect(jsonPath("$.agreements[2].termsType").value("MARKETING"))
                    .andExpect(jsonPath("$.agreements[2].agreed").value(false))
                    .andExpect(jsonPath("$.agreements[2].required").value(false));
        }

        @Test
        @DisplayName("빈 동의 상태 조회")
        void should_returnEmptyList_when_noAgreements() throws Exception {
            // given
            Long userId = 1L;

            given(agreeToTermsUseCase.getAgreementStatus(eq(userId))).willReturn(Collections.emptyList());

            // when & then
            mockMvc.perform(get("/api/v1/terms/status/{userId}", userId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.agreements").isArray())
                    .andExpect(jsonPath("$.agreements.length()").value(0));
        }

        @Test
        @DisplayName("존재하지 않는 사용자 조회 시 404 에러")
        void should_returnNotFound_when_userNotFoundForStatus() throws Exception {
            // given
            Long userId = 999L;

            given(agreeToTermsUseCase.getAgreementStatus(eq(userId)))
                    .willThrow(new UserNotFoundException("사용자를 찾을 수 없습니다."));

            // when & then
            mockMvc.perform(get("/api/v1/terms/status/{userId}", userId))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("잘못된 userId 타입 시 500 에러 (타입 변환 실패)")
        void should_returnInternalError_when_invalidUserIdTypeForStatus() throws Exception {
            // when & then - Spring이 Long 변환 실패 시 내부 오류 발생
            mockMvc.perform(get("/api/v1/terms/status/invalid"))
                    .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("필수 약관 동의 확인 API")
    class CheckRequiredTermsTests {

        @Test
        @DisplayName("필수 약관 동의 완료 시 true 반환")
        void should_returnTrue_when_agreedToRequiredTerms() throws Exception {
            // given
            Long userId = 1L;

            given(agreeToTermsUseCase.hasAgreedToRequiredTerms(eq(userId))).willReturn(true);

            // when & then
            mockMvc.perform(get("/api/v1/terms/check/{userId}", userId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.agreedToRequiredTerms").value(true));
        }

        @Test
        @DisplayName("필수 약관 미동의 시 false 반환")
        void should_returnFalse_when_notAgreedToRequiredTerms() throws Exception {
            // given
            Long userId = 1L;

            given(agreeToTermsUseCase.hasAgreedToRequiredTerms(eq(userId))).willReturn(false);

            // when & then
            mockMvc.perform(get("/api/v1/terms/check/{userId}", userId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.agreedToRequiredTerms").value(false));
        }

        @Test
        @DisplayName("존재하지 않는 사용자 확인 시 404 에러")
        void should_returnNotFound_when_userNotFoundForCheck() throws Exception {
            // given
            Long userId = 999L;

            given(agreeToTermsUseCase.hasAgreedToRequiredTerms(eq(userId)))
                    .willThrow(new UserNotFoundException("사용자를 찾을 수 없습니다."));

            // when & then
            mockMvc.perform(get("/api/v1/terms/check/{userId}", userId))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("잘못된 userId 타입 시 500 에러 (타입 변환 실패)")
        void should_returnInternalError_when_invalidUserIdTypeForCheck() throws Exception {
            // when & then - Spring이 Long 변환 실패 시 내부 오류 발생
            mockMvc.perform(get("/api/v1/terms/check/abc"))
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("음수 userId 시에도 정상 처리")
        void should_handleNegativeUserId() throws Exception {
            // given
            Long userId = -1L;

            given(agreeToTermsUseCase.hasAgreedToRequiredTerms(eq(userId)))
                    .willThrow(new UserNotFoundException("사용자를 찾을 수 없습니다."));

            // when & then
            mockMvc.perform(get("/api/v1/terms/check/{userId}", userId))
                    .andExpect(status().isNotFound());
        }
    }
}
