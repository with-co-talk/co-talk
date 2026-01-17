package com.cotalk.application.service.chatroom;

import com.cotalk.domain.entity.ChatRoom;
import com.cotalk.domain.entity.ChatRoomMember;
import com.cotalk.domain.port.outbound.ChatRoomMemberRepository;
import com.cotalk.domain.port.outbound.ChatRoomRepository;
import com.cotalk.infrastructure.id.SnowflakeIdGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CreateChatRoomServiceTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @Mock
    private SnowflakeIdGenerator idGenerator;

    @InjectMocks
    private CreateChatRoomService createChatRoomService;

    @Test
    @DisplayName("1:1 채팅방 생성 성공")
    void should_createChatRoom_when_validUsers() {
        // given
        Long userId1 = 1L;
        Long userId2 = 2L;
        Long chatRoomId = 100L;
        Long memberId1 = 101L;
        Long memberId2 = 102L;

        given(chatRoomRepository.findDirectChatRoomByUserIds(userId1, userId2))
                .willReturn(Optional.empty());
        given(idGenerator.nextId())
                .willReturn(chatRoomId)
                .willReturn(memberId1)
                .willReturn(memberId2);
        given(chatRoomRepository.save(any(ChatRoom.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(chatRoomMemberRepository.save(any(ChatRoomMember.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        Long result = createChatRoomService.createChatRoom(userId1, userId2);

        // then
        assertThat(result).isEqualTo(chatRoomId);

        ArgumentCaptor<ChatRoom> chatRoomCaptor = ArgumentCaptor.forClass(ChatRoom.class);
        verify(chatRoomRepository).save(chatRoomCaptor.capture());
        assertThat(chatRoomCaptor.getValue().getType()).isEqualTo(ChatRoom.ChatRoomType.DIRECT);

        verify(chatRoomMemberRepository, times(2)).save(any(ChatRoomMember.class));
    }

    @Test
    @DisplayName("이미 존재하는 1:1 채팅방이면 기존 채팅방 ID 반환")
    void should_returnExistingRoomId_when_chatRoomAlreadyExists() {
        // given
        Long userId1 = 1L;
        Long userId2 = 2L;
        Long existingRoomId = 50L;

        ChatRoom existingRoom = ChatRoom.builder()
                .id(existingRoomId)
                .type(ChatRoom.ChatRoomType.DIRECT)
                .build();

        given(chatRoomRepository.findDirectChatRoomByUserIds(userId1, userId2))
                .willReturn(Optional.of(existingRoom));

        // when
        Long result = createChatRoomService.createChatRoom(userId1, userId2);

        // then
        assertThat(result).isEqualTo(existingRoomId);
        verify(chatRoomRepository, times(0)).save(any(ChatRoom.class));
    }
}
