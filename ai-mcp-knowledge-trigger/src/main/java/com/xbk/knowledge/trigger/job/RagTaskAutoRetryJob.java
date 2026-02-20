package com.xbk.knowledge.trigger.job;

import com.xbk.knowledge.application.service.app.RagAppService;
import com.xbk.knowledge.domain.model.entity.RagTask;
import com.xbk.knowledge.domain.model.adapter.repository.rag.RagTaskRepository;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 自动重试失败任务定时任务
 * 每天凌晨 2 点自动重试昨天失败的任务
 *
 * 职责：定时任务入口，用于自动重试失败任务
 * @author xiexu
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RagTaskAutoRetryJob {

    private final RagTaskRepository ragTaskRepository;
    private final RagAppService ragAppService;

    /**
     * 自动重试失败任务
     * XXL-Job Handler: ragTaskAutoRetryHandler
     * 建议 Cron: 0 0 2 * * ? (每天凌晨 2 点执行)
     *
     * 为什么：集中在离峰时间批量重试，降低白天资源竞争与用户体验波动。
     */
    @XxlJob("ragTaskAutoRetryHandler")
    public void autoRetryFailedTasks() {
        
        // 查询昨天失败的任务（状态为 FAILED 或 COMPLETED 但有失败详情）
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
        List<RagTask> failedTasks = ragTaskRepository.findFailedTasksSince(yesterday);

        if (failedTasks.isEmpty()) {
            log.info("没有需要重试的失败任务");
            return;
        }

        log.info("找到 {} 个失败任务，开始重试", failedTasks.size());

        
        int successCount = 0;
        int skipCount = 0;
        int failCount = 0;

        for (RagTask task : failedTasks) {
            try {
                
                // 检查重试次数（最多自动重试 3 次）
                Integer retryCount = task.getRetryCount();
                if (retryCount != null && retryCount >= 3) {
                    log.warn("任务 {} 已达到最大自动重试次数 3 次，跳过", task.getTaskId());
                    skipCount++;
                    continue;
                }

                
                // 检查是否有失败详情
                if (!StringUtils.hasText(task.getErrorDetails())) {
                    log.warn("任务 {} 没有失败详情，跳过", task.getTaskId());
                    skipCount++;
                    continue;
                }

                // 重试任务
                String newTaskId = ragAppService.retryTask(task.getTaskId());
                successCount++;

                log.info("任务 {} 重试成功，新任务 ID: {}", task.getTaskId(), newTaskId);

            } catch (Exception e) {
                failCount++;
                log.error("任务 {} 重试失败", task.getTaskId(), e);
            }
        }

        log.info("自动重试完成，成功: {}, 跳过: {}, 失败: {}", successCount, skipCount, failCount);
    }
}
