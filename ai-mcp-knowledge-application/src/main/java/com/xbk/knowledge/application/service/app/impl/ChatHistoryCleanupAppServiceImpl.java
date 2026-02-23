package com.xbk.knowledge.application.service.app.impl;

import com.xbk.knowledge.application.service.app.ChatHistoryCleanupAppService;
import com.xbk.knowledge.domain.chat.adapter.repository.ChatMessageRepository;
import com.xbk.knowledge.domain.chat.adapter.repository.ChatSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 聊天历史清理服务实现
 * 先删消息再删会话，保证数据一致
 *
 * 职责：应用层用例实现，用于协调仓储清理逻辑
 *
 * @author sxie
 */
@Service
@RequiredArgsConstructor
public class ChatHistoryCleanupAppServiceImpl implements ChatHistoryCleanupAppService {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;

    /**
     * 清理过期会话与消息
     *
     * 为什么：按更新时间清理历史数据，控制存储规模
     * 入参：截止时间
     * 出参：删除的会话数量
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cleanupExpired(LocalDateTime updatedBefore) {
        if (updatedBefore == null) {
            return 0;
        }
        // 先清理消息，避免会话删除后遗留孤儿消息
        chatMessageRepository.deleteBySessionUpdatedBefore(updatedBefore);
        return chatSessionRepository.deleteByUpdatedBefore(updatedBefore);
    }
}
