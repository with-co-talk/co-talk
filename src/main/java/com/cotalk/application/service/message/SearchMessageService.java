package com.cotalk.application.service.message;

import com.cotalk.domain.entity.Message;
import com.cotalk.domain.port.inbound.message.SearchMessageUseCase;
import com.cotalk.domain.port.outbound.BlindIndexTokenizer;
import com.cotalk.domain.port.outbound.ChatRoomMemberRepository;
import com.cotalk.domain.port.outbound.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 메시지 검색 유스케이스 구현체.
 *
 * <p>메시지 본문이 AES-GCM(랜덤 IV)으로 암호화되어 DB LIKE 검색이 불가능하므로,
 * 블라인드 인덱스(HMAC 트라이그램 토큰) 기반 2단계 검색을 수행한다.</p>
 *
 * <ol>
 *   <li><b>1단계(DB)</b>: 키워드를 동일 파이프라인으로 토큰화 → "모든 토큰을 포함하는 메시지"를
 *       토큰 조인 쿼리로 조회. 토큰 AND 매칭은 substring을 보장하지 않으므로 over-fetch한다.</li>
 *   <li><b>2단계(애플리케이션)</b>: JPA가 {@code @Convert}로 이미 복호화한 본문을 동일 규칙으로
 *       정규화하여 {@code contains}로 substring을 최종 검증한다(false positive 제거 — 정확도의 진실).</li>
 * </ol>
 *
 * <p>1~2글자 키워드는 트라이그램이 나오지 않아 블라인드 인덱스로 검색할 수 없으므로
 * 명시적으로 거부한다(3글자 이상 안내).</p>
 *
 * @author seunggu.lee
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchMessageService implements SearchMessageUseCase {

    /** 블라인드 인덱스 트라이그램에 필요한 최소 키워드 길이(코드포인트). */
    private static final int MIN_KEYWORD_LENGTH = 3;

    /**
     * over-fetch 배수.
     * 2단계 복호화 substring 검증에서 행이 줄어들 수 있으므로 1단계에서 size보다 넉넉히 조회한다.
     * 정확 페이지 크기는 보장하지 않는다(approximate; PR 본문에 명시).
     */
    private static final int OVER_FETCH_MULTIPLIER = 3;

    private final MessageRepository messageRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final BlindIndexTokenizer blindIndexTokenizer;

    /**
     * 특정 채팅방 내에서 메시지를 검색한다. 채팅방 멤버만 검색할 수 있다.
     *
     * @param chatRoomId 채팅방 ID
     * @param userId 요청 사용자 ID
     * @param keyword 검색 키워드
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 검색된 메시지 목록 (최신순)
     * @throws IllegalArgumentException 채팅방 멤버가 아니거나 키워드가 3글자 미만인 경우
     */
    @Override
    public List<Message> searchInChatRoom(Long chatRoomId, Long userId, String keyword, int page, int size) {
        if (!chatRoomMemberRepository.existsByChatRoomIdAndUserId(chatRoomId, userId)) {
            throw new IllegalArgumentException("채팅방 멤버만 메시지를 검색할 수 있습니다.");
        }

        if (isBlank(keyword)) {
            return Collections.emptyList();
        }

        String trimmed = keyword.trim();
        validateKeywordLength(trimmed);

        int clampedSize = clampSize(size);
        Set<String> tokens = blindIndexTokenizer.tokenizeQuery(trimmed);
        if (tokens.isEmpty()) {
            return Collections.emptyList();
        }

        List<Message> candidates = messageRepository.searchByTokensInChatRoom(
                chatRoomId, List.copyOf(tokens), tokens.size(), page, fetchSize(clampedSize));
        return finalFilter(candidates, trimmed, clampedSize);
    }

    /**
     * 사용자가 참여한 모든 채팅방에서 메시지를 검색한다.
     *
     * @param userId 사용자 ID
     * @param keyword 검색 키워드
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 검색된 메시지 목록 (최신순)
     * @throws IllegalArgumentException 키워드가 3글자 미만인 경우
     */
    @Override
    public List<Message> searchAcrossAllChatRooms(Long userId, String keyword, int page, int size) {
        if (isBlank(keyword)) {
            return Collections.emptyList();
        }

        String trimmed = keyword.trim();
        validateKeywordLength(trimmed);

        int clampedSize = clampSize(size);
        Set<String> tokens = blindIndexTokenizer.tokenizeQuery(trimmed);
        if (tokens.isEmpty()) {
            return Collections.emptyList();
        }

        List<Message> candidates = messageRepository.searchByTokensInUserChatRooms(
                userId, List.copyOf(tokens), tokens.size(), page, fetchSize(clampedSize));
        return finalFilter(candidates, trimmed, clampedSize);
    }

    /**
     * 2단계 최종 필터: 삭제 제외 + 복호화 본문 substring 검증 + size 컷.
     * 토큰화와 동일한 정규화를 키워드/본문에 적용해 트라이그램 흩어짐 오탐을 제거한다.
     *
     * @param candidates 1단계 토큰 조인 결과 (복호화된 본문 보유)
     * @param keyword 검색 키워드(trim됨)
     * @param size 최종 반환 크기
     * @return substring을 실제로 포함하는 메시지 목록 (최대 size개)
     */
    private List<Message> finalFilter(List<Message> candidates, String keyword, int size) {
        String normalizedKeyword = blindIndexTokenizer.normalize(keyword);
        return candidates.stream()
                .filter(m -> !m.isDeleted())
                .filter(m -> blindIndexTokenizer.normalize(m.getContent()).contains(normalizedKeyword))
                .limit(size)
                .toList();
    }

    private boolean isBlank(String keyword) {
        return keyword == null || keyword.trim().isEmpty();
    }

    private void validateKeywordLength(String trimmed) {
        if (trimmed.codePointCount(0, trimmed.length()) < MIN_KEYWORD_LENGTH) {
            throw new IllegalArgumentException("검색어는 3글자 이상 입력해야 합니다.");
        }
    }

    private int clampSize(int size) {
        return Math.min(Math.max(size, 1), 100);
    }

    private int fetchSize(int clampedSize) {
        return Math.min(clampedSize * OVER_FETCH_MULTIPLIER, 300);
    }
}
