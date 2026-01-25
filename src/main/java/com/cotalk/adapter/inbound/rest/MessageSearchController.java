package com.cotalk.adapter.inbound.rest;

import com.cotalk.adapter.inbound.rest.dto.message.MessageSearchResponse;
import com.cotalk.adapter.inbound.rest.dto.message.SearchedMessageDto;
import com.cotalk.domain.entity.Message;
import com.cotalk.domain.port.inbound.message.SearchMessageUseCase;
import com.cotalk.infrastructure.security.CustomUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 메시지 검색을 위한 REST 컨트롤러.
 * <p>
 * 특정 채팅방 내 또는 전체 채팅방에서 키워드로 메시지를 검색하는 기능을 제공합니다.
 *
 * @author seunggu.lee
 */
@RestController
@RequestMapping("/api/v1/messages/search")
@RequiredArgsConstructor
@Tag(name = "메시지 검색", description = "메시지 검색 API")
public class MessageSearchController {

    private final SearchMessageUseCase searchMessageUseCase;

    /**
     * 특정 채팅방 내에서 키워드로 메시지를 검색합니다.
     *
     * @param principal  인증된 사용자 정보
     * @param chatRoomId 채팅방 ID
     * @param keyword    검색 키워드
     * @param page       페이지 번호 (기본값: 0)
     * @param size       페이지 크기 (기본값: 20)
     * @return 검색된 메시지 목록
     */
    @Operation(summary = "채팅방 내 메시지 검색", description = "특정 채팅방 내에서 키워드로 메시지를 검색합니다.")
    @GetMapping
    public ResponseEntity<MessageSearchResponse> searchInChatRoom(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @RequestParam Long chatRoomId,
            @RequestParam @NotBlank @Size(max = 255, message = "검색어는 255자 이하여야 합니다.") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        List<Message> messages = searchMessageUseCase.searchInChatRoom(chatRoomId, principal.getUserId(), keyword, page, size);
        List<SearchedMessageDto> messageDtos = messages.stream()
                .map(SearchedMessageDto::from)
                .toList();

        return ResponseEntity.ok(MessageSearchResponse.of(messageDtos));
    }

    /**
     * 사용자가 속한 모든 채팅방에서 키워드로 메시지를 검색합니다.
     *
     * @param principal 인증된 사용자 정보
     * @param keyword   검색 키워드
     * @param page      페이지 번호 (기본값: 0)
     * @param size      페이지 크기 (기본값: 20)
     * @return 검색된 메시지 목록
     */
    @Operation(summary = "전체 채팅방 메시지 검색", description = "사용자가 속한 모든 채팅방에서 키워드로 메시지를 검색합니다.")
    @GetMapping("/all")
    public ResponseEntity<MessageSearchResponse> searchAcrossAllChatRooms(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @RequestParam @NotBlank @Size(max = 255, message = "검색어는 255자 이하여야 합니다.") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        List<Message> messages = searchMessageUseCase.searchAcrossAllChatRooms(principal.getUserId(), keyword, page, size);
        List<SearchedMessageDto> messageDtos = messages.stream()
                .map(SearchedMessageDto::from)
                .toList();

        return ResponseEntity.ok(MessageSearchResponse.of(messageDtos));
    }

}
