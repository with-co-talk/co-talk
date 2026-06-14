package com.cotalk.domain.port.outbound;

import com.cotalk.domain.entity.Message;

import java.util.List;

/**
 * 기존 암호화 메시지의 검색 토큰 백필을 위한 아웃바운드 포트.
 *
 * <p>PR1(블라인드 인덱스 인프라)은 <b>신규</b> 메시지부터 토큰을 적재하므로,
 * 그 이전에 저장된 메시지는 검색되지 않는다. 이 포트는 백필 작업이 기존 TEXT 메시지를
 * {@code id} 커서 청크 단위로 순회하며 복호화(JPA {@code @Convert} 자동 처리)된 본문을 얻기 위한
 * 조회 경로를 제공한다.</p>
 *
 * <p>대량 데이터를 한 번에 메모리에 올리지 않도록 반드시 커서({@code afterId}) + 청크 크기
 * ({@code chunkSize})로 제한된 범위만 조회한다. 검색 대상이 아닌 FILE/IMAGE/SYSTEM 메시지와
 * 소프트 삭제된 메시지는 제외한다.</p>
 *
 * @author seunggu.lee
 */
public interface MessageSearchBackfillPort {

    /**
     * 토큰 백필 대상이 되는 TEXT(미삭제) 메시지를 {@code id} 오름차순 커서 청크로 조회한다.
     *
     * <p>조건: {@code id > afterId AND type = 'TEXT' AND is_deleted = false},
     * {@code ORDER BY id ASC LIMIT chunkSize}. 반환된 메시지의 {@code content}는 JPA 컨버터로
     * 이미 복호화된 평문이다.</p>
     *
     * @param afterId   이 ID보다 큰 메시지만 조회 (커서; 첫 호출은 0)
     * @param chunkSize 한 번에 조회할 최대 메시지 수
     * @return id 오름차순 메시지 목록 (더 이상 없으면 빈 목록)
     */
    List<Message> findTextMessagesForBackfill(long afterId, int chunkSize);

    /**
     * 특정 메시지에 이미 검색 토큰이 적재되어 있는지 확인한다.
     *
     * <p>"이미 토큰이 있는 메시지는 건너뛰기(skip-existing)" 최적화에 사용한다. 백필 자체는
     * delete-then-insert로 항상 idempotent하지만, 신규 메시지(PR1)로 이미 토큰이 있는 메시지의
     * 불필요한 복호화/HMAC 재계산을 피해 대량 데이터에서 부하를 줄인다.</p>
     *
     * @param messageId 확인할 메시지 ID
     * @return 토큰이 1개 이상 존재하면 {@code true}
     */
    boolean hasTokens(long messageId);
}
