package com.xbk.knowledge.config.trace;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步执行器 Trace 配置。
 * 为 @Async 默认线程池注入 MDC 透传能力，保证异步链路 traceId 连续。
 *
 * @author sxie
 */
@Slf4j
@Configuration
public class AsyncTraceConfig implements AsyncConfigurer {

    /**
     * 异步任务 Trace/MDC 装饰器。
     */
    private final TaskDecorator traceMdcTaskDecorator;

    public AsyncTraceConfig(TaskDecorator traceMdcTaskDecorator) {
        this.traceMdcTaskDecorator = traceMdcTaskDecorator;
    }

    @Bean(name = {"taskExecutor", "applicationTaskExecutor"})
    public ThreadPoolTaskExecutor applicationTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(32);
        executor.setQueueCapacity(1000);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("async-trace-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setTaskDecorator(traceMdcTaskDecorator);
        executor.initialize();
        return executor;
    }

    @Override
    public Executor getAsyncExecutor() {
        return applicationTaskExecutor();
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (Throwable ex, Method method, Object... params) ->
                log.error("异步任务执行失败, method={}, paramsCount={}", method.getName(), params == null ? 0 : params.length, ex);
    }
}
