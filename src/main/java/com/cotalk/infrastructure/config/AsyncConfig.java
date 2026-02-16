package com.cotalk.infrastructure.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 비동기 작업 스레드 풀 설정.
 *
 * <p>푸시 알림, 링크 프리뷰, 이메일 전송 등 {@code @Async} 메서드의
 * 동시 실행 수를 제한하여 리소스 고갈을 방지한다.</p>
 *
 * <p>Virtual Threads는 별도로 활성화되어 있지만, {@code @Async} 작업은
 * 명시적으로 구성된 스레드 풀에서 실행된다. 이는 비동기 작업의 동시성을
 * 제어하고 모니터링하기 위함이다.</p>
 *
 * @author seunggu.lee
 */
@Configuration
public class AsyncConfig implements AsyncConfigurer {

    /**
     * 비동기 작업용 스레드 풀 Executor를 생성한다.
     *
     * <p>코어 5개, 최대 20개 스레드로 제한하며, 큐가 가득 차면
     * 호출 스레드에서 직접 실행한다 (CallerRunsPolicy).
     * Graceful Shutdown 시 최대 30초 동안 실행 중인 작업이 완료될 때까지 대기한다.</p>
     *
     * @return 비동기 작업 Executor
     */
    @Override
    @Bean(name = "taskExecutor")
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
