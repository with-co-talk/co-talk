package com.cotalk.application.service;

import com.cotalk.domain.entity.ChatRoomMember;
import com.cotalk.domain.exception.DomainException;
import com.cotalk.domain.port.inbound.LeaveChatRoomUseCase;
import com.cotalk.domain.port.outbound.ChatRoomMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LeaveChatRoomServiceTest {

    @Mock
    private ChatRoomMemberRepository chatRoomMemberRepository;

    private LeaveChatRoomUseCase leaveChatRoomUseCase;

    @BeforeEach
    void setUp() {
        leaveChatRoomUseCase = new LeaveChatRoomService(chatRoomMemberRepository);
    }

    @Test
    @DisplayName("채팅방 나가기 성공")
    void should_leaveChatRoom_when_validRequest() {
        // given
        Long userId = 1L;
        Long chatRoomId = 100L;
        ChatRoomMember member = ChatRoomMember.builder()
                .id(500L)
                .chatRoomId(chatRoomId)
                .userId(userId)
                .joinedAt(LocalDateTime.now())
                .build();

        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId))
                .willReturn(Optional.of(member));

        // when
        leaveChatRoomUseCase.leaveChatRoom(userId, chatRoomId);

        // then
        verify(chatRoomMemberRepository).delete(member);
    }

    @Test
    @DisplayName("채팅방 멤버가 아니면 예외 발생")
    void should_throwException_when_notMember() {
        // given
        Long userId = 1L;
        Long chatRoomId = 100L;
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> leaveChatRoomUseCase.leaveChatRoom(userId, chatRoomId))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("채팅방 멤버가 아닙니다");
    }
}
