package com.xbk.knowledge.application.service.app;

import java.time.LocalDateTime;

/**
 * 聊天历史清理服务
 * 负责清理超过保留期的聊天会话与消息
 *
 * @author sxie
 */
public interface ChatHistoryCleanupAppService {

    /**
     * 清理过期聊天记录
     * 
     * @param updatedBefore 截止时间
     * @return 清理的话数量
     */
    int cleanupExpired(LocalDateTime updatedBefore);
}
