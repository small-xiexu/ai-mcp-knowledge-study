package com.xbk.knowledge.trigger.job;

import com.xbk.knowledge.domain.repository.RagTaskRepository;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 清理过期任务定时任务
 * 每天凌晨 3 点清理 30 天前的已完成任务
 *
 * 职责：定时任务入口，用于清理过期数据
 * @author xiexu
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RagTaskCleanupJob {

    private final RagTaskRepository ragTaskRepository;

    /**
     * 清理过期任务
     * XXL-Job Handler: ragTaskCleanupHandler
     * 建议 Cron: 0 0 3 * * ? (每天凌晨 3 点执行)
     */
    @XxlJob("ragTaskCleanupHandler")
    public void cleanupExpiredTasks() {
        log.info("开始清理过期任务...");

        try {
            // 删除 30 天前的已完成任务
            LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
            int deletedCount = ragTaskRepository.deleteCompletedTasksBefore(thirtyDaysAgo);

            log.info("清理完成，删除 {} 个过期任务", deletedCount);

        } catch (Exception e) {
            log.error("清理过期任务异常", e);
        }
    }
}
