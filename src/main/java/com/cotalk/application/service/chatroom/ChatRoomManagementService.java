package com.cotalk.application.service.chatroom;

import com.cotalk.domain.entity.ChatRoom;
import com.cotalk.domain.entity.ChatRoomMember;
import com.cotalk.domain.exception.ChatRoomAccessDeniedException;
import com.cotalk.domain.exception.ChatRoomNotFoundException;
import com.cotalk.domain.exception.InvalidChatRoomException;
import com.cotalk.domain.exception.UserNotFoundException;
import com.cotalk.domain.port.inbound.chatroom.ChatRoomManagementUseCase;
import com.cotalk.domain.port.outbound.ChatRoomMemberRepository;
import com.cotalk.domain.port.outbound.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 채팅방 관리 유스케이스 구현체.
 * 채팅방 이름 변경, 공지사항 설정, 관리자 권한 관리 등의 기능을 제공한다.
 *
 * @author seunggu.lee
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ChatRoomManagementService implements ChatRoomManagementUseCase {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;

    /**
     * 채팅방 이름을 변경한다.
     * 1:1 채팅방은 이름 변경이 불가하며, 관리자만 변경할 수 있다.
     *
     * @param chatRoomId 채팅방 ID
     * @param userId 요청 사용자 ID
     * @param newName 새로운 채팅방 이름
     * @return 업데이트된 채팅방 정보
     * @throws InvalidChatRoomException 1:1 채팅방인 경우
     * @throws ChatRoomAccessDeniedException 관리자 권한이 없는 경우
     */
    @Override
    public ChatRoom updateChatRoomName(Long chatRoomId, Long userId, String newName) {
        ChatRoom chatRoom = getChatRoom(chatRoomId);

        if (chatRoom.isDirectChat()) {
            throw new InvalidChatRoomException("1:1 채팅방은 이름을 변경할 수 없습니다.");
        }

        validateAdminPermission(chatRoomId, userId);

        chatRoom.updateName(newName);
        return chatRoomRepository.save(chatRoom);
    }

    /**
     * 채팅방 공지사항을 설정한다.
     * 관리자만 공지사항을 설정할 수 있다.
     *
     * @param chatRoomId 채팅방 ID
     * @param userId 요청 사용자 ID
     * @param announcement 공지사항 내용
     * @return 업데이트된 채팅방 정보
     * @throws ChatRoomAccessDeniedException 관리자 권한이 없는 경우
     */
    @Override
    public ChatRoom setAnnouncement(Long chatRoomId, Long userId, String announcement) {
        ChatRoom chatRoom = getChatRoom(chatRoomId);
        validateAdminPermission(chatRoomId, userId);

        chatRoom.setAnnouncement(announcement);
        return chatRoomRepository.save(chatRoom);
    }

    /**
     * 채팅방 공지사항을 삭제한다.
     * 관리자만 공지사항을 삭제할 수 있다.
     *
     * @param chatRoomId 채팅방 ID
     * @param userId 요청 사용자 ID
     * @return 업데이트된 채팅방 정보
     * @throws ChatRoomAccessDeniedException 관리자 권한이 없는 경우
     */
    @Override
    public ChatRoom clearAnnouncement(Long chatRoomId, Long userId) {
        ChatRoom chatRoom = getChatRoom(chatRoomId);
        validateAdminPermission(chatRoomId, userId);

        chatRoom.clearAnnouncement();
        return chatRoomRepository.save(chatRoom);
    }

    /**
     * 채팅방 멤버를 관리자로 승격시킨다.
     * 기존 관리자만 다른 멤버를 관리자로 승격시킬 수 있다.
     *
     * @param chatRoomId 채팅방 ID
     * @param adminUserId 요청 관리자 사용자 ID
     * @param targetUserId 승격시킬 대상 사용자 ID
     * @return 업데이트된 멤버 정보
     * @throws ChatRoomAccessDeniedException 관리자 권한이 없는 경우
     * @throws UserNotFoundException 대상 사용자가 채팅방 멤버가 아닌 경우
     */
    @Override
    public ChatRoomMember promoteToAdmin(Long chatRoomId, Long adminUserId, Long targetUserId) {
        getChatRoom(chatRoomId);
        validateAdminPermission(chatRoomId, adminUserId);

        ChatRoomMember targetMember = getMember(chatRoomId, targetUserId);
        targetMember.promoteToAdmin();
        return chatRoomMemberRepository.save(targetMember);
    }

    /**
     * 채팅방 관리자를 일반 멤버로 강등시킨다.
     * 기존 관리자만 다른 관리자를 일반 멤버로 강등시킬 수 있다.
     *
     * @param chatRoomId 채팅방 ID
     * @param adminUserId 요청 관리자 사용자 ID
     * @param targetUserId 강등시킬 대상 사용자 ID
     * @return 업데이트된 멤버 정보
     * @throws ChatRoomAccessDeniedException 관리자 권한이 없는 경우
     * @throws UserNotFoundException 대상 사용자가 채팅방 멤버가 아닌 경우
     */
    @Override
    public ChatRoomMember demoteFromAdmin(Long chatRoomId, Long adminUserId, Long targetUserId) {
        getChatRoom(chatRoomId);
        validateAdminPermission(chatRoomId, adminUserId);

        ChatRoomMember targetMember = getMember(chatRoomId, targetUserId);
        targetMember.demoteToMember();
        return chatRoomMemberRepository.save(targetMember);
    }

    private ChatRoom getChatRoom(Long chatRoomId) {
        return chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new ChatRoomNotFoundException(chatRoomId));
    }

    private ChatRoomMember getMember(Long chatRoomId, Long userId) {
        return chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    private void validateAdminPermission(Long chatRoomId, Long userId) {
        ChatRoomMember member = getMember(chatRoomId, userId);
        if (!member.isAdmin()) {
            throw new ChatRoomAccessDeniedException(userId, chatRoomId);
        }
    }
}
