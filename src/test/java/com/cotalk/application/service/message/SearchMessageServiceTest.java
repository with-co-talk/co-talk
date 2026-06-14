package com.cotalk.application.service.message;

import com.cotalk.domain.entity.Message;
import com.cotalk.domain.port.outbound.BlindIndexTokenizer;
import com.cotalk.domain.port.outbound.ChatRoomMemberRepository;
import com.cotalk.domain.port.outbound.MessageRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

/**
 * {@link SearchMessageService} 단위 테스트.
 *
 * <p>블라인드 인덱스 토큰화 호출 + 복호화 substring 최종 필터 + 길이 가드(3글자 미만 거부)를 검증한다.
 * 토큰화는 Mock으로 대체하되, "키워드를 포함하지 않는" 메시지가 2단계 substring 필터에서
 * 제거되는지(false positive 제거)를 핵심으로 본다.</p>
 */
@ExtendWith(MockitoExtension.class)
class SearchMessageServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @Mock
    private BlindIndexTokenizer tokenizer;

    @InjectMocks
    private SearchMessageService searchMessageService;

    /**
     * 테스트용 토큰화/정규화 동작을 설정한다.
     * normalize는 소문자+공백제거, tokenizeQuery는 비어있지 않은 토큰을 돌려준다.
     */
    private void stubTokenizer(String keyword) {
        stubNormalize();
        lenient().when(tokenizer.tokenizeQuery(keyword)).thenReturn(Set.of("tok1", "tok2"));
    }

    /**
     * normalize 스텁: 실제 구현과 동일하게 소문자+공백제거, null→"".
     * 길이 가드가 normalize 결과를 사용하므로(조용한 빈 결과 방지) 거부 경로 테스트에도 필요하다.
     */
    private void stubNormalize() {
        lenient().when(tokenizer.normalize(any())).thenAnswer(inv -> {
            String s = inv.getArgument(0);
            return s == null ? "" : s.toLowerCase().replaceAll("\\s+", "");
        });
    }

    @Test
    @DisplayName("채팅방 내 메시지 검색 성공 — 토큰 조회 후 복호화 substring 필터 통과분 반환")
    void should_returnMessages_when_searchInChatRoom() {
        Long chatRoomId = 1L;
        Long userId = 100L;
        String keyword = "안녕하세요";
        stubTokenizer(keyword);

        Message m1 = Message.builder().id(1L).chatRoomId(chatRoomId).senderId(100L).content("안녕하세요!").build();
        Message m2 = Message.builder().id(2L).chatRoomId(chatRoomId).senderId(200L).content("안녕하세요 반가워요").build();

        given(chatRoomMemberRepository.existsByChatRoomIdAndUserId(chatRoomId, userId)).willReturn(true);
        given(messageRepository.searchByTokensInChatRoom(eq(chatRoomId), any(), anyLong(), anyLong(), anyInt()))
                .willReturn(List.of(m1, m2));

        List<Message> result = searchMessageService.searchInChatRoom(chatRoomId, userId, keyword, 0, 20);

        assertThat(result).hasSize(2)
                .allSatisfy(m -> assertThat(m.getContent()).contains("안녕하세요"));
    }

    @Test
    @DisplayName("토큰은 맞지만 substring이 아닌 false positive는 2단계 필터에서 제거된다")
    void should_removeFalsePositive_when_substringNotPresent() {
        Long chatRoomId = 1L;
        Long userId = 100L;
        String keyword = "비밀번호";
        stubTokenizer(keyword);

        // "비밀번호"의 트라이그램은 흩어져 포함되지만 연속 substring은 아닌 메시지 (false positive)
        Message hit = Message.builder().id(1L).chatRoomId(chatRoomId).senderId(100L).content("새 비밀번호로 변경했어요").build();
        Message falsePositive = Message.builder().id(2L).chatRoomId(chatRoomId).senderId(200L)
                .content("비밀 얘기, 전화번호 알려줘").build(); // '비밀번호' 연속 아님

        given(chatRoomMemberRepository.existsByChatRoomIdAndUserId(chatRoomId, userId)).willReturn(true);
        given(messageRepository.searchByTokensInChatRoom(eq(chatRoomId), any(), anyLong(), anyLong(), anyInt()))
                .willReturn(List.of(hit, falsePositive));

        List<Message> result = searchMessageService.searchInChatRoom(chatRoomId, userId, keyword, 0, 20);

        assertThat(result).extracting(Message::getId).containsExactly(1L);
    }

    @Test
    @DisplayName("채팅방 멤버가 아닌 경우 검색 실패")
    void should_throwException_when_notChatRoomMember() {
        Long chatRoomId = 1L;
        Long userId = 100L;

        given(chatRoomMemberRepository.existsByChatRoomIdAndUserId(chatRoomId, userId)).willReturn(false);

        assertThatThrownBy(() -> searchMessageService.searchInChatRoom(chatRoomId, userId, "안녕하세요", 0, 20))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("채팅방 멤버만 메시지를 검색할 수 있습니다.");
    }

    @ParameterizedTest(name = "검색어가 {0}인 경우 빈 결과 반환")
    @MethodSource("blankKeywordSource")
    @DisplayName("검색어가 비어있거나 공백인 경우 빈 결과 반환")
    void should_returnEmpty_when_keywordIsEmptyOrBlank(String displayValue, String keyword) {
        Long chatRoomId = 1L;
        Long userId = 100L;

        given(chatRoomMemberRepository.existsByChatRoomIdAndUserId(chatRoomId, userId)).willReturn(true);

        List<Message> result = searchMessageService.searchInChatRoom(chatRoomId, userId, keyword, 0, 20);

        assertThat(result).isEmpty();
    }

    static Stream<Arguments> blankKeywordSource() {
        return Stream.of(
                Arguments.of("empty", ""),
                Arguments.of("null", (String) null),
                Arguments.of("whitespace", "   ")
        );
    }

    @ParameterizedTest(name = "{0}글자 키워드 \"{1}\"는 거부된다")
    @MethodSource("shortKeywordSource")
    @DisplayName("3글자 미만 키워드는 검색을 거부한다 (블라인드 인덱스 트라이그램 불가)")
    void should_rejectKeyword_when_shorterThanThreeChars(int len, String keyword) {
        Long chatRoomId = 1L;
        Long userId = 100L;
        stubNormalize();

        given(chatRoomMemberRepository.existsByChatRoomIdAndUserId(chatRoomId, userId)).willReturn(true);

        assertThatThrownBy(() -> searchMessageService.searchInChatRoom(chatRoomId, userId, keyword, 0, 20))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("3글자");
    }

    @Test
    @DisplayName("전체 채팅방 검색에서도 3글자 미만 키워드는 거부한다")
    void should_rejectKeyword_when_shortInAllChatRooms() {
        stubNormalize();
        assertThatThrownBy(() -> searchMessageService.searchAcrossAllChatRooms(100L, "가나", 0, 20))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("3글자");
    }

    static Stream<Arguments> shortKeywordSource() {
        return Stream.of(
                Arguments.of(1, "가"),
                Arguments.of(2, "가나"),
                Arguments.of(2, "ab")
        );
    }

    @Test
    @DisplayName("사용자의 모든 채팅방에서 메시지 검색 성공")
    void should_returnMessages_when_searchAcrossAllChatRooms() {
        Long userId = 100L;
        String keyword = "회의시간";
        stubTokenizer(keyword);

        Message m1 = Message.builder().id(1L).chatRoomId(1L).senderId(100L).content("회의시간 알려주세요").build();
        Message m2 = Message.builder().id(2L).chatRoomId(2L).senderId(200L).content("회의시간 정했어요").build();

        given(messageRepository.searchByTokensInUserChatRooms(eq(userId), any(), anyLong(), anyLong(), anyInt()))
                .willReturn(List.of(m1, m2));

        List<Message> result = searchMessageService.searchAcrossAllChatRooms(userId, keyword, 0, 20);

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("소프트 삭제된 메시지는 결과에서 제외된다")
    void should_excludeDeletedMessages() {
        Long chatRoomId = 1L;
        Long userId = 100L;
        String keyword = "회의시간";
        stubTokenizer(keyword);

        Message alive = Message.builder().id(1L).chatRoomId(chatRoomId).senderId(100L).content("회의시간 공유").build();
        Message deleted = Message.builder().id(2L).chatRoomId(chatRoomId).senderId(200L).content("회의시간 변경").deleted(true).build();

        given(chatRoomMemberRepository.existsByChatRoomIdAndUserId(chatRoomId, userId)).willReturn(true);
        given(messageRepository.searchByTokensInChatRoom(eq(chatRoomId), any(), anyLong(), anyLong(), anyInt()))
                .willReturn(List.of(alive, deleted));

        List<Message> result = searchMessageService.searchInChatRoom(chatRoomId, userId, keyword, 0, 20);

        assertThat(result).extracting(Message::getId).containsExactly(1L);
    }

    @Test
    @DisplayName("over-fetch: substring 통과분을 size만큼만 잘라 반환한다")
    void should_limitToSize_afterOverFetch() {
        Long chatRoomId = 1L;
        Long userId = 100L;
        String keyword = "회의시간";
        stubTokenizer(keyword);

        // size=2 이지만 substring 통과 메시지가 3개 → 2개만 반환
        Message a = Message.builder().id(1L).chatRoomId(chatRoomId).senderId(100L).content("회의시간 1").build();
        Message b = Message.builder().id(2L).chatRoomId(chatRoomId).senderId(100L).content("회의시간 2").build();
        Message c = Message.builder().id(3L).chatRoomId(chatRoomId).senderId(100L).content("회의시간 3").build();

        given(chatRoomMemberRepository.existsByChatRoomIdAndUserId(chatRoomId, userId)).willReturn(true);
        given(messageRepository.searchByTokensInChatRoom(eq(chatRoomId), any(), anyLong(), anyLong(), anyInt()))
                .willReturn(List.of(a, b, c));

        List<Message> result = searchMessageService.searchInChatRoom(chatRoomId, userId, keyword, 0, 2);

        assertThat(result).hasSize(2).extracting(Message::getId).containsExactly(1L, 2L);
    }

    @Test
    @DisplayName("page>0이면 오프셋이 page*size(=윈도우 크기가 아니라 사용자 페이지 기준)로 전달된다")
    void should_useUserPageOffset_when_pageGreaterThanZero() {
        Long chatRoomId = 1L;
        Long userId = 100L;
        String keyword = "회의시간";
        stubTokenizer(keyword);

        given(chatRoomMemberRepository.existsByChatRoomIdAndUserId(chatRoomId, userId)).willReturn(true);
        given(messageRepository.searchByTokensInChatRoom(eq(chatRoomId), any(), anyLong(), anyLong(), anyInt()))
                .willReturn(List.of());

        // page=2, size=20 → offset은 2*20=40 이어야 한다 (over-fetch 윈도우 60이 아니라).
        searchMessageService.searchInChatRoom(chatRoomId, userId, keyword, 2, 20);

        ArgumentCaptor<Long> offsetCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Integer> limitCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(messageRepository).searchByTokensInChatRoom(
                eq(chatRoomId), any(), anyLong(), offsetCaptor.capture(), limitCaptor.capture());

        assertThat(offsetCaptor.getValue()).isEqualTo(40L);
        assertThat(limitCaptor.getValue()).isEqualTo(60); // over-fetch = size * 3
    }

    @Test
    @DisplayName("전체 채팅방 검색도 page>0에서 사용자 페이지 기준 오프셋을 사용한다")
    void should_useUserPageOffset_inAllChatRooms_when_pageGreaterThanZero() {
        Long userId = 100L;
        String keyword = "회의시간";
        stubTokenizer(keyword);

        given(messageRepository.searchByTokensInUserChatRooms(eq(userId), any(), anyLong(), anyLong(), anyInt()))
                .willReturn(List.of());

        searchMessageService.searchAcrossAllChatRooms(userId, keyword, 3, 10);

        ArgumentCaptor<Long> offsetCaptor = ArgumentCaptor.forClass(Long.class);
        verify(messageRepository).searchByTokensInUserChatRooms(
                eq(userId), any(), anyLong(), offsetCaptor.capture(), anyInt());

        assertThat(offsetCaptor.getValue()).isEqualTo(30L); // 3 * 10
    }

    @Test
    @DisplayName("내부 공백 제거(normalize) 후 3글자 미만이면 거부한다 (\"a b\"=2글자) — 조용한 빈 결과 방지")
    void should_rejectKeyword_when_normalizedLengthBelowThree() {
        Long chatRoomId = 1L;
        Long userId = 100L;
        // normalize는 공백을 제거하므로 "a b" → "ab"(2글자) → 트라이그램 0개가 되어야 한다.
        stubNormalize();

        given(chatRoomMemberRepository.existsByChatRoomIdAndUserId(chatRoomId, userId)).willReturn(true);

        assertThatThrownBy(() -> searchMessageService.searchInChatRoom(chatRoomId, userId, "a b", 0, 20))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("3글자");
    }
}
