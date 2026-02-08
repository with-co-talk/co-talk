package com.cotalk.adapter.inbound.rest;

import com.cotalk.adapter.inbound.rest.dto.chatroom.AdminResponse;
import com.cotalk.adapter.inbound.rest.dto.chatroom.AnnouncementResponse;
import com.cotalk.adapter.inbound.rest.dto.chatroom.ChatRoomDto;
import com.cotalk.adapter.inbound.rest.dto.chatroom.ChatRoomMemberDto;
import com.cotalk.adapter.inbound.rest.dto.chatroom.ChatRoomMembersResponse;
import com.cotalk.adapter.inbound.rest.dto.chatroom.ChatRoomsResponse;
import com.cotalk.adapter.inbound.rest.dto.chatroom.CreateChatRoomRequest;
import com.cotalk.adapter.inbound.rest.dto.chatroom.CreateChatRoomResponse;
import com.cotalk.adapter.inbound.rest.dto.chatroom.CreateGroupChatRoomRequest;
import com.cotalk.adapter.inbound.rest.dto.chatroom.InviteMembersRequest;
import com.cotalk.adapter.inbound.rest.dto.chatroom.ReinviteMemberRequest;
import com.cotalk.adapter.inbound.rest.dto.chatroom.SetAnnouncementRequest;
import com.cotalk.adapter.inbound.rest.dto.chatroom.UpdateChatRoomNameRequest;
import com.cotalk.adapter.inbound.rest.dto.chatroom.UpdateChatRoomNameResponse;
import com.cotalk.adapter.inbound.rest.dto.common.MessageResponse;
import com.cotalk.domain.entity.ChatRoom;
import com.cotalk.domain.entity.ChatRoomMember;
import com.cotalk.domain.entity.ChatRoomSummary;
import com.cotalk.domain.port.inbound.chatroom.ChatRoomManagementUseCase;
import com.cotalk.domain.port.inbound.chatroom.CreateChatRoomUseCase;
import com.cotalk.domain.port.inbound.chatroom.CreateGroupChatRoomUseCase;
import com.cotalk.domain.port.inbound.chatroom.GetChatRoomMembersUseCase;
import com.cotalk.domain.port.inbound.chatroom.GetChatRoomUseCase;
import com.cotalk.domain.port.inbound.chatroom.GetChatRoomsUseCase;
import com.cotalk.domain.port.inbound.chatroom.InviteGroupChatMemberUseCase;
import com.cotalk.domain.port.inbound.chatroom.KickChatRoomMemberUseCase;
import com.cotalk.domain.port.inbound.chatroom.LeaveChatRoomUseCase;
import com.cotalk.domain.port.inbound.chatroom.ReinviteDirectChatMemberUseCase;
import com.cotalk.domain.port.inbound.message.MarkAsReadUseCase;
import com.cotalk.infrastructure.security.CustomUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 채팅방 관리를 위한 REST 컨트롤러.
 * <p>
 * 1:1 채팅방 및 그룹 채팅방의 생성, 조회, 나가기, 관리 등의 기능을 제공합니다.
 *
 * @author seunggu.lee
 */
@RestController
@RequestMapping("/api/v1/chat/rooms")
@RequiredArgsConstructor
@Tag(name = "채팅방", description = "채팅방 관리 API")
public class ChatRoomController {

    private final CreateChatRoomUseCase createChatRoomUseCase;
    private final GetChatRoomsUseCase getChatRoomsUseCase;
    private final GetChatRoomUseCase getChatRoomUseCase;
    private final LeaveChatRoomUseCase leaveChatRoomUseCase;
    private final MarkAsReadUseCase markAsReadUseCase;
    private final CreateGroupChatRoomUseCase createGroupChatRoomUseCase;
    private final InviteGroupChatMemberUseCase inviteGroupChatMemberUseCase;
    private final ChatRoomManagementUseCase chatRoomManagementUseCase;
    private final GetChatRoomMembersUseCase getChatRoomMembersUseCase;
    private final KickChatRoomMemberUseCase kickChatRoomMemberUseCase;
    private final ReinviteDirectChatMemberUseCase reinviteDirectChatMemberUseCase;

    /**
     * 1:1 채팅방을 생성합니다.
     *
     * @param request 채팅방 생성 요청 (두 사용자의 ID 포함)
     * @return 생성된 채팅방 정보
     */
    @Operation(summary = "채팅방 생성", description = "1:1 채팅방을 생성합니다.")
    @PostMapping
    public ResponseEntity<CreateChatRoomResponse> createChatRoom(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody CreateChatRoomRequest request) {
        // userId1은 인증된 사용자에서 추출 (요청의 userId1은 무시)
        Long roomId = createChatRoomUseCase.createChatRoom(principal.getUserId(), request.userId2());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CreateChatRoomResponse.of(roomId, "채팅방이 생성되었습니다."));
    }

    /**
     * 사용자의 채팅방 목록을 조회합니다.
     *
     * @param principal 인증된 사용자 정보
     * @param page 페이지 번호 (기본값: 0)
     * @param size 페이지 크기 (기본값: 20, 최대: 100)
     * @return 채팅방 목록
     */
    @Operation(summary = "채팅방 목록 조회", description = "사용자의 채팅방 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<ChatRoomsResponse> getChatRooms(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int safeSize = Math.min(size, 100);
        List<ChatRoomSummary> chatRooms = getChatRoomsUseCase.getChatRooms(principal.getUserId());
        List<ChatRoomDto> roomDtos = chatRooms.stream()
                .skip((long) page * safeSize)
                .limit(safeSize)
                .map(ChatRoomDto::from)
                .toList();
        return ResponseEntity.ok(ChatRoomsResponse.of(roomDtos));
    }

    /**
     * 특정 채팅방의 상세 정보를 조회합니다.
     *
     * @param principal 인증된 사용자 정보
     * @param roomId    채팅방 ID
     * @return 채팅방 상세 정보
     */
    @Operation(summary = "채팅방 상세 조회", description = "특정 채팅방의 상세 정보를 조회합니다.")
    @GetMapping("/{roomId}")
    public ResponseEntity<ChatRoomDto> getChatRoom(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long roomId) {
        ChatRoomSummary chatRoom = getChatRoomUseCase.getChatRoom(roomId, principal.getUserId());
        return ResponseEntity.ok(ChatRoomDto.from(chatRoom));
    }

    /**
     * 채팅방 멤버 목록을 조회합니다.
     *
     * @param principal 인증된 사용자 정보
     * @param roomId    채팅방 ID
     * @param page 페이지 번호 (기본값: 0)
     * @param size 페이지 크기 (기본값: 20, 최대: 100)
     * @return 멤버 목록
     */
    @Operation(summary = "채팅방 멤버 목록 조회", description = "채팅방의 멤버 목록을 조회합니다.")
    @GetMapping("/{roomId}/members")
    public ResponseEntity<ChatRoomMembersResponse> getChatRoomMembers(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long roomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int safeSize = Math.min(size, 100);
        List<GetChatRoomMembersUseCase.MemberInfo> members =
                getChatRoomMembersUseCase.getChatRoomMembers(roomId, principal.getUserId());
        List<ChatRoomMemberDto> memberDtos = members.stream()
                .skip((long) page * safeSize)
                .limit(safeSize)
                .map(ChatRoomMemberDto::from)
                .toList();
        return ResponseEntity.ok(ChatRoomMembersResponse.of(memberDtos));
    }

    /**
     * 채팅방에서 나갑니다.
     *
     * @param principal 인증된 사용자 정보
     * @param roomId    채팅방 ID
     * @return 처리 결과 메시지
     */
    @Operation(summary = "채팅방 나가기", description = "채팅방에서 나갑니다.")
    @PostMapping("/{roomId}/leave")
    public ResponseEntity<MessageResponse> leaveChatRoom(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long roomId) {
        leaveChatRoomUseCase.leaveChatRoom(roomId, principal.getUserId());
        return ResponseEntity.ok(MessageResponse.of("채팅방을 나갔습니다."));
    }

    /**
     * 채팅방 메시지를 읽음 처리합니다.
     *
     * @param principal 인증된 사용자 정보
     * @param roomId    채팅방 ID
     * @return 처리 결과 메시지
     */
    @Operation(summary = "읽음 표시", description = "채팅방 메시지를 읽음 처리합니다.")
    @PostMapping("/{roomId}/read")
    public ResponseEntity<MessageResponse> markAsRead(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long roomId) {
        markAsReadUseCase.markAsRead(principal.getUserId(), roomId);
        return ResponseEntity.ok(MessageResponse.of("읽음 처리되었습니다."));
    }

    /**
     * 그룹 채팅방을 생성합니다.
     *
     * @param request 그룹 채팅방 생성 요청 (생성자 ID, 방 이름, 멤버 목록)
     * @return 생성된 채팅방 정보
     */
    @Operation(summary = "그룹 채팅방 생성", description = "그룹 채팅방을 생성합니다.")
    @PostMapping("/group")
    public ResponseEntity<CreateChatRoomResponse> createGroupChatRoom(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody CreateGroupChatRoomRequest request) {
        Long roomId = createGroupChatRoomUseCase.createGroupChatRoom(
                principal.getUserId(), request.roomName(), request.memberIds());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CreateChatRoomResponse.of(roomId, "그룹 채팅방이 생성되었습니다."));
    }

    /**
     * 그룹 채팅방에 멤버를 초대합니다.
     *
     * @param roomId  채팅방 ID
     * @param request 멤버 초대 요청 (초대자 ID, 초대할 멤버 ID 목록)
     * @return 처리 결과 메시지
     */
    @Operation(summary = "그룹 채팅방 멤버 초대", description = "그룹 채팅방에 멤버를 초대합니다.")
    @PostMapping("/{roomId}/invite")
    public ResponseEntity<MessageResponse> inviteMembers(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long roomId,
            @Valid @RequestBody InviteMembersRequest request) {
        inviteGroupChatMemberUseCase.inviteMembers(roomId, principal.getUserId(), request.inviteeIds());
        return ResponseEntity.ok(MessageResponse.of("멤버를 초대했습니다."));
    }

    /**
     * 그룹 채팅방의 이름을 변경합니다. (관리자 권한 필요)
     *
     * @param roomId  채팅방 ID
     * @param request 이름 변경 요청 (사용자 ID, 새 이름)
     * @return 변경된 채팅방 이름 정보
     */
    @Operation(summary = "채팅방 이름 변경", description = "그룹 채팅방의 이름을 변경합니다. (관리자 권한 필요)")
    @PutMapping("/{roomId}/name")
    public ResponseEntity<UpdateChatRoomNameResponse> updateChatRoomName(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long roomId,
            @Valid @RequestBody UpdateChatRoomNameRequest request) {
        ChatRoom chatRoom = chatRoomManagementUseCase.updateChatRoomName(roomId, principal.getUserId(), request.newName());
        return ResponseEntity.ok(UpdateChatRoomNameResponse.of(chatRoom.getName(), "채팅방 이름이 변경되었습니다."));
    }

    /**
     * 채팅방 공지사항을 설정합니다. (관리자 권한 필요)
     *
     * @param roomId  채팅방 ID
     * @param request 공지사항 설정 요청 (사용자 ID, 공지 내용)
     * @return 설정된 공지사항 정보
     */
    @Operation(summary = "공지사항 설정", description = "채팅방 공지사항을 설정합니다. (관리자 권한 필요)")
    @PostMapping("/{roomId}/announcement")
    public ResponseEntity<AnnouncementResponse> setAnnouncement(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long roomId,
            @Valid @RequestBody SetAnnouncementRequest request) {
        ChatRoom chatRoom = chatRoomManagementUseCase.setAnnouncement(roomId, principal.getUserId(), request.announcement());
        return ResponseEntity.ok(AnnouncementResponse.of(chatRoom.getAnnouncement(), "공지사항이 설정되었습니다."));
    }

    /**
     * 채팅방 공지사항을 삭제합니다. (관리자 권한 필요)
     *
     * @param principal 인증된 사용자 정보
     * @param roomId    채팅방 ID
     * @return 처리 결과 메시지
     */
    @Operation(summary = "공지사항 삭제", description = "채팅방 공지사항을 삭제합니다. (관리자 권한 필요)")
    @DeleteMapping("/{roomId}/announcement")
    public ResponseEntity<MessageResponse> clearAnnouncement(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long roomId) {
        chatRoomManagementUseCase.clearAnnouncement(roomId, principal.getUserId());
        return ResponseEntity.ok(MessageResponse.of("공지사항이 삭제되었습니다."));
    }

    /**
     * 멤버를 관리자로 임명합니다. (관리자 권한 필요)
     *
     * @param principal    인증된 사용자 정보
     * @param roomId       채팅방 ID
     * @param targetUserId 관리자로 임명할 사용자 ID
     * @return 임명된 관리자 정보
     */
    @Operation(summary = "관리자 임명", description = "멤버를 관리자로 임명합니다. (관리자 권한 필요)")
    @PostMapping("/{roomId}/admins/{targetUserId}")
    public ResponseEntity<AdminResponse> promoteToAdmin(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long roomId,
            @PathVariable Long targetUserId) {
        ChatRoomMember member = chatRoomManagementUseCase.promoteToAdmin(roomId, principal.getUserId(), targetUserId);
        return ResponseEntity.ok(AdminResponse.from(member, "관리자로 임명되었습니다."));
    }

    /**
     * 관리자 권한을 해제합니다. (관리자 권한 필요)
     *
     * @param principal    인증된 사용자 정보
     * @param roomId       채팅방 ID
     * @param targetUserId 권한 해제할 사용자 ID
     * @return 권한 해제된 멤버 정보
     */
    @Operation(summary = "관리자 해제", description = "관리자 권한을 해제합니다. (관리자 권한 필요)")
    @DeleteMapping("/{roomId}/admins/{targetUserId}")
    public ResponseEntity<AdminResponse> demoteFromAdmin(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long roomId,
            @PathVariable Long targetUserId) {
        ChatRoomMember member = chatRoomManagementUseCase.demoteFromAdmin(roomId, principal.getUserId(), targetUserId);
        return ResponseEntity.ok(AdminResponse.from(member, "관리자 권한이 해제되었습니다."));
    }

    /**
     * 채팅방에서 멤버를 강제 퇴장시킵니다. (관리자 권한 필요)
     *
     * @param principal    인증된 사용자 정보
     * @param roomId       채팅방 ID
     * @param targetUserId 강제 퇴장시킬 사용자 ID
     * @return 처리 결과 메시지
     */
    @Operation(summary = "멤버 강제 퇴장", description = "채팅방에서 멤버를 강제 퇴장시킵니다. (관리자 권한 필요)")
    @DeleteMapping("/{roomId}/members/{targetUserId}")
    public ResponseEntity<MessageResponse> kickMember(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long roomId,
            @PathVariable Long targetUserId) {
        kickChatRoomMemberUseCase.kickMember(roomId, principal.getUserId(), targetUserId);
        return ResponseEntity.ok(MessageResponse.of("멤버가 강제 퇴장되었습니다."));
    }

    /**
     * 1:1 채팅방에서 나간 상대방을 재초대합니다.
     *
     * @param principal 인증된 사용자 정보
     * @param roomId    채팅방 ID
     * @param request   재초대 요청 (재초대할 멤버 ID)
     * @return 처리 결과 메시지
     */
    @Operation(summary = "1:1 채팅방 재초대", description = "1:1 채팅방에서 나간 상대방을 재초대합니다.")
    @PostMapping("/{roomId}/reinvite")
    public ResponseEntity<MessageResponse> reinviteMember(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long roomId,
            @Valid @RequestBody ReinviteMemberRequest request) {
        reinviteDirectChatMemberUseCase.reinviteMember(roomId, principal.getUserId(), request.inviteeId());
        return ResponseEntity.ok(MessageResponse.of("상대방을 다시 초대했습니다."));
    }

}
