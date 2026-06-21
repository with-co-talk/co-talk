package com.cotalk.application.service.message;

import com.cotalk.domain.entity.ChatRoomMember;
import com.cotalk.domain.entity.Message;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.exception.ChatRoomAccessDeniedException;
import com.cotalk.domain.port.inbound.message.GetMessageHistoryUseCase.EnrichedMessageHistoryResult;
import com.cotalk.domain.port.outbound.ChatRoomMemberRepository;
import com.cotalk.domain.port.outbound.FileStorage;
import com.cotalk.domain.port.outbound.MessageRepository;
import com.cotalk.domain.port.outbound.UserRepository;
import com.cotalk.domain.validator.ChatRoomMemberValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GetMessageHistoryServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FileStorage fileStorage;

    private ChatRoomMemberValidator chatRoomMemberValidator;

    private GetMessageHistoryService getMessageHistoryService;

    private static final int PRESIGN_EXPIRY_MINUTES = 10;

    @BeforeEach
    void setUp() {
        chatRoomMemberValidator = new ChatRoomMemberValidator(chatRoomMemberRepository);
        getMessageHistoryService = new GetMessageHistoryService(
                messageRepository, chatRoomMemberRepository, userRepository, chatRoomMemberValidator,
                fileStorage, PRESIGN_EXPIRY_MINUTES);
    }

    @Test
    @DisplayName("커서 기반 메시지 조회 - 최신 메시지부터 (beforeMessageId가 null)")
    void should_returnLatestMessages_when_beforeMessageIdIsNull() {
        // given
        Long chatRoomId = 100L;
        Long userId = 1L;
        int size = 20;

        ChatRoomMember member = ChatRoomMember.builder()
                .id(1L)
                .chatRoomId(chatRoomId)
                .userId(userId)
                .build();

        List<Message> messages = List.of(
                Message.builder()
                        .id(1000L)
                        .chatRoomId(chatRoomId)
                        .senderId(userId)
                        .content("최신 메시지")
                        .build(),
                Message.builder()
                        .id(999L)
                        .chatRoomId(chatRoomId)
                        .senderId(2L)
                        .content("이전 메시지")
                        .build()
        );

        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId))
                .willReturn(Optional.of(member));
        given(messageRepository.findByChatRoomIdBeforeMessageId(chatRoomId, null, size))
                .willReturn(messages);

        // when
        List<Message> result = getMessageHistoryService.getMessageHistory(chatRoomId, userId, null, size);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(1000L);
        assertThat(result.get(0).getContent()).isEqualTo("최신 메시지");
    }

    @Test
    @DisplayName("커서 기반 메시지 조회 - 특정 메시지 이전부터 (위로 스크롤)")
    void should_returnOlderMessages_when_beforeMessageIdProvided() {
        // given
        Long chatRoomId = 100L;
        Long userId = 1L;
        Long beforeMessageId = 1000L;
        int size = 20;

        ChatRoomMember member = ChatRoomMember.builder()
                .id(1L)
                .chatRoomId(chatRoomId)
                .userId(userId)
                .build();

        List<Message> messages = List.of(
                Message.builder()
                        .id(999L)
                        .chatRoomId(chatRoomId)
                        .senderId(2L)
                        .content("이전 메시지 1")
                        .build(),
                Message.builder()
                        .id(998L)
                        .chatRoomId(chatRoomId)
                        .senderId(userId)
                        .content("이전 메시지 2")
                        .build()
        );

        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId))
                .willReturn(Optional.of(member));
        given(messageRepository.findByChatRoomIdBeforeMessageId(chatRoomId, beforeMessageId, size))
                .willReturn(messages);

        // when
        List<Message> result = getMessageHistoryService.getMessageHistory(chatRoomId, userId, beforeMessageId, size);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(999L);
        assertThat(result.get(1).getId()).isEqualTo(998L);
    }

    @Test
    @DisplayName("채팅방 멤버가 아닌 경우 예외 발생")
    void should_throwException_when_userIsNotMember() {
        // given
        Long chatRoomId = 100L;
        Long userId = 1L;
        int size = 20;

        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> getMessageHistoryService.getMessageHistory(chatRoomId, userId, null, size))
                .isInstanceOf(ChatRoomAccessDeniedException.class);
    }

    @Test
    @DisplayName("메시지가 없는 경우 빈 리스트 반환")
    void should_returnEmptyList_when_noMessages() {
        // given
        Long chatRoomId = 100L;
        Long userId = 1L;
        int size = 20;

        ChatRoomMember member = ChatRoomMember.builder()
                .id(1L)
                .chatRoomId(chatRoomId)
                .userId(userId)
                .build();

        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId))
                .willReturn(Optional.of(member));
        given(messageRepository.findByChatRoomIdBeforeMessageId(chatRoomId, null, size))
                .willReturn(List.of());

        // when
        List<Message> result = getMessageHistoryService.getMessageHistory(chatRoomId, userId, null, size);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("size가 0인 경우 빈 리스트 반환")
    void should_returnEmptyList_when_sizeIsZero() {
        // given
        Long chatRoomId = 100L;
        Long userId = 1L;
        int size = 0;

        ChatRoomMember member = ChatRoomMember.builder()
                .id(1L)
                .chatRoomId(chatRoomId)
                .userId(userId)
                .build();

        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId))
                .willReturn(Optional.of(member));
        given(messageRepository.findByChatRoomIdBeforeMessageId(chatRoomId, null, size))
                .willReturn(List.of());

        // when
        List<Message> result = getMessageHistoryService.getMessageHistory(chatRoomId, userId, null, size);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("멤버가 히스토리 조회 시 첨부파일은 단기 Pre-signed URL로 재발급된다")
    void should_returnPresignedAttachmentUrl_when_memberFetchesEnrichedHistory() {
        // given
        Long chatRoomId = 100L;
        Long userId = 1L;
        int size = 20;

        ChatRoomMember member = ChatRoomMember.builder()
                .id(1L).chatRoomId(chatRoomId).userId(userId).build();

        Message fileMessage = Message.builder()
                .id(1000L)
                .chatRoomId(chatRoomId)
                .senderId(userId)
                .type(Message.MessageType.IMAGE)
                .content("photo.png")
                .fileUrl("http://minio.example.com/cotalk/uploads/1/abc.png")
                .thumbnailUrl("http://minio.example.com/cotalk/uploads/1/abc-thumb.png")
                .build();

        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId))
                .willReturn(Optional.of(member));
        given(messageRepository.findByChatRoomIdBeforeMessageId(chatRoomId, null, size))
                .willReturn(List.of(fileMessage));
        given(chatRoomMemberRepository.batchCountUnreadMembersByMessageIds(eq(chatRoomId), org.mockito.ArgumentMatchers.anyList()))
                .willReturn(java.util.Map.of());
        given(userRepository.findAllById(org.mockito.ArgumentMatchers.anySet()))
                .willReturn(List.of(User.builder().id(userId).nickname("나").build()));
        given(fileStorage.presignAttachmentUrl(eq(fileMessage.getFileUrl()), eq(PRESIGN_EXPIRY_MINUTES)))
                .willReturn("http://minio.example.com/cotalk/uploads/1/abc.png?X-Amz-Signature=signed");
        given(fileStorage.presignAttachmentUrl(eq(fileMessage.getThumbnailUrl()), eq(PRESIGN_EXPIRY_MINUTES)))
                .willReturn("http://minio.example.com/cotalk/uploads/1/abc-thumb.png?X-Amz-Signature=signed");

        // when
        EnrichedMessageHistoryResult result =
                getMessageHistoryService.getEnrichedMessageHistory(chatRoomId, userId, null, size);

        // then: 영구 공개 URL이 아니라 서명된 단기 URL이 노출된다
        assertThat(result.messages()).hasSize(1);
        assertThat(result.messages().get(0).fileUrl()).contains("X-Amz-Signature");
        assertThat(result.messages().get(0).thumbnailUrl()).contains("X-Amz-Signature");
        verify(fileStorage).presignAttachmentUrl(fileMessage.getFileUrl(), PRESIGN_EXPIRY_MINUTES);
    }

    @Test
    @DisplayName("멤버가 아니면 히스토리(첨부파일) 조회가 거부되고 Pre-signed URL을 발급하지 않는다")
    void should_rejectAndNotPresign_when_nonMemberFetchesEnrichedHistory() {
        // given
        Long chatRoomId = 100L;
        Long userId = 999L;
        int size = 20;

        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> getMessageHistoryService.getEnrichedMessageHistory(chatRoomId, userId, null, size))
                .isInstanceOf(ChatRoomAccessDeniedException.class);
        verify(fileStorage, never()).presignAttachmentUrl(org.mockito.ArgumentMatchers.anyString(), anyInt());
    }
}
