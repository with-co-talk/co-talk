package com.cotalk.application.service.message;

import com.cotalk.domain.entity.Message;
import com.cotalk.domain.port.inbound.message.SearchMessageUseCase;
import com.cotalk.domain.port.outbound.ChatRoomMemberRepository;
import com.cotalk.domain.port.outbound.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

/**
 * 메시지 검색 유스케이스 구현체.
 * 채팅방 내 또는 전체 채팅방에서 메시지를 검색한다.
 *
 * @author seunggu.lee
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchMessageService implements SearchMessageUseCase {

    private final MessageRepository messageRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;

    /**
     * 특정 채팅방 내에서 메시지를 검색한다.
     * 채팅방 멤버만 검색할 수 있다.
     *
     * @param chatRoomId 채팅방 ID
     * @param userId 요청 사용자 ID
     * @param keyword 검색 키워드
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 검색된 메시지 목록
     * @throws IllegalArgumentException 채팅방 멤버가 아닌 경우
     */
    @Override
    public List<Message> searchInChatRoom(Long chatRoomId, Long userId, String keyword, int page, int size) {
        if (!chatRoomMemberRepository.existsByChatRoomIdAndUserId(chatRoomId, userId)) {
            throw new IllegalArgumentException("채팅방 멤버만 메시지를 검색할 수 있습니다.");
        }

        if (keyword == null || keyword.trim().isEmpty()) {
            return Collections.emptyList();
        }

        int clampedSize = Math.min(Math.max(size, 1), 100);
        return messageRepository.searchByKeywordInChatRoom(chatRoomId, keyword.trim(), page, clampedSize)
                .stream().filter(m -> !m.isDeleted()).toList();
    }

    /**
     * 사용자가 참여한 모든 채팅방에서 메시지를 검색한다.
     *
     * @param userId 사용자 ID
     * @param keyword 검색 키워드
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 검색된 메시지 목록
     */
    @Override
    public List<Message> searchAcrossAllChatRooms(Long userId, String keyword, int page, int size) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return Collections.emptyList();
        }

        int clampedSize = Math.min(Math.max(size, 1), 100);
        return messageRepository.searchByKeywordInUserChatRooms(userId, keyword.trim(), page, clampedSize)
                .stream().filter(m -> !m.isDeleted()).toList();
    }
}
