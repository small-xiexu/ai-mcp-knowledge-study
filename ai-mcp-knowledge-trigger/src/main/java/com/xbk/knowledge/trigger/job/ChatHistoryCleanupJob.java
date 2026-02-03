package com.xbk.knowledge.trigger.job;

import com.xbk.knowledge.application.service.app.ChatHistoryCleanupAppService;
import com.xbk.knowledge.config.ChatHistoryProperties;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 聊天历史清理任务
 * 定时清理超过保留期的聊天会话与消息
 *
 * @author xiexu
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatHistoryCleanupJob {

    private final ChatHistoryCleanupAppService chatHistoryCleanupAppService;
    private final ChatHistoryProperties chatHistoryProperties;

    /**
     * 清理过期聊天历史
     * XXL-Job Handler: chatHistoryCleanupHandler
     * 建议 Cron: 0 0 3 * * ? (每天凌晨 3 点执行)
     *
     * 为什么：避免聊天历史无限增长导致存储成本与查询性能问题。
     */
    @XxlJob("chatHistoryCleanupHandler")
    public void cleanupExpiredChatHistory() {
        
        int retentionDays = chatHistoryProperties.getRetentionDays();
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        int deletedSessions = chatHistoryCleanupAppService.cleanupExpired(cutoff);
        log.info("聊天历史清理完成，删除会话数={}，截止时间={}", deletedSessions, cutoff);
    }
}
