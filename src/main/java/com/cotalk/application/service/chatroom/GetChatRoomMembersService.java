package com.cotalk.application.service.chatroom;

import com.cotalk.domain.entity.ChatRoomMember;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.port.inbound.chatroom.GetChatRoomMembersUseCase;
import com.cotalk.domain.port.outbound.ChatRoomMemberRepository;
import com.cotalk.domain.port.outbound.UserRepository;
import com.cotalk.domain.validator.ChatRoomMemberValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 채팅방 멤버 목록 조회 유스케이스 구현체.
 *
 * @author seunggu.lee
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetChatRoomMembersService implements GetChatRoomMembersUseCase {

    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final UserRepository userRepository;
    private final ChatRoomMemberValidator chatRoomMemberValidator;

    /**
     * 채팅방의 멤버 목록을 조회한다.
     * 관리자가 먼저 정렬되어 반환된다.
     *
     * @param chatRoomId    채팅방 ID
     * @param requestUserId 요청 사용자 ID
     * @return 멤버 정보 목록 (관리자 우선 정렬)
     * @throws com.cotalk.domain.exception.ChatRoomAccessDeniedException 채팅방 멤버가 아닌 경우
     */
    @Override
    public List<MemberInfo> getChatRoomMembers(Long chatRoomId, Long requestUserId) {
        // 요청자가 채팅방 멤버인지 검증
        chatRoomMemberValidator.getMemberOrThrow(chatRoomId, requestUserId);

        // 채팅방 멤버 조회
        List<ChatRoomMember> members = chatRoomMemberRepository.findByChatRoomId(chatRoomId);

        // 사용자 ID 목록 추출
        List<Long> userIds = members.stream()
                .map(ChatRoomMember::getUserId)
                .toList();

        // 사용자 정보 조회 및 Map으로 변환
        Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        // 멤버 정보 생성 (관리자 우선 정렬)
        return members.stream()
                .sorted(Comparator.comparing(
                        (ChatRoomMember m) -> m.getRole() == ChatRoomMember.MemberRole.ADMIN ? 0 : 1))
                .map(member -> {
                    User user = userMap.get(member.getUserId());
                    return new MemberInfo(
                            member.getUserId(),
                            user != null ? user.getNickname() : null,
                            user != null ? user.getAvatarUrl() : null,
                            member.getRole()
                    );
                })
                .toList();
    }
}
