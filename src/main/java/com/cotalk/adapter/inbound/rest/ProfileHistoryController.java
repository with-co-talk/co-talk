package com.cotalk.adapter.inbound.rest;

import com.cotalk.adapter.inbound.rest.dto.common.MessageResponse;
import com.cotalk.adapter.inbound.rest.dto.profile.CreateProfileHistoryRequest;
import com.cotalk.adapter.inbound.rest.dto.profile.ProfileHistoryDto;
import com.cotalk.adapter.inbound.rest.dto.profile.ProfileHistoryListResponse;
import com.cotalk.adapter.inbound.rest.dto.profile.UpdateProfileHistoryRequest;
import com.cotalk.domain.entity.ProfileHistory;
import com.cotalk.domain.entity.ProfileHistoryType;
import com.cotalk.domain.exception.ResourceAccessDeniedException;
import com.cotalk.domain.port.inbound.profile.CreateProfileHistoryUseCase;
import com.cotalk.domain.port.inbound.profile.DeleteProfileHistoryUseCase;
import com.cotalk.domain.port.inbound.profile.GetProfileHistoryUseCase;
import com.cotalk.domain.port.inbound.profile.SetCurrentProfileUseCase;
import com.cotalk.domain.port.inbound.profile.UpdateProfileHistoryUseCase;
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
 * 프로필 이력 관리를 위한 REST 컨트롤러.
 * 프로필 사진, 배경화면, 상태메시지 이력을 조회, 생성, 수정, 삭제하는 기능을 제공한다.
 *
 * @author seunggu.lee
 */
@RestController
@RequestMapping("/api/v1/users/{userId}/profile/history")
@RequiredArgsConstructor
@Tag(name = "프로필 이력", description = "프로필 이력 관리 API")
public class ProfileHistoryController {

    private final GetProfileHistoryUseCase getProfileHistoryUseCase;
    private final CreateProfileHistoryUseCase createProfileHistoryUseCase;
    private final UpdateProfileHistoryUseCase updateProfileHistoryUseCase;
    private final DeleteProfileHistoryUseCase deleteProfileHistoryUseCase;
    private final SetCurrentProfileUseCase setCurrentProfileUseCase;

    /**
     * 프로필 이력 목록을 조회한다.
     * 타인의 프로필 조회 시 비공개 이력은 제외된다.
     *
     * @param principal 인증된 사용자 정보
     * @param userId    조회할 사용자 ID
     * @param type      이력 유형 필터 (선택)
     * @return 프로필 이력 목록
     */
    @Operation(summary = "프로필 이력 조회", description = "사용자의 프로필 이력을 조회합니다. 타인 조회 시 비공개 이력은 제외됩니다.")
    @GetMapping
    public ResponseEntity<ProfileHistoryListResponse> getProfileHistory(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long userId,
            @RequestParam(required = false) ProfileHistoryType type) {

        List<ProfileHistory> histories = getProfileHistoryUseCase.getProfileHistory(
                userId, type, principal.getUserId());

        List<ProfileHistoryDto> dtos = histories.stream()
                .map(ProfileHistoryDto::from)
                .toList();

        return ResponseEntity.ok(ProfileHistoryListResponse.of(dtos));
    }

    /**
     * 새로운 프로필 이력을 생성한다.
     *
     * @param principal 인증된 사용자 정보
     * @param userId    사용자 ID
     * @param request   생성 요청 정보
     * @return 생성된 프로필 이력
     */
    @Operation(summary = "프로필 이력 생성", description = "새로운 프로필 이력을 생성합니다.")
    @PostMapping
    public ResponseEntity<ProfileHistoryDto> createProfileHistory(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long userId,
            @Valid @RequestBody CreateProfileHistoryRequest request) {

        validateUserAccess(principal.getUserId(), userId);

        ProfileHistory created = createProfileHistoryUseCase.createProfileHistory(
                userId,
                request.type(),
                request.url(),
                request.content(),
                request.isPrivate(),
                request.setCurrent()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ProfileHistoryDto.from(created));
    }

    /**
     * 프로필 이력의 나만보기 설정을 변경한다.
     *
     * @param principal 인증된 사용자 정보
     * @param userId    사용자 ID
     * @param historyId 프로필 이력 ID
     * @param request   수정 요청 정보
     * @return 수정 완료 메시지
     */
    @Operation(summary = "프로필 이력 수정", description = "프로필 이력의 나만보기 설정을 변경합니다.")
    @PutMapping("/{historyId}")
    public ResponseEntity<MessageResponse> updateProfileHistory(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long userId,
            @PathVariable Long historyId,
            @Valid @RequestBody UpdateProfileHistoryRequest request) {

        validateUserAccess(principal.getUserId(), userId);

        updateProfileHistoryUseCase.updatePrivacy(historyId, userId, request.isPrivate());

        return ResponseEntity.ok(MessageResponse.of("프로필 이력이 수정되었습니다."));
    }

    /**
     * 프로필 이력을 삭제한다.
     *
     * @param principal 인증된 사용자 정보
     * @param userId    사용자 ID
     * @param historyId 프로필 이력 ID
     * @return 삭제 완료 메시지
     */
    @Operation(summary = "프로필 이력 삭제", description = "프로필 이력을 삭제합니다.")
    @DeleteMapping("/{historyId}")
    public ResponseEntity<MessageResponse> deleteProfileHistory(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long userId,
            @PathVariable Long historyId) {

        validateUserAccess(principal.getUserId(), userId);

        deleteProfileHistoryUseCase.deleteProfileHistory(historyId, userId);

        return ResponseEntity.ok(MessageResponse.of("프로필 이력이 삭제되었습니다."));
    }

    /**
     * 특정 프로필 이력을 현재 프로필로 설정한다.
     *
     * @param principal 인증된 사용자 정보
     * @param userId    사용자 ID
     * @param historyId 프로필 이력 ID
     * @return 설정 완료 메시지
     */
    @Operation(summary = "현재 프로필로 설정", description = "특정 프로필 이력을 현재 프로필로 설정합니다.")
    @PutMapping("/{historyId}/current")
    public ResponseEntity<MessageResponse> setCurrentProfile(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long userId,
            @PathVariable Long historyId) {

        validateUserAccess(principal.getUserId(), userId);

        setCurrentProfileUseCase.setCurrentProfile(historyId, userId);

        return ResponseEntity.ok(MessageResponse.of("현재 프로필로 설정되었습니다."));
    }

    /**
     * 요청된 userId가 현재 인증된 사용자의 ID와 일치하는지 검증합니다.
     *
     * @param currentUserId 현재 인증된 사용자 ID
     * @param userId        검증할 사용자 ID
     * @throws ResourceAccessDeniedException 사용자 ID가 일치하지 않는 경우
     */
    private void validateUserAccess(Long currentUserId, Long userId) {
        if (!currentUserId.equals(userId)) {
            throw new ResourceAccessDeniedException();
        }
    }
}
