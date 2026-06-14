package com.cotalk.adapter.inbound.rest;

import com.cotalk.adapter.inbound.rest.dto.chatroom.ChatRoomDto;
import com.cotalk.adapter.inbound.rest.dto.chatroom.ChatRoomMemberDto;
import com.cotalk.adapter.inbound.rest.dto.chatroom.ChatRoomMembersResponse;
import com.cotalk.adapter.inbound.rest.dto.chatroom.ChatRoomsResponse;
import com.cotalk.adapter.inbound.rest.dto.chatroom.CreateChatRoomRequest;
import com.cotalk.adapter.inbound.rest.dto.chatroom.CreateChatRoomResponse;
import com.cotalk.adapter.inbound.rest.dto.chatroom.ReinviteMemberRequest;
import com.cotalk.adapter.inbound.rest.dto.common.MessageResponse;
import com.cotalk.domain.entity.ChatRoomSummary;
import com.cotalk.domain.port.inbound.chatroom.CreateChatRoomUseCase;
import com.cotalk.domain.port.inbound.chatroom.GetChatRoomMembersUseCase;
import com.cotalk.domain.port.inbound.chatroom.GetChatRoomUseCase;
import com.cotalk.domain.port.inbound.chatroom.GetChatRoomsUseCase;
import com.cotalk.domain.port.inbound.chatroom.LeaveChatRoomUseCase;
import com.cotalk.domain.port.inbound.chatroom.ReinviteDirectChatMemberUseCase;
import com.cotalk.domain.port.inbound.message.MarkAsReadUseCase;
import com.cotalk.infrastructure.security.CustomUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 채팅방 관리를 위한 REST 컨트롤러.
 * <p>
 * 1:1 채팅방의 생성, 조회, 나가기, 재초대 등의 기능을 제공합니다.
 * 그룹 채팅방 관련 기능은 {@link GroupChatRoomController}를 참조하세요.
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
    private final GetChatRoomMembersUseCase getChatRoomMembersUseCase;
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
     * DB 레벨 페이지네이션을 사용하여 대규모 데이터에서도 효율적으로 동작합니다.
     *
     * @param principal 인증된 사용자 정보
     * @param page 페이지 번호 (기본값: 0)
     * @param size 페이지 크기 (기본값: 20, 최대: 100)
     * @return 페이지네이션된 채팅방 목록
     */
    @Operation(summary = "채팅방 목록 조회", description = "사용자의 채팅방 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<ChatRoomsResponse> getChatRooms(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Page<ChatRoomSummary> chatRoomPage = getChatRoomsUseCase.getChatRooms(
                principal.getUserId(), PageRequest.of(safePage, safeSize));
        List<ChatRoomDto> roomDtos = chatRoomPage.getContent().stream()
                .map(ChatRoomDto::from)
                .toList();
        return ResponseEntity.ok(ChatRoomsResponse.of(roomDtos, chatRoomPage));
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
     * <p>채팅방 멤버 수는 유한하고 응답 DTO에 페이지 메타데이터(total/page/hasNext)가
     * 없으므로, 관리자 우선 정렬을 유지한 전체 멤버 목록을 단건으로 반환합니다.
     * (이전의 page/size 파라미터는 전체 로딩 후 인메모리 컷이라 실제 DB 페이지네이션이
     * 아니었고, 응답에 페이지 메타데이터도 없어 클라이언트가 활용할 수 없었으므로 제거.)</p>
     *
     * @param principal 인증된 사용자 정보
     * @param roomId    채팅방 ID
     * @return 멤버 목록 (관리자 우선 정렬)
     */
    @Operation(summary = "채팅방 멤버 목록 조회", description = "채팅방의 전체 멤버 목록을 관리자 우선 정렬로 조회합니다.")
    @GetMapping("/{roomId}/members")
    public ResponseEntity<ChatRoomMembersResponse> getChatRoomMembers(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long roomId) {
        List<GetChatRoomMembersUseCase.MemberInfo> members =
                getChatRoomMembersUseCase.getChatRoomMembers(roomId, principal.getUserId());
        List<ChatRoomMemberDto> memberDtos = members.stream()
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
