package com.cotalk.application.service.message;

import com.cotalk.domain.entity.Message;
import com.cotalk.domain.port.inbound.linkpreview.GetLinkPreviewUseCase;
import com.cotalk.domain.port.inbound.linkpreview.GetLinkPreviewUseCase.LinkPreviewResult;
import com.cotalk.domain.port.outbound.ChatMessageBroker;
import com.cotalk.domain.port.outbound.MessageRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * MessageLinkPreviewService 단위 테스트.
 * TDD를 통해 링크 미리보기 WebSocket 실시간 업데이트 기능을 검증한다.
 *
 * @author seunggu.lee
 */
@ExtendWith(MockitoExtension.class)
class MessageLinkPreviewServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private GetLinkPreviewUseCase getLinkPreviewUseCase;

    @Mock
    private ChatMessageBroker chatMessageBroker;

    @Captor
    private ArgumentCaptor<Map<String, Object>> eventCaptor;

    @InjectMocks
    private MessageLinkPreviewService messageLinkPreviewService;

    @Test
    @DisplayName("링크 미리보기 저장 후 LINK_PREVIEW_UPDATED 이벤트를 발행해야 함")
    void should_publishLinkPreviewUpdatedEvent_when_linkPreviewSaved() {
        // Given: 링크 미리보기를 가져올 메시지와 URL
        Long messageId = 100L;
        Long chatRoomId = 10L;
        String url = "https://example.com";

        LinkPreviewResult linkPreview = new LinkPreviewResult(
                url,
                "Example Title",
                "Example Description",
                "https://example.com/image.jpg",
                "example.com",
                "Example Site",
                "https://example.com/favicon.ico"
        );

        Message message = Message.builder()
                .id(messageId)
                .chatRoomId(chatRoomId)
                .senderId(1L)
                .content("Check this out: " + url)
                .type(Message.MessageType.TEXT)
                .build();

        given(getLinkPreviewUseCase.getLinkPreview(url)).willReturn(linkPreview);
        given(messageRepository.findById(messageId)).willReturn(Optional.of(message));
        given(messageRepository.save(any(Message.class))).willReturn(message);

        // When: fetchAndSaveLinkPreview 호출
        messageLinkPreviewService.fetchAndSaveLinkPreview(messageId, url);

        // Then: chatMessageBroker.publishRoomEvent가 호출되어야 함
        verify(chatMessageBroker, times(1)).publishRoomEvent(eq(chatRoomId), eventCaptor.capture());

        Map<String, Object> event = eventCaptor.getValue();
        assertThat(event).isNotNull();
        assertThat(event.get("schemaVersion")).isEqualTo(1);
        assertThat(event.get("eventType")).isEqualTo("LINK_PREVIEW_UPDATED");
        assertThat(event.get("chatRoomId")).isEqualTo(chatRoomId);
        assertThat(event.get("messageId")).isEqualTo(messageId);
        assertThat(event.get("linkPreviewUrl")).isEqualTo(url);
        assertThat(event.get("linkPreviewTitle")).isEqualTo("Example Title");
        assertThat(event.get("linkPreviewDescription")).isEqualTo("Example Description");
        assertThat(event.get("linkPreviewImageUrl")).isEqualTo("https://example.com/image.jpg");
        assertThat(event.get("eventId")).asString().startsWith("linkpreview:" + messageId + ":");
    }

    @Test
    @DisplayName("메시지가 존재하지 않으면 이벤트를 발행하지 않음")
    void should_notPublishEvent_when_messageNotFound() {
        // Given: 존재하지 않는 메시지
        Long messageId = 999L;
        String url = "https://example.com";

        LinkPreviewResult linkPreview = new LinkPreviewResult(
                url,
                "Example Title",
                "Example Description",
                "https://example.com/image.jpg",
                "example.com",
                "Example Site",
                "https://example.com/favicon.ico"
        );

        given(getLinkPreviewUseCase.getLinkPreview(url)).willReturn(linkPreview);
        given(messageRepository.findById(messageId)).willReturn(Optional.empty());

        // When: fetchAndSaveLinkPreview 호출
        messageLinkPreviewService.fetchAndSaveLinkPreview(messageId, url);

        // Then: publishRoomEvent가 호출되지 않아야 함
        verify(chatMessageBroker, never()).publishRoomEvent(any(), any());
        verify(messageRepository, never()).save(any());
    }

    @Test
    @DisplayName("링크 미리보기 가져오기 실패 시 이벤트를 발행하지 않음")
    void should_notPublishEvent_when_linkPreviewFetchFails() {
        // Given: 링크 미리보기 가져오기 실패
        Long messageId = 100L;
        String url = "https://invalid-url.com";

        given(getLinkPreviewUseCase.getLinkPreview(url))
                .willThrow(new RuntimeException("Failed to fetch link preview"));

        // When: fetchAndSaveLinkPreview 호출
        messageLinkPreviewService.fetchAndSaveLinkPreview(messageId, url);

        // Then: publishRoomEvent가 호출되지 않아야 함
        verify(chatMessageBroker, never()).publishRoomEvent(any(), any());
        verify(messageRepository, never()).save(any());
    }

    @Test
    @DisplayName("URL에서 첫 번째 URL을 올바르게 추출해야 함")
    void should_extractFirstUrl_when_multipleUrlsInContent() {
        // Given: 여러 URL이 포함된 내용
        String content = "Check this https://example.com and this https://test.com";

        // When: extractFirstUrl 호출
        Optional<String> url = messageLinkPreviewService.extractFirstUrl(content);

        // Then: 첫 번째 URL만 추출되어야 함
        assertThat(url).isPresent();
        assertThat(url.get()).isEqualTo("https://example.com");
    }

    @Test
    @DisplayName("URL 끝의 구두점을 제거해야 함")
    void should_removePunctuation_when_urlEndsWithPunctuation() {
        // Given: 구두점으로 끝나는 URL
        String content = "Check this https://example.com.";

        // When: extractFirstUrl 호출
        Optional<String> url = messageLinkPreviewService.extractFirstUrl(content);

        // Then: 구두점이 제거된 URL이 반환되어야 함
        assertThat(url).isPresent();
        assertThat(url.get()).isEqualTo("https://example.com");
    }

    @Test
    @DisplayName("URL이 없으면 빈 Optional을 반환해야 함")
    void should_returnEmpty_when_noUrlInContent() {
        // Given: URL이 없는 내용
        String content = "This is just text without any link";

        // When: extractFirstUrl 호출
        Optional<String> url = messageLinkPreviewService.extractFirstUrl(content);

        // Then: 빈 Optional이 반환되어야 함
        assertThat(url).isEmpty();
    }
}
