package com.cotalk.infrastructure.config;

import com.cotalk.application.service.message.MessageSearchBackfillService;
import com.cotalk.application.service.message.MessageSearchBackfillService.BackfillInterruptedException;
import com.cotalk.application.service.message.MessageSearchBackfillService.BackfillOptions;
import com.cotalk.application.service.message.MessageSearchBackfillService.BackfillResult;
import com.cotalk.infrastructure.config.properties.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 기존 암호화 메시지 검색 토큰 백필 실행기 (PR2, 1회성 관리 작업).
 *
 * <p>{@code app.search.backfill.enabled=true}일 때만 빈으로 등록되어 애플리케이션 기동 직후
 * {@link MessageSearchBackfillService#backfill}을 실행한다. 기본값은 false라 평상시 배포에서는
 * 실행되지 않는다(실수 실행 방지).</p>
 *
 * <h2>운영 런북 (요약)</h2>
 * <ul>
 *   <li><b>실행</b>: 백필 전용 인스턴스(또는 운영 윈도우)에 환경변수
 *       {@code SEARCH_BACKFILL_ENABLED=true}(또는 {@code app.search.backfill.enabled=true})로 기동.
 *       청크/throttle은 {@code app.search.backfill.chunk-size},
 *       {@code app.search.backfill.throttle-millis}로 조절.</li>
 *   <li><b>모니터링</b>: 진행 로그("백필 진행: 누적 scanned/indexed/skipped, 마지막 커서 id=...")로
 *       처리량과 커서를 추적. 완료 시 "백필 완료" 로그 + 결과 요약.</li>
 *   <li><b>중단/재개</b>: idempotent(delete-then-insert) + {@code skipExisting}이라 다시 기동해도
 *       안전하게 재개/재처리된다.</li>
 *   <li><b>롤백</b>: {@code TRUNCATE message_search_tokens;}로 전체 토큰 제거(검색은 신규 메시지부터
 *       다시 점진 채워짐). 부분 롤백이 필요하면 메시지 ID 범위로 {@code DELETE}.</li>
 *   <li><b>완료 후</b>: {@code enabled=false}(기본)로 되돌려 재기동 시 재실행되지 않게 한다.</li>
 * </ul>
 *
 * @author seunggu.lee
 */
@Slf4j
@Component
@Order(0)
@ConditionalOnProperty(prefix = "app.search.backfill", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class MessageSearchBackfillRunner implements ApplicationRunner {

    private final MessageSearchBackfillService backfillService;
    private final AppProperties appProperties;

    /**
     * 애플리케이션 기동 시 백필을 1회 실행한다.
     *
     * @param args 애플리케이션 인자(미사용)
     */
    @Override
    public void run(ApplicationArguments args) {
        AppProperties.Backfill cfg = appProperties.search().backfill();
        log.info("=== 기존 메시지 검색 토큰 백필 실행기 시작 (app.search.backfill.enabled=true) ===");

        BackfillOptions options = new BackfillOptions(
                cfg.chunkSize() <= 0 ? BackfillOptions.DEFAULT_CHUNK_SIZE : cfg.chunkSize(),
                cfg.throttleMillis(),
                cfg.skipExisting());

        try {
            BackfillResult result = backfillService.backfill(options);
            log.info("=== 백필 상태=SUCCESS(완료): scanned={}, indexed={}, skipped={}, 마지막 커서 id={} ===",
                    result.scanned(), result.indexed(), result.skipped(), result.lastCursorId());
        } catch (BackfillInterruptedException e) {
            // 백필이 중간에 실패해 일부만 반영됨. "완료"로 오인하지 않도록 상태=PARTIAL로 명확히 보고하고,
            // 진행분/마지막 커서를 노출한다. 기동 실패로 번지지 않게 흡수하되(재실행 안전 — idempotent),
            // 운영자가 재실행으로 재개해야 함을 강조한다.
            BackfillResult partial = e.partialResult();
            log.error("=== 백필 상태=PARTIAL(중단/실패): 끝까지 완료되지 못했습니다. "
                            + "scanned={}, indexed={}, skipped={}, 마지막 커서 id={}. "
                            + "idempotent하므로 재실행으로 재개 필요. ===",
                    partial.scanned(), partial.indexed(), partial.skipped(), partial.lastCursorId(), e.getCause());
        } catch (RuntimeException e) {
            // 백필 시작 전/조회 단계 등에서의 예기치 못한 실패. 진행 통계 없이 상태=FAILED로 보고.
            log.error("=== 백필 상태=FAILED(실패): 진행 통계 없이 중단되었습니다. "
                    + "idempotent하므로 재실행으로 재개 가능합니다. ===", e);
        }
    }
}
