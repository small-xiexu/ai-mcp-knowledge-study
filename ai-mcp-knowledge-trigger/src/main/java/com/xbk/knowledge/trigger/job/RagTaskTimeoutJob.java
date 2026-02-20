package com.xbk.knowledge.trigger.job;

import com.xbk.knowledge.domain.rag.model.entity.RagTask;
import com.xbk.knowledge.domain.rag.adapter.repository.RagTaskRepository;
import com.xbk.knowledge.types.enums.RagTaskStatus;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 超时任务处理定时任务
 * 每小时检查并标记超时任务
 *
 * 职责：定时任务入口，用于处理长时间处于 PROCESSING 状态的任务
 * @author sxie
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RagTaskTimeoutJob {

    private final RagTaskRepository ragTaskRepository;

    /**
     * 处理超时任务
     * XXL-Job Handler: ragTaskTimeoutHandler
     * 建议 Cron: 0 0 * * * ? (每小时执行一次)
     *
     * 为什么：按小时扫描可及时释放卡住的任务，避免无穷等待。
     */
    @XxlJob("ragTaskTimeoutHandler")
    public void handleTimeoutTasks() {
        
        // 查询超过 2 小时仍处于 PROCESSING 状态的任务
        LocalDateTime twoHoursAgo = LocalDateTime.now().minusHours(2);
        List<RagTask> timeoutTasks = ragTaskRepository.findProcessingTasksBefore(twoHoursAgo);

        if (timeoutTasks.isEmpty()) {
            log.info("没有超时任务");
            return;
        }

        log.warn("发现 {} 个超时任务", timeoutTasks.size());

        
        // 标记为失败
        int successCount = 0;
        for (RagTask task : timeoutTasks) {
            try {
                
                task.setStatus(RagTaskStatus.FAILED);
                task.setMessage("任务超时（超过 2 小时未完成）");
                ragTaskRepository.update(task);
                successCount++;

                log.warn("任务 {} 已标记为失败（超时）", task.getTaskId());

            } catch (Exception e) {
                log.error("处理超时任务 {} 失败", task.getTaskId(), e);
            }
        }

        log.info("超时任务处理完成，成功: {}, 失败: {}", successCount, timeoutTasks.size() - successCount);
    }
}
