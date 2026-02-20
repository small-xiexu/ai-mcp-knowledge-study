package com.xbk.knowledge.application.service.app.impl;

import com.xbk.knowledge.application.service.app.ChatSessionAppService;
import com.xbk.knowledge.application.service.app.ModelConfigAppService;
import com.xbk.knowledge.domain.chat.model.entity.ChatMessage;
import com.xbk.knowledge.domain.chat.model.entity.ChatSession;
import com.xbk.knowledge.domain.llm.model.entity.ModelConfig;
import com.xbk.knowledge.domain.chat.model.valobj.ChatMessagePageQuery;
import com.xbk.knowledge.domain.chat.model.valobj.ChatSessionPageQuery;
import com.xbk.knowledge.domain.common.model.valobj.IdQuery;
import com.xbk.knowledge.domain.chat.adapter.repository.ChatMessageRepository;
import com.xbk.knowledge.domain.chat.adapter.repository.ChatSessionRepository;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 聊天会话应用服务实现
 * 负责会话与消息相关用例编排
 *
 * 职责：应用层用例实现，用于协调领域能力
 *
 * @author sxie
 */
@Service
@RequiredArgsConstructor
public class ChatSessionAppServiceImpl implements ChatSessionAppService {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatMemoryRepository chatMemoryRepository;
    private final ModelConfigAppService modelConfigAppService;

    /**
     * 创建会话
     *
     * 为什么：会话是消息聚合根，创建时保证数据一致性
     * 入参：会话实体
     * 出参：持久化后的会话
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatSession createSession(ChatSession session) {
        return chatSessionRepository.create(session);
    }

    /**
     * 更新会话
     *
     * 为什么：集中处理会话元数据修改，保持统一事务边界
     * 入参：会话实体
     * 出参：更新后的会话
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatSession updateSession(ChatSession session) {
        /*
         * 目的：首次消息发送后锁定模型，避免会话中途切换模型
 */
        if (session != null && session.getId() != null) {
            ChatSession existing = chatSessionRepository.findById(session.getId());
            if (existing != null && existing.getModelId() != null) {
                Long requestedModelId = session.getModelId();
                if (!existing.getModelId().equals(requestedModelId)) {
                    long messageCount = chatMessageRepository.countBySessionId(session.getId());
                    if (messageCount > 0) {
                        String message = buildModelLockMessage(existing.getModelId());
                        throw new BusinessException(message);
                    }
                }
            }
        }
        return chatSessionRepository.update(session);
    }

    /**
     * 删除会话
     *
     * 为什么：删除会话时需同步清理消息与记忆，避免残留数据
     * 入参：会话 ID
     * 出参：无
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSession(Long sessionId) {
        /*
         * 目的：先删消息再删会话，避免外键或引用一致性问题
 */
        chatMessageRepository.deleteBySessionId(sessionId);
        chatSessionRepository.deleteById(sessionId);
        if (sessionId != null) {
            chatMemoryRepository.deleteByConversationId(String.valueOf(sessionId));
        }
    }

    /**
     * 查询会话
     *
     * 为什么：提供会话详情给前端或业务流程
     * 入参：会话 ID
     * 出参：会话实体
     */
    @Override
    public ChatSession getSession(Long sessionId) {
        return chatSessionRepository.findById(sessionId);
    }

    /**
     * 分页查询会话
     *
     * 为什么：控制单次返回数量，避免加载过大
     * 入参：页码、页大小
     * 出参：分页结果
     */
    @Override
    public PageResult<ChatSession> listSessions(int pageNum, int pageSize) {
        /*
         * 目的：将页码转换为偏移量以适配仓储分页
 */
        int offset = Math.max(pageNum - 1, 0) * pageSize;
        ChatSessionPageQuery query = new ChatSessionPageQuery(offset, pageSize);
        List<ChatSession> sessions = chatSessionRepository.findPage(query);
        long total = chatSessionRepository.countAll();
        return PageResult.of(sessions, total, pageNum, pageSize);
    }

    /**
     * 追加消息
     *
     * 为什么：消息写入需保证事务性，便于后续检索与统计
     * 入参：消息实体
     * 出参：持久化后的消息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatMessage appendMessage(ChatMessage message) {
        return chatMessageRepository.create(message);
    }

    /**
     * 分页查询会话消息
     *
     * 为什么：历史消息可能很大，分页防止接口超时
     * 入参：会话 ID、页码、页大小
     * 出参：分页结果
     */
    @Override
    public PageResult<ChatMessage> listMessages(Long sessionId, int pageNum, int pageSize) {
        /*
         * 目的：将页码转换为偏移量以适配仓储分页
 */
        int offset = Math.max(pageNum - 1, 0) * pageSize;
        ChatMessagePageQuery query = new ChatMessagePageQuery(sessionId, offset, pageSize);
        List<ChatMessage> messages = chatMessageRepository.findPage(query);
        long total = chatMessageRepository.countBySessionId(sessionId);
        return PageResult.of(messages, total, pageNum, pageSize);
    }

    /**
     * 清空会话消息
     *
     * 为什么：仅清理消息，不影响会话元数据
     * 入参：会话 ID
     * 出参：无
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMessages(Long sessionId) {
        chatMessageRepository.deleteBySessionId(sessionId);
    }

    private String buildModelLockMessage(Long modelId) {
        String modelName = resolveModelName(modelId);
        String displayName = modelName != null ? modelName : String.valueOf(modelId);
        return "该会话已绑定模型【" + displayName + "】，为保证对话一致性不可切换模型。如需切换，请新建会话。";
    }

    private String resolveModelName(Long modelId) {
        if (modelId == null) {
            return null;
        }
        ModelConfig modelConfig = modelConfigAppService.queryModelConfigById(new IdQuery(modelId));
        if (modelConfig == null) {
            return null;
        }
        return modelConfig.getModelName();
    }
}
