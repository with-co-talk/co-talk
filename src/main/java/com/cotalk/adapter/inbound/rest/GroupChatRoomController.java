package com.cotalk.adapter.inbound.rest;

import com.cotalk.adapter.inbound.rest.dto.chatroom.AdminResponse;
import com.cotalk.adapter.inbound.rest.dto.chatroom.AnnouncementResponse;
import com.cotalk.adapter.inbound.rest.dto.chatroom.CreateChatRoomResponse;
import com.cotalk.adapter.inbound.rest.dto.chatroom.CreateGroupChatRoomRequest;
import com.cotalk.adapter.inbound.rest.dto.chatroom.InviteMembersRequest;
import com.cotalk.adapter.inbound.rest.dto.chatroom.SetAnnouncementRequest;
import com.cotalk.adapter.inbound.rest.dto.chatroom.UpdateChatRoomImageRequest;
import com.cotalk.adapter.inbound.rest.dto.chatroom.UpdateChatRoomImageResponse;
import com.cotalk.adapter.inbound.rest.dto.chatroom.UpdateChatRoomNameRequest;
import com.cotalk.adapter.inbound.rest.dto.chatroom.UpdateChatRoomNameResponse;
import com.cotalk.adapter.inbound.rest.dto.common.MessageResponse;
import com.cotalk.domain.entity.ChatRoom;
import com.cotalk.domain.entity.ChatRoomMember;
import com.cotalk.domain.port.inbound.chatroom.ChatRoomManagementUseCase;
import com.cotalk.domain.port.inbound.chatroom.CreateGroupChatRoomUseCase;
import com.cotalk.domain.port.inbound.chatroom.InviteGroupChatMemberUseCase;
import com.cotalk.domain.port.inbound.chatroom.KickChatRoomMemberUseCase;
import com.cotalk.infrastructure.security.CustomUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 그룹 채팅방 관리를 위한 REST 컨트롤러.
 * <p>
 * 그룹 채팅방의 생성, 멤버 초대, 이름/이미지 변경, 공지사항 관리, 관리자 권한 관리, 멤버 강제 퇴장 등의 기능을 제공합니다.
 *
 * @author seunggu.lee
 */
@RestController
@RequestMapping("/api/v1/chat/rooms")
@RequiredArgsConstructor
@Tag(name = "그룹 채팅방", description = "그룹 채팅방 관리 API")
public class GroupChatRoomController {

    private final CreateGroupChatRoomUseCase createGroupChatRoomUseCase;
    private final InviteGroupChatMemberUseCase inviteGroupChatMemberUseCase;
    private final ChatRoomManagementUseCase chatRoomManagementUseCase;
    private final KickChatRoomMemberUseCase kickChatRoomMemberUseCase;

    /**
     * 그룹 채팅방을 생성합니다.
     *
     * @param principal 인증된 사용자 정보
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
     * @param principal 인증된 사용자 정보
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
     * @param principal 인증된 사용자 정보
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
     * 그룹 채팅방의 이미지를 변경합니다. (관리자 권한 필요)
     *
     * @param principal 인증된 사용자 정보
     * @param roomId  채팅방 ID
     * @param request 이미지 변경 요청 (이미지 URL)
     * @return 변경된 이미지 URL 정보
     */
    @Operation(summary = "채팅방 이미지 변경", description = "그룹 채팅방의 이미지를 변경합니다. (관리자 권한 필요)")
    @PutMapping("/{roomId}/image")
    public ResponseEntity<UpdateChatRoomImageResponse> updateChatRoomImage(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long roomId,
            @Valid @RequestBody UpdateChatRoomImageRequest request) {
        ChatRoom chatRoom = chatRoomManagementUseCase.updateChatRoomImage(
                roomId, principal.getUserId(), request.imageUrl());
        return ResponseEntity.ok(UpdateChatRoomImageResponse.of(
                chatRoom.getImageUrl(), "채팅방 이미지가 변경되었습니다."));
    }

    /**
     * 채팅방 공지사항을 설정합니다. (관리자 권한 필요)
     *
     * @param principal 인증된 사용자 정보
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

}
