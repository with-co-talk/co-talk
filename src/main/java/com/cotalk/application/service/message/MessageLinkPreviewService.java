package com.cotalk.application.service.message;

import com.cotalk.domain.entity.Message;
import com.cotalk.domain.port.inbound.linkpreview.GetLinkPreviewUseCase;
import com.cotalk.domain.port.inbound.linkpreview.GetLinkPreviewUseCase.LinkPreviewResult;
import com.cotalk.domain.port.outbound.ChatMessageBroker;
import com.cotalk.domain.port.outbound.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 메시지에 포함된 URL의 링크 미리보기를 비동기로 수집하여 메시지에 저장하는 서비스.
 * 메시지 전송 후 호출되며, 메시지 응답 지연 없이 백그라운드에서 OG 메타를 수집한다.
 *
 * @author seunggu.lee
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageLinkPreviewService {

    private static final java.util.regex.Pattern URL_PATTERN =
            java.util.regex.Pattern.compile("https?://[^\\s<>\"']+");

    private final MessageRepository messageRepository;
    private final GetLinkPreviewUseCase getLinkPreviewUseCase;
    private final ChatMessageBroker chatMessageBroker;

    /**
     * 메시지 내용에서 첫 번째 URL을 추출한다.
     * 끝의 구두점(.,;:!?)은 제거한다.
     *
     * @param content 메시지 내용
     * @return 추출된 URL (없으면 empty)
     */
    public Optional<String> extractFirstUrl(String content) {
        if (content == null || content.isBlank()) {
            return Optional.empty();
        }
        java.util.regex.Matcher matcher = URL_PATTERN.matcher(content);
        if (!matcher.find()) {
            return Optional.empty();
        }
        String url = matcher.group();
        url = url.replaceAll("[.,;:!?)]+$", "");
        return Optional.of(url);
    }

    /**
     * URL에서 링크 미리보기를 수집하여 해당 메시지에 저장한다.
     * 메시지 전송 직후 비동기로 호출되므로 메시지 응답 지연이 없다.
     * 링크 미리보기 저장 후 WebSocket을 통해 실시간으로 클라이언트에게 업데이트를 전송한다.
     *
     * @param messageId 메시지 ID
     * @param url       미리보기 수집할 URL
     */
    @Async
    @Transactional
    public void fetchAndSaveLinkPreview(Long messageId, String url) {
        try {
            LinkPreviewResult result = getLinkPreviewUseCase.getLinkPreview(url);
            Optional<Message> opt = messageRepository.findById(messageId);
            if (opt.isEmpty()) {
                return;
            }
            Message message = opt.get();
            message.applyLinkPreview(
                    result.url(),
                    result.title(),
                    result.description(),
                    result.imageUrl()
            );
            messageRepository.save(message);
            log.debug("Link preview saved for messageId={}, url={}", messageId, url);

            // Publish LINK_PREVIEW_UPDATED event so WebSocket clients can update in real-time
            Map<String, Object> event = new LinkedHashMap<>();
            event.put("schemaVersion", 1);
            event.put("eventId", "linkpreview:" + messageId + ":" + System.currentTimeMillis());
            event.put("eventType", "LINK_PREVIEW_UPDATED");
            event.put("chatRoomId", message.getChatRoomId());
            event.put("messageId", messageId);
            event.put("linkPreviewUrl", result.url());
            event.put("linkPreviewTitle", result.title());
            event.put("linkPreviewDescription", result.description());
            event.put("linkPreviewImageUrl", result.imageUrl());
            chatMessageBroker.publishRoomEvent(message.getChatRoomId(), event);
            log.debug("Published LINK_PREVIEW_UPDATED event for messageId={}", messageId);
        } catch (Exception e) {
            log.warn("Failed to fetch link preview for messageId={}, url={}: {}", messageId, url, e.getMessage());
        }
    }
}
