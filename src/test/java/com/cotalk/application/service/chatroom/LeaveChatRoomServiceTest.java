package com.cotalk.application.service.chatroom;

import com.cotalk.domain.entity.ChatRoom;
import com.cotalk.domain.entity.ChatRoomMember;
import com.cotalk.domain.exception.ChatRoomAccessDeniedException;
import com.cotalk.domain.port.outbound.ChatRoomMemberRepository;
import com.cotalk.domain.port.outbound.ChatRoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LeaveChatRoomServiceTest {

    @Mock
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    private LeaveChatRoomService service;

    @BeforeEach
    void setUp() {
        service = new LeaveChatRoomService(chatRoomMemberRepository, chatRoomRepository);
    }

    @Test
    @DisplayName("채팅방 나가기 성공")
    void should_leaveChatRoom_when_validMember() {
        // given
        Long chatRoomId = 100L;
        Long userId = 1L;

        ChatRoomMember member = ChatRoomMember.builder()
                .id(500L)
                .chatRoomId(chatRoomId)
                .userId(userId)
                .joinedAt(LocalDateTime.now())
                .build();

        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId))
                .willReturn(Optional.of(member));

        // 다른 멤버가 존재하는 경우
        ChatRoomMember otherMember = ChatRoomMember.builder()
                .id(501L)
                .chatRoomId(chatRoomId)
                .userId(2L)
                .joinedAt(LocalDateTime.now())
                .build();

        given(chatRoomMemberRepository.findByChatRoomId(chatRoomId))
                .willReturn(Arrays.asList(member, otherMember));

        // when
        service.leaveChatRoom(chatRoomId, userId);

        // then
        verify(chatRoomMemberRepository).delete(member);
    }

    @Test
    @DisplayName("채팅방 멤버가 아니면 예외 발생")
    void should_throwException_when_notMember() {
        // given
        Long chatRoomId = 100L;
        Long userId = 1L;

        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> service.leaveChatRoom(chatRoomId, userId))
                .isInstanceOf(ChatRoomAccessDeniedException.class);
    }

    @Test
    @DisplayName("마지막 멤버가 나가면 채팅방도 삭제")
    void should_deleteChatRoom_when_lastMemberLeaves() {
        // given
        Long chatRoomId = 100L;
        Long userId = 1L;

        ChatRoomMember member = ChatRoomMember.builder()
                .id(500L)
                .chatRoomId(chatRoomId)
                .userId(userId)
                .joinedAt(LocalDateTime.now())
                .build();

        ChatRoom chatRoom = ChatRoom.builder()
                .id(chatRoomId)
                .type(ChatRoom.ChatRoomType.DIRECT)
                .build();

        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId))
                .willReturn(Optional.of(member));
        // delete 후에는 빈 리스트 반환 (이미 삭제되었으므로)
        given(chatRoomMemberRepository.findByChatRoomId(chatRoomId))
                .willReturn(List.of()); // 삭제 후 빈 리스트
        given(chatRoomRepository.findById(chatRoomId))
                .willReturn(Optional.of(chatRoom));

        // when
        service.leaveChatRoom(chatRoomId, userId);

        // then
        verify(chatRoomMemberRepository).delete(member);
        verify(chatRoomRepository).delete(chatRoom);
    }
}
