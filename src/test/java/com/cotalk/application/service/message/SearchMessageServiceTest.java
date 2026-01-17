package com.cotalk.application.service.message;

import com.cotalk.domain.entity.Message;
import com.cotalk.domain.port.outbound.ChatRoomMemberRepository;
import com.cotalk.domain.port.outbound.MessageRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class SearchMessageServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @InjectMocks
    private SearchMessageService searchMessageService;

    @Test
    @DisplayName("채팅방 내 메시지 검색 성공")
    void should_returnMessages_when_searchInChatRoom() {
        // given
        Long chatRoomId = 1L;
        Long userId = 100L;
        String keyword = "안녕";

        Message message1 = Message.builder()
                .id(1L)
                .chatRoomId(chatRoomId)
                .senderId(100L)
                .content("안녕하세요!")
                .createdAt(LocalDateTime.now())
                .build();

        Message message2 = Message.builder()
                .id(2L)
                .chatRoomId(chatRoomId)
                .senderId(200L)
                .content("안녕, 반가워요")
                .createdAt(LocalDateTime.now().minusMinutes(5))
                .build();

        given(chatRoomMemberRepository.existsByChatRoomIdAndUserId(chatRoomId, userId)).willReturn(true);
        given(messageRepository.searchByKeywordInChatRoom(chatRoomId, keyword, 0, 20))
                .willReturn(List.of(message1, message2));

        // when
        List<Message> result = searchMessageService.searchInChatRoom(chatRoomId, userId, keyword, 0, 20);

        // then
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(m -> m.getContent().contains("안녕"));
    }

    @Test
    @DisplayName("채팅방 멤버가 아닌 경우 검색 실패")
    void should_throwException_when_notChatRoomMember() {
        // given
        Long chatRoomId = 1L;
        Long userId = 100L;
        String keyword = "안녕";

        given(chatRoomMemberRepository.existsByChatRoomIdAndUserId(chatRoomId, userId)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> searchMessageService.searchInChatRoom(chatRoomId, userId, keyword, 0, 20))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("채팅방 멤버만 메시지를 검색할 수 있습니다.");
    }

    @Test
    @DisplayName("검색어가 비어있는 경우 빈 결과 반환")
    void should_returnEmpty_when_keywordIsEmpty() {
        // given
        Long chatRoomId = 1L;
        Long userId = 100L;
        String keyword = "";

        given(chatRoomMemberRepository.existsByChatRoomIdAndUserId(chatRoomId, userId)).willReturn(true);

        // when
        List<Message> result = searchMessageService.searchInChatRoom(chatRoomId, userId, keyword, 0, 20);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("사용자의 모든 채팅방에서 메시지 검색 성공")
    void should_returnMessages_when_searchAcrossAllChatRooms() {
        // given
        Long userId = 100L;
        String keyword = "회의";

        Message message1 = Message.builder()
                .id(1L)
                .chatRoomId(1L)
                .senderId(100L)
                .content("회의 시간 알려주세요")
                .createdAt(LocalDateTime.now())
                .build();

        Message message2 = Message.builder()
                .id(2L)
                .chatRoomId(2L)
                .senderId(200L)
                .content("회의 끝났어요")
                .createdAt(LocalDateTime.now().minusHours(1))
                .build();

        given(messageRepository.searchByKeywordInUserChatRooms(userId, keyword, 0, 20))
                .willReturn(List.of(message1, message2));

        // when
        List<Message> result = searchMessageService.searchAcrossAllChatRooms(userId, keyword, 0, 20);

        // then
        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("검색 결과가 없는 경우 빈 리스트 반환")
    void should_returnEmptyList_when_noMatchingMessages() {
        // given
        Long chatRoomId = 1L;
        Long userId = 100L;
        String keyword = "존재하지않는키워드";

        given(chatRoomMemberRepository.existsByChatRoomIdAndUserId(chatRoomId, userId)).willReturn(true);
        given(messageRepository.searchByKeywordInChatRoom(chatRoomId, keyword, 0, 20))
                .willReturn(List.of());

        // when
        List<Message> result = searchMessageService.searchInChatRoom(chatRoomId, userId, keyword, 0, 20);

        // then
        assertThat(result).isEmpty();
    }
}
