package com.xbk.knowledge.config.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * RAG 任务执行器配置
 * 用于并行处理文档解析任务
 *
 * @author sxie
 */
@Slf4j
@Configuration
public class RagTaskExecutorConfig {

    /**
     * RAG 任务线程池
     * 核心线程数：5
     * 最大线程数：10
     * 队列容量：100
     */
    @Bean(name = "ragTaskExecutor")
    public ThreadPoolTaskExecutor ragTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 核心线程数：同时处理的文件数
        executor.setCorePoolSize(5);

        // 最大线程数：高峰期最多处理的文件数
        executor.setMaxPoolSize(10);

        // 队列容量：等待处理的任务数
        executor.setQueueCapacity(100);

        // 线程名称前缀
        executor.setThreadNamePrefix("rag-task-");

        // 拒绝策略：队列满时，由调用线程执行
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // 线程空闲时间：60 秒
        executor.setKeepAliveSeconds(60);

        // 允许核心线程超时
        executor.setAllowCoreThreadTimeOut(true);

        // 等待所有任务完成后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);

        // 等待时间：30 秒
        executor.setAwaitTerminationSeconds(30);

        executor.initialize();

        log.info("RAG 任务线程池初始化完成，核心线程数: {}, 最大线程数: {}, 队列容量: {}",
                executor.getCorePoolSize(),
                executor.getMaxPoolSize(),
                executor.getQueueCapacity());

        return executor;
    }
}
