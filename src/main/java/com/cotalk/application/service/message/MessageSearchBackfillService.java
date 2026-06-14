package com.cotalk.application.service.message;

import com.cotalk.domain.entity.Message;
import com.cotalk.domain.port.outbound.BlindIndexTokenizer;
import com.cotalk.domain.port.outbound.MessageSearchBackfillPort;
import com.cotalk.domain.port.outbound.MessageSearchTokenRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Set;

/**
 * 기존 암호화 메시지 검색 토큰 백필 서비스 (PR2).
 *
 * <p>PR1(블라인드 인덱스 인프라)은 신규 메시지부터만 {@code message_search_tokens}에 토큰을
 * 적재한다. 이 서비스는 그 이전에 저장된 모든 TEXT(미삭제) 메시지를 복호화 → 토큰화 →
 * 토큰 적재하여 과거 메시지도 검색되도록 복구하는 <b>1회성 관리 작업</b>이다.</p>
 *
 * <h2>처리 방식</h2>
 * <ul>
 *   <li><b>커서 청크</b>: {@code id} 오름차순 커서로 {@code chunkSize}건씩 순회한다
 *       ({@link MessageSearchBackfillPort#findTextMessagesForBackfill}). 전체를 메모리에 올리지 않는다.</li>
 *   <li><b>청크 단위 트랜잭션</b>: 청크 하나를 한 트랜잭션으로 커밋한다. 장기 트랜잭션/락을 피한다.</li>
 *   <li><b>재실행 안전(idempotent)</b>: 메시지별 {@code deleteByMessageId} 후 재적재(delete-then-insert).
 *       토큰 PK가 {@code (message_id, token)}이라 중복 적재가 발생하지 않으며, 몇 번을 다시 돌려도
 *       결과가 동일하다.</li>
 *   <li><b>중단 복구</b>: 진행 커서가 {@code id} 기반이라 마지막으로 처리한 ID부터 재개하면 그대로
 *       이어진다(매 실행은 0부터 시작하되 처리 완료분은 idempotent로 재처리, 작은 데이터면 단순).
 *       처리 진행 로그로 마지막 커서를 추적할 수 있다.</li>
 *   <li><b>부하 제어</b>: {@code skipExisting=true}면 이미 토큰이 있는 메시지(신규 PR1 적재분)는
 *       복호화/HMAC 없이 건너뛴다. 청크마다 {@code throttleMillis}만큼 슬립해 운영 윈도우 부하를 낮춘다.</li>
 * </ul>
 *
 * <h2>헥사고날</h2>
 * <p>application 서비스는 아웃바운드 포트({@link MessageSearchBackfillPort},
 * {@link MessageSearchTokenRepository}, {@link BlindIndexTokenizer})만 의존하며 infrastructure를
 * 직접 참조하지 않는다(ArchUnit 준수).</p>
 *
 * @author seunggu.lee
 */
@Slf4j
@Service
public class MessageSearchBackfillService {

    private final MessageSearchBackfillPort backfillPort;
    private final MessageSearchTokenRepository messageSearchTokenRepository;
    private final BlindIndexTokenizer blindIndexTokenizer;
    private final TransactionTemplate transactionTemplate;

    /**
     * 백필 서비스 생성자.
     *
     * @param backfillPort                 백필 대상 메시지 청크 조회 포트
     * @param messageSearchTokenRepository 검색 토큰 적재/삭제 포트
     * @param blindIndexTokenizer          블라인드 인덱스 토큰화 포트
     * @param transactionTemplate          청크 단위 트랜잭션 템플릿
     */
    public MessageSearchBackfillService(MessageSearchBackfillPort backfillPort,
                                        MessageSearchTokenRepository messageSearchTokenRepository,
                                        BlindIndexTokenizer blindIndexTokenizer,
                                        TransactionTemplate transactionTemplate) {
        this.backfillPort = backfillPort;
        this.messageSearchTokenRepository = messageSearchTokenRepository;
        this.blindIndexTokenizer = blindIndexTokenizer;
        this.transactionTemplate = transactionTemplate;
    }

    /**
     * 기존 TEXT 메시지의 검색 토큰을 백필한다.
     *
     * <p>{@code id} 오름차순 커서로 청크를 반복 조회해, 청크 단위 트랜잭션 안에서 각 메시지를
     * delete-then-insert로 재토큰화한다. 더 이상 처리할 메시지가 없으면(빈 청크) 종료한다.</p>
     *
     * @param options 백필 옵션(청크 크기, throttle, skip-existing)
     * @return 백필 결과 요약(스캔/색인/스킵 건수, 마지막 커서)
     */
    public BackfillResult backfill(BackfillOptions options) {
        BackfillOptions opts = options == null ? BackfillOptions.defaults() : options.sanitized();
        log.info("메시지 검색 토큰 백필 시작: chunkSize={}, throttleMillis={}, skipExisting={}",
                opts.chunkSize(), opts.throttleMillis(), opts.skipExisting());

        long cursor = 0L;
        long scanned = 0L;
        long indexed = 0L;
        long skipped = 0L;

        while (true) {
            List<Message> chunk = backfillPort.findTextMessagesForBackfill(cursor, opts.chunkSize());
            if (chunk.isEmpty()) {
                break;
            }

            ChunkResult chunkResult = processChunk(chunk, opts);
            scanned += chunk.size();
            indexed += chunkResult.indexed();
            skipped += chunkResult.skipped();
            cursor = chunkResult.lastId();

            log.info("백필 진행: 누적 scanned={}, indexed={}, skipped={}, 마지막 커서 id={}",
                    scanned, indexed, skipped, cursor);

            throttle(opts.throttleMillis());

            // 마지막 청크(요청 크기 미만)면 더 이상 조회할 게 없으므로 종료
            if (chunk.size() < opts.chunkSize()) {
                break;
            }
        }

        BackfillResult result = new BackfillResult(scanned, indexed, skipped, cursor);
        log.info("메시지 검색 토큰 백필 완료: {}", result);
        return result;
    }

    /**
     * 청크 하나를 한 트랜잭션으로 처리한다(메시지별 delete-then-insert 재토큰화).
     *
     * @param chunk   처리할 메시지 청크(id 오름차순)
     * @param options 백필 옵션
     * @return 이 청크의 색인/스킵 건수와 마지막 메시지 ID
     */
    private ChunkResult processChunk(List<Message> chunk, BackfillOptions options) {
        ChunkResult fallback = new ChunkResult(0L, 0L, chunk.get(chunk.size() - 1).getId());
        ChunkResult result = transactionTemplate.execute(status -> {
            long indexed = 0L;
            long skipped = 0L;
            long lastId = 0L;
            for (Message message : chunk) {
                lastId = message.getId();
                if (options.skipExisting() && backfillPort.hasTokens(message.getId())) {
                    skipped++;
                    continue;
                }
                Set<String> tokens = blindIndexTokenizer.tokenize(message.getContent());
                // idempotent: 기존 토큰 제거 후 재적재. 토큰이 비어도(3글자 미만) 기존 토큰은 정리한다.
                messageSearchTokenRepository.deleteByMessageId(message.getId());
                if (!tokens.isEmpty()) {
                    messageSearchTokenRepository.saveTokens(message.getId(), tokens);
                    indexed++;
                }
            }
            return new ChunkResult(indexed, skipped, lastId);
        });
        return result == null ? fallback : result;
    }

    private void throttle(long throttleMillis) {
        if (throttleMillis <= 0) {
            return;
        }
        try {
            Thread.sleep(throttleMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("백필 throttle 슬립이 인터럽트되었습니다. 백필을 중단합니다.");
            throw new IllegalStateException("백필이 인터럽트로 중단되었습니다.", e);
        }
    }

    /**
     * 한 청크 처리 결과(내부용).
     *
     * @param indexed 토큰을 적재한 메시지 수
     * @param skipped 스킵한 메시지 수
     * @param lastId  처리한 마지막 메시지 ID(다음 커서)
     */
    private record ChunkResult(long indexed, long skipped, long lastId) {}

    /**
     * 백필 실행 옵션.
     *
     * @param chunkSize      한 청크(=한 트랜잭션) 당 메시지 수
     * @param throttleMillis 청크 사이 슬립(ms). 0이면 슬립 없음
     * @param skipExisting   이미 토큰이 있는 메시지는 건너뛸지 여부
     */
    public record BackfillOptions(int chunkSize, long throttleMillis, boolean skipExisting) {

        /** 기본 청크 크기. */
        public static final int DEFAULT_CHUNK_SIZE = 500;
        private static final int MAX_CHUNK_SIZE = 5000;

        /**
         * 안전 기본 옵션을 만든다(청크 500, throttle 0, skip-existing true).
         *
         * @return 기본 옵션
         */
        public static BackfillOptions defaults() {
            return new BackfillOptions(DEFAULT_CHUNK_SIZE, 0L, true);
        }

        /**
         * 비정상 값을 안전 범위로 보정한 옵션을 반환한다.
         *
         * @return 보정된 옵션
         */
        public BackfillOptions sanitized() {
            int safeChunk = Math.min(Math.max(chunkSize, 1), MAX_CHUNK_SIZE);
            long safeThrottle = Math.max(throttleMillis, 0L);
            return new BackfillOptions(safeChunk, safeThrottle, skipExisting);
        }
    }

    /**
     * 백필 결과 요약.
     *
     * @param scanned   조회(스캔)한 총 메시지 수
     * @param indexed   토큰을 적재한 메시지 수
     * @param skipped   이미 토큰이 있어 건너뛴 메시지 수
     * @param lastCursorId 마지막으로 처리한 메시지 ID(중단/재개 추적용)
     */
    public record BackfillResult(long scanned, long indexed, long skipped, long lastCursorId) {}
}
