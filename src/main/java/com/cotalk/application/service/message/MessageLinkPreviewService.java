package com.cotalk.application.service.message;

import com.cotalk.domain.entity.Message;
import com.cotalk.domain.port.outbound.ChatMessageBroker;
import com.cotalk.domain.port.outbound.LinkPreviewQueryPort;
import com.cotalk.domain.port.outbound.LinkPreviewQueryResult;
import com.cotalk.domain.port.outbound.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final LinkPreviewQueryPort linkPreviewQueryPort;
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
        Message message;
        LinkPreviewQueryResult result;
        // DB 커밋 관심사: 미리보기 수집/저장 실패는 그레이스풀 디그레이션(메시지 자체는 유지)이므로
        // 여기서만 광범위하게 흡수한다. 이 블록을 통과하지 못하면 이벤트 발행도 하지 않는다.
        try {
            result = linkPreviewQueryPort.queryLinkPreview(url);
            Optional<Message> opt = messageRepository.findById(messageId);
            if (opt.isEmpty()) {
                return;
            }
            message = opt.get();
            message.applyLinkPreview(
                    result.url(),
                    result.title(),
                    result.description(),
                    result.imageUrl()
            );
            messageRepository.save(message);
            log.debug("Link preview saved for messageId={}, url={}", messageId, url);
        } catch (Exception e) {
            log.warn("Failed to fetch/save link preview for messageId={}, url={}: {}", messageId, url, e.getMessage());
            return;
        }

        // 발행 관심사: DB 커밋과 분리한다. 발행 실패는 영속화된 미리보기와 별개의 문제이므로
        // 별도 try로 좁혀 명확한 메시지/레벨로 가시화한다(그레이스풀 디그레이션은 유지).
        publishLinkPreviewUpdated(messageId, message.getChatRoomId(), result);
    }

    /**
     * LINK_PREVIEW_UPDATED 이벤트를 채팅방에 발행하여 WebSocket 클라이언트가 실시간 갱신하도록 한다.
     *
     * <p>발행 실패가 메시지 영속화(이미 커밋됨)를 되돌리지 않도록 DB 관심사와 분리해 호출한다.
     * 지속적인 발행 실패도 최소한 가시적으로 로깅되도록 별도 try로 좁힌다.</p>
     *
     * @param messageId  메시지 ID
     * @param chatRoomId 채팅방 ID
     * @param result     수집된 링크 미리보기 결과
     */
    private void publishLinkPreviewUpdated(Long messageId, Long chatRoomId, LinkPreviewQueryResult result) {
        try {
            LinkPreviewUpdatedEvent event = new LinkPreviewUpdatedEvent(
                    1,
                    "linkpreview:" + messageId + ":" + System.currentTimeMillis(),
                    "LINK_PREVIEW_UPDATED",
                    chatRoomId,
                    messageId,
                    result.url(),
                    result.title(),
                    result.description(),
                    result.imageUrl()
            );
            chatMessageBroker.publishRoomEvent(chatRoomId, event);
            log.debug("Published LINK_PREVIEW_UPDATED event for messageId={}", messageId);
        } catch (Exception e) {
            log.warn("Failed to publish LINK_PREVIEW_UPDATED event for messageId={}: {}", messageId, e.getMessage());
        }
    }

    /**
     * 링크 미리보기 갱신 이벤트 DTO.
     * Redis Pub/Sub -> WebSocket 방 토픽(/topic/chat/room/{roomId})으로 전달되는 이벤트다.
     *
     * @param schemaVersion          스키마 버전
     * @param eventId                이벤트 고유 ID (중복 체크용)
     * @param eventType              이벤트 유형 (LINK_PREVIEW_UPDATED)
     * @param chatRoomId             채팅방 ID
     * @param messageId              미리보기가 갱신된 메시지 ID
     * @param linkPreviewUrl         미리보기 대상 URL
     * @param linkPreviewTitle       미리보기 제목
     * @param linkPreviewDescription 미리보기 설명
     * @param linkPreviewImageUrl    미리보기 이미지 URL
     */
    private record LinkPreviewUpdatedEvent(
            Integer schemaVersion,
            String eventId,
            String eventType,
            Long chatRoomId,
            Long messageId,
            String linkPreviewUrl,
            String linkPreviewTitle,
            String linkPreviewDescription,
            String linkPreviewImageUrl
    ) {}
}
