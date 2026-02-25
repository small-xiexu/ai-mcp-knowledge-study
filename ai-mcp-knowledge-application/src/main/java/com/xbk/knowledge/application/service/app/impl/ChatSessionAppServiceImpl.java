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

    /**
     * 聊天会话仓储。
     */
    private final ChatSessionRepository chatSessionRepository;

    /**
     * 聊天消息仓储。
     */
    private final ChatMessageRepository chatMessageRepository;

    /**
     * 聊天记忆仓储。
     */
    private final ChatMemoryRepository chatMemoryRepository;

    /**
     * 模型配置应用服务。
     */
    private final ModelConfigAppService modelConfigAppService;

    /**
     * 创建会话
     *
     * 话是消息聚合根，创建时保证数据一致性
     * 
     * @param session 待创建的会话实体。
     * @return 已持久化的会话实体。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatSession createSession(ChatSession session) {
        return chatSessionRepository.create(session);
    }

    /**
     * 更新会话
     *
     * 集中处理会话元数据修改，保持统一事务边界
     * 
     * @param session 待更新的会话实体。
     * @return 更新后的会话实体。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatSession updateSession(ChatSession session) {
        // 首次消息发送后锁定模型，避免话中途切换模型
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
     * 删除会话时需同步清理消息与记忆，避免残留数据
     * 
     * @param sessionId 会话 ID。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSession(Long sessionId) {
        // 先删消息再删话，避免外键或引用一致性问题
        chatMessageRepository.deleteBySessionId(sessionId);
        chatSessionRepository.deleteById(sessionId);
        if (sessionId != null) {
            chatMemoryRepository.deleteByConversationId(String.valueOf(sessionId));
        }
    }

    /**
     * 查询会话
     *
     * 提供会话详情给前端或业务流程
     * 
     * @param sessionId 会话 ID。
     * @return 会话实体。
     */
    @Override
    public ChatSession getSession(Long sessionId) {
        return chatSessionRepository.findById(sessionId);
    }

    /**
     * 分页查询会话
     *
     * 控制单次返回数量，避免加载过大
     * 
     * @param pageNum 页码。
     * @param pageSize 分页大小。
     * @return 会话分页结果。
     */
    @Override
    public PageResult<ChatSession> listSessions(int pageNum, int pageSize) {
        // 将页码转换为偏移量以适配仓储分页
        int offset = Math.max(pageNum - 1, 0) * pageSize;
        ChatSessionPageQuery query = new ChatSessionPageQuery(offset, pageSize);
        List<ChatSession> sessions = chatSessionRepository.findPage(query);
        long total = chatSessionRepository.countAll();
        return PageResult.of(sessions, total, pageNum, pageSize);
    }

    /**
     * 追加消息
     *
     * 消息写入需保证事务性，便于后续检索与统计
     * 
     * @param message 待追加的消息实体。
     * @return 已持久化的消息实体。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatMessage appendMessage(ChatMessage message) {
        return chatMessageRepository.create(message);
    }

    /**
     * 分页查询会话消息
     *
     * 历史消息可能很大，分页防止接口超时
     * 
     * @param sessionId 会话 ID。
     * @param pageNum 页码。
     * @param pageSize 分页大小。
     * @return 消息分页结果。
     */
    @Override
    public PageResult<ChatMessage> listMessages(Long sessionId, int pageNum, int pageSize) {
        // 将页码转换为偏移量以适配仓储分页
        int offset = Math.max(pageNum - 1, 0) * pageSize;
        ChatMessagePageQuery query = new ChatMessagePageQuery(sessionId, offset, pageSize);
        List<ChatMessage> messages = chatMessageRepository.findPage(query);
        long total = chatMessageRepository.countBySessionId(sessionId);
        return PageResult.of(messages, total, pageNum, pageSize);
    }

    /**
     * 清空会话消息
     *
     * 仅清理消息，不影响会话元数据
     * 
     * @param sessionId 会话 ID。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMessages(Long sessionId) {
        chatMessageRepository.deleteBySessionId(sessionId);
    }

    private String buildModelLockMessage(Long modelId) {
        String modelName = resolveModelName(modelId);
        String displayName = modelName != null ? modelName : String.valueOf(modelId);
        return "该话已绑定模型【" + displayName + "】，为保证对话一致性不可切换模型。如需切换，请新建话。";
    }

    /**
     * 解析模型名称。
     * 
     * @param modelId 模型ID。
     * @return 名称文本。
     */
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
