package com.xbk.knowledge.application.service.app.impl;

import com.xbk.knowledge.application.service.app.ChatHistoryCleanupAppService;
import com.xbk.knowledge.domain.repository.ChatMessageRepository;
import com.xbk.knowledge.domain.repository.ChatSessionRepository;
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
 * @author xiexu
 */
@Service
@RequiredArgsConstructor
public class ChatHistoryCleanupAppServiceImpl implements ChatHistoryCleanupAppService {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cleanupExpired(LocalDateTime updatedBefore) {
        if (updatedBefore == null) {
            return 0;
        }
        chatMessageRepository.deleteBySessionUpdatedBefore(updatedBefore);
        return chatSessionRepository.deleteByUpdatedBefore(updatedBefore);
    }
}
