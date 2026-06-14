package com.cotalk.application.service.message;

import com.cotalk.application.service.message.MessageSearchBackfillService.BackfillOptions;
import com.cotalk.application.service.message.MessageSearchBackfillService.BackfillResult;
import com.cotalk.domain.entity.Message;
import com.cotalk.domain.entity.Message.MessageType;
import com.cotalk.domain.port.outbound.BlindIndexTokenizer;
import com.cotalk.domain.port.outbound.MessageSearchBackfillPort;
import com.cotalk.domain.port.outbound.MessageSearchTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * {@link MessageSearchBackfillService} 단위 테스트.
 *
 * <p>커서 청크 반복, 토큰화 호출, idempotent delete-then-insert, 커서 진행, skip-existing,
 * 빈 토큰(3글자 미만) 처리를 Mockito로 검증한다.</p>
 *
 * @author seunggu.lee
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("메시지 검색 토큰 백필 서비스")
class MessageSearchBackfillServiceTest {

    @Mock
    private MessageSearchBackfillPort backfillPort;
    @Mock
    private MessageSearchTokenRepository messageSearchTokenRepository;
    @Mock
    private BlindIndexTokenizer blindIndexTokenizer;
    @Mock
    private TransactionTemplate transactionTemplate;

    private MessageSearchBackfillService service;

    @BeforeEach
    void setUp() {
        service = new MessageSearchBackfillService(
                backfillPort, messageSearchTokenRepository, blindIndexTokenizer, transactionTemplate);
        // 트랜잭션 콜백을 즉시 실행
        lenient().when(transactionTemplate.execute(any(TransactionCallback.class)))
                .thenAnswer(invocation -> {
                    TransactionCallback<?> callback = invocation.getArgument(0);
                    return callback.doInTransaction(null);
                });
    }

    private Message textMessage(long id, String content) {
        return Message.builder().id(id).chatRoomId(1L).senderId(1L)
                .content(content).type(MessageType.TEXT).build();
    }

    @Test
    @DisplayName("커서 청크를 반복 조회해 빈 청크가 나올 때까지 모든 메시지를 토큰화한다")
    void should_iterateChunks_untilEmpty() {
        Message m1 = textMessage(10L, "첫번째 메시지");
        Message m2 = textMessage(20L, "두번째 메시지");
        Message m3 = textMessage(30L, "세번째 메시지");

        // chunkSize=2: [m1,m2] → [m3] → []
        given(backfillPort.findTextMessagesForBackfill(0L, 2)).willReturn(List.of(m1, m2));
        given(backfillPort.findTextMessagesForBackfill(20L, 2)).willReturn(List.of(m3));
        given(blindIndexTokenizer.tokenize(any())).willReturn(Set.of("tokA", "tokB"));

        BackfillResult result = service.backfill(new BackfillOptions(2, 0L, false));

        // m3 청크가 chunkSize 미만이라 추가 조회 없이 종료
        verify(backfillPort).findTextMessagesForBackfill(0L, 2);
        verify(backfillPort).findTextMessagesForBackfill(20L, 2);
        verify(blindIndexTokenizer, times(3)).tokenize(any());
        assertThat(result.scanned()).isEqualTo(3L);
        assertThat(result.indexed()).isEqualTo(3L);
        assertThat(result.lastCursorId()).isEqualTo(30L);
    }

    @Test
    @DisplayName("메시지별로 delete-then-insert 순서로 idempotent하게 재적재한다")
    void should_deleteThenInsert_perMessage() {
        Message m1 = textMessage(10L, "회의시간 안내");
        given(backfillPort.findTextMessagesForBackfill(0L, 500)).willReturn(List.of(m1));
        given(blindIndexTokenizer.tokenize("회의시간 안내")).willReturn(Set.of("t1", "t2"));

        service.backfill(BackfillOptions.defaults());

        InOrder order = inOrder(messageSearchTokenRepository);
        order.verify(messageSearchTokenRepository).deleteByMessageId(10L);
        order.verify(messageSearchTokenRepository).saveTokens(eq(10L), eq(Set.of("t1", "t2")));
    }

    @Test
    @DisplayName("skipExisting=true면 이미 토큰이 있는 메시지는 복호화/토큰화 없이 건너뛴다")
    void should_skipMessagesWithExistingTokens_when_skipExisting() {
        Message m1 = textMessage(10L, "이미 토큰 있음");
        Message m2 = textMessage(20L, "토큰 없음 신규");
        given(backfillPort.findTextMessagesForBackfill(0L, 500)).willReturn(List.of(m1, m2));
        given(backfillPort.hasTokens(10L)).willReturn(true);
        given(backfillPort.hasTokens(20L)).willReturn(false);
        given(blindIndexTokenizer.tokenize("토큰 없음 신규")).willReturn(Set.of("x"));

        BackfillResult result = service.backfill(new BackfillOptions(500, 0L, true));

        verify(blindIndexTokenizer, never()).tokenize("이미 토큰 있음");
        verify(messageSearchTokenRepository, never()).deleteByMessageId(10L);
        verify(messageSearchTokenRepository).deleteByMessageId(20L);
        verify(messageSearchTokenRepository).saveTokens(eq(20L), any());
        assertThat(result.scanned()).isEqualTo(2L);
        assertThat(result.indexed()).isEqualTo(1L);
        assertThat(result.skipped()).isEqualTo(1L);
    }

    @Test
    @DisplayName("skipExisting=false면 토큰 존재 여부를 확인하지 않고 모두 재토큰화한다")
    void should_notCheckExistence_when_skipExistingFalse() {
        Message m1 = textMessage(10L, "강제 재토큰화");
        given(backfillPort.findTextMessagesForBackfill(0L, 500)).willReturn(List.of(m1));
        given(blindIndexTokenizer.tokenize(any())).willReturn(Set.of("a"));

        service.backfill(new BackfillOptions(500, 0L, false));

        verify(backfillPort, never()).hasTokens(anyLong());
        verify(messageSearchTokenRepository).deleteByMessageId(10L);
    }

    @Test
    @DisplayName("토큰이 비어도(3글자 미만) 기존 토큰은 삭제하되 새 토큰은 적재하지 않는다")
    void should_deleteButNotInsert_when_tokensEmpty() {
        Message m1 = textMessage(10L, "ab");
        given(backfillPort.findTextMessagesForBackfill(0L, 500)).willReturn(List.of(m1));
        given(blindIndexTokenizer.tokenize("ab")).willReturn(Set.of());

        BackfillResult result = service.backfill(new BackfillOptions(500, 0L, false));

        verify(messageSearchTokenRepository).deleteByMessageId(10L);
        verify(messageSearchTokenRepository, never()).saveTokens(anyLong(), any());
        assertThat(result.indexed()).isEqualTo(0L);
        assertThat(result.scanned()).isEqualTo(1L);
    }

    @Test
    @DisplayName("처리할 메시지가 없으면 즉시 종료하고 빈 결과를 반환한다")
    void should_returnEmpty_when_noMessages() {
        given(backfillPort.findTextMessagesForBackfill(0L, 500)).willReturn(List.of());

        BackfillResult result = service.backfill(BackfillOptions.defaults());

        assertThat(result.scanned()).isZero();
        assertThat(result.indexed()).isZero();
        assertThat(result.lastCursorId()).isZero();
        verify(blindIndexTokenizer, never()).tokenize(any());
    }

    @Test
    @DisplayName("재실행해도 동일한 delete-then-insert 결과로 idempotent하다")
    void should_beIdempotent_onRerun() {
        Message m1 = textMessage(10L, "재실행 안전 메시지");
        given(backfillPort.findTextMessagesForBackfill(0L, 500)).willReturn(List.of(m1));
        given(blindIndexTokenizer.tokenize(any())).willReturn(Set.of("z1", "z2"));

        BackfillResult first = service.backfill(new BackfillOptions(500, 0L, false));
        BackfillResult second = service.backfill(new BackfillOptions(500, 0L, false));

        assertThat(first).isEqualTo(second);
        // 두 번 실행 → delete-then-insert 두 번 (중복/오류 없음)
        verify(messageSearchTokenRepository, times(2)).deleteByMessageId(10L);
        verify(messageSearchTokenRepository, times(2)).saveTokens(eq(10L), any());
    }

    @Test
    @DisplayName("null/비정상 옵션은 안전 기본값으로 보정한다")
    void should_sanitizeOptions() {
        given(backfillPort.findTextMessagesForBackfill(0L, 1)).willReturn(List.of());

        // chunkSize=0 → 1로 보정되어 NPE/무한루프 없이 동작
        BackfillResult result = service.backfill(new BackfillOptions(0, -5L, true));

        assertThat(result.scanned()).isZero();
        verify(backfillPort).findTextMessagesForBackfill(0L, 1);
    }
}
