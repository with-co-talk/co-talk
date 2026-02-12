package com.cotalk.domain.port.outbound;

import com.cotalk.domain.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * 사용자 레포지토리 포트.
 * 사용자 데이터 저장 및 조회를 위한 인터페이스를 정의한다.
 *
 * @author seunggu.lee
 */
public interface UserRepository {

    /**
     * 사용자를 저장한다.
     *
     * @param user 저장할 사용자
     * @return 저장된 사용자
     */
    User save(User user);

    /**
     * ID로 사용자를 조회한다.
     *
     * @param id 사용자 ID
     * @return 조회된 사용자 (Optional)
     */
    Optional<User> findById(Long id);

    /**
     * 이메일로 사용자를 조회한다.
     *
     * @param email 이메일 주소
     * @return 조회된 사용자 (Optional)
     */
    Optional<User> findByEmail(String email);

    /**
     * OAuth 제공자와 OAuth ID로 사용자를 조회한다.
     *
     * @param provider OAuth 제공자 (GOOGLE, KAKAO 등)
     * @param oauthId  OAuth ID
     * @return 조회된 사용자 (Optional)
     */
    Optional<User> findByOAuthProviderAndOAuthId(User.OAuthProvider provider, String oauthId);

    /**
     * 닉네임에 특정 문자열이 포함된 사용자 목록을 조회한다.
     *
     * @param nickname 검색할 닉네임 문자열
     * @return 닉네임이 일치하는 사용자 목록
     */
    List<User> findByNicknameContaining(String nickname);

    /**
     * 닉네임에 특정 문자열이 포함된 사용자 목록을 조회한다. (DB-레벨 limit 적용)
     *
     * @param nickname 검색할 닉네임 문자열
     * @param limit 최대 조회 건수
     * @return 닉네임이 일치하는 사용자 목록 (최대 limit 건)
     */
    List<User> findByNicknameContaining(String nickname, int limit);

    /**
     * 이메일 존재 여부를 확인한다.
     *
     * @param email 확인할 이메일 주소
     * @return 존재 여부
     */
    boolean existsByEmail(String email);

    /**
     * 닉네임 존재 여부를 확인한다.
     *
     * @param nickname 확인할 닉네임
     * @return 존재 여부
     */
    boolean existsByNickname(String nickname);

    /**
     * 사용자를 삭제한다.
     *
     * @param user 삭제할 사용자
     */
    void delete(User user);

    /**
     * 모든 사용자를 조회한다.
     *
     * @return 전체 사용자 목록
     */
    List<User> findAll();

    /**
     * 모든 사용자를 DB 레벨 페이지네이션으로 조회한다.
     *
     * @param pageable 페이지네이션 정보
     * @return 페이지네이션된 사용자 목록
     */
    Page<User> findAll(Pageable pageable);

    /**
     * 특정 상태의 사용자 목록을 조회한다.
     *
     * @param status 사용자 상태
     * @return 해당 상태의 사용자 목록
     */
    List<User> findByStatus(User.UserStatus status);

    /**
     * 특정 상태의 사용자 목록을 DB 레벨 페이지네이션으로 조회한다.
     *
     * @param status   사용자 상태
     * @param pageable 페이지네이션 정보
     * @return 페이지네이션된 사용자 목록
     */
    Page<User> findByStatus(User.UserStatus status, Pageable pageable);

    /**
     * 여러 ID로 사용자 목록을 조회한다.
     *
     * @param ids 조회할 사용자 ID 목록
     * @return 조회된 사용자 목록
     */
    List<User> findAllById(Iterable<Long> ids);

    /**
     * 전체 사용자 수를 조회한다.
     *
     * @return 사용자 수
     */
    long count();

    /**
     * 특정 상태의 사용자 수를 조회한다.
     *
     * @param status 사용자 상태
     * @return 해당 상태의 사용자 수
     */
    long countByStatus(User.UserStatus status);

    /**
     * 닉네임과 전화번호로 사용자를 조회한다.
     *
     * @param nickname 닉네임
     * @param phoneNumber 전화번호
     * @return 조회된 사용자 (Optional)
     */
    Optional<User> findByNicknameAndPhoneNumber(String nickname, String phoneNumber);
}
