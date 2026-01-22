package com.cotalk.adapter.inbound.rest;

import com.cotalk.domain.entity.Message;
import com.cotalk.domain.port.inbound.message.SearchMessageUseCase;
import com.cotalk.infrastructure.exception.GlobalExceptionHandler;
import com.cotalk.infrastructure.ratelimit.RateLimitTestConfiguration;
import com.cotalk.infrastructure.security.JwtAuthenticationFilter;
import com.cotalk.infrastructure.security.JwtTokenProvider;
import com.cotalk.infrastructure.security.SecurityContextHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MessageSearchController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({RateLimitTestConfiguration.class, GlobalExceptionHandler.class})
class MessageSearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SearchMessageUseCase searchMessageUseCase;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private SecurityContextHelper securityContextHelper;

    @BeforeEach
    void setUp() {
        // 기본적으로 userId 100L로 인증된 사용자 설정
        given(securityContextHelper.getCurrentUserId()).willReturn(100L);
    }

    @Test
    @DisplayName("채팅방 내 메시지 검색 성공")
    void should_returnMessages_when_searchInChatRoom() throws Exception {
        // given
        Long chatRoomId = 1L;
        Long userId = 100L;
        String keyword = "안녕";

        Message message1 = Message.builder()
                .id(1L)
                .chatRoomId(chatRoomId)
                .senderId(100L)
                .content("안녕하세요!")
                .build();

        Message message2 = Message.builder()
                .id(2L)
                .chatRoomId(chatRoomId)
                .senderId(200L)
                .content("안녕, 반가워요")
                .build();

        given(searchMessageUseCase.searchInChatRoom(eq(chatRoomId), eq(userId), eq(keyword), anyInt(), anyInt()))
                .willReturn(List.of(message1, message2));

        // when & then
        mockMvc.perform(get("/api/v1/messages/search")
                        .param("chatRoomId", chatRoomId.toString())
                        .param("userId", userId.toString())
                        .param("keyword", keyword))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages").isArray())
                .andExpect(jsonPath("$.messages.length()").value(2))
                .andExpect(jsonPath("$.messages[0].content").value("안녕하세요!"));
    }

    @Test
    @DisplayName("전체 채팅방에서 메시지 검색 성공")
    void should_returnMessages_when_searchAcrossAllChatRooms() throws Exception {
        // given
        Long userId = 100L;
        String keyword = "회의";

        Message message1 = Message.builder()
                .id(1L)
                .chatRoomId(1L)
                .senderId(100L)
                .content("회의 시간 알려주세요")
                .build();

        given(searchMessageUseCase.searchAcrossAllChatRooms(eq(userId), eq(keyword), anyInt(), anyInt()))
                .willReturn(List.of(message1));

        // when & then
        mockMvc.perform(get("/api/v1/messages/search/all")
                        .param("userId", userId.toString())
                        .param("keyword", keyword))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages").isArray())
                .andExpect(jsonPath("$.messages.length()").value(1))
                .andExpect(jsonPath("$.messages[0].content").value("회의 시간 알려주세요"));
    }

    @Test
    @DisplayName("검색어가 없으면 400 에러")
    void should_returnBadRequest_when_keywordMissing() throws Exception {
        // when & then
        mockMvc.perform(get("/api/v1/messages/search")
                        .param("chatRoomId", "1")
                        .param("userId", "100"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("페이지네이션 파라미터 적용")
    void should_applyPagination_when_provided() throws Exception {
        // given
        Long chatRoomId = 1L;
        Long userId = 100L;
        String keyword = "테스트";

        given(searchMessageUseCase.searchInChatRoom(eq(chatRoomId), eq(userId), eq(keyword), eq(1), eq(10)))
                .willReturn(List.of());

        // when & then
        mockMvc.perform(get("/api/v1/messages/search")
                        .param("chatRoomId", chatRoomId.toString())
                        .param("userId", userId.toString())
                        .param("keyword", keyword)
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages").isArray());
    }
}
