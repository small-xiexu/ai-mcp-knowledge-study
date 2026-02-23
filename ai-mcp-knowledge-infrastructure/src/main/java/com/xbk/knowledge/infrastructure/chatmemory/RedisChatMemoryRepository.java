package com.xbk.knowledge.infrastructure.chatmemory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xbk.knowledge.infrastructure.redis.key.ChatRedisKeys;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.AbstractMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Redis ChatMemory 仓储
 * 负责将会话上下文以 JSON 形式持久化到 Redis，并配置过期时间
 *
 * 职责：ChatMemoryRepository 适配器，用于 Redis 存储对话上下文
 * @author sxie
 */
public class RedisChatMemoryRepository implements ChatMemoryRepository {

    /**
     * Redis 读写入口
     * 为什么：集中管理序列化后的会话内容
     */
    private final StringRedisTemplate stringRedisTemplate;
    /**
     * JSON 序列化工具
     * 为什么：仅持久化必要字段，减少存储体积
     */
    private final ObjectMapper objectMapper;
    /**
     * 会话过期时间
     * 为什么：对话记忆应有生命周期，避免无限增长
     */
    private final Duration ttl;

    public RedisChatMemoryRepository(StringRedisTemplate stringRedisTemplate,
                                     ObjectMapper objectMapper,
                                     Duration ttl) {
        
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.ttl = ttl;
    }

    /**
     * 查询所有会话 ID
     *
     * 为什么：用于后台清理或管理会话
     * 入参：无
     * 出参：会话 ID 列表
     */
    @Override
    public List<String> findConversationIds() {
        
        Set<String> keys = stringRedisTemplate.keys(ChatRedisKeys.CHAT_MEMORY_PREFIX + "*");
        if (keys.isEmpty()) {
            return Collections.emptyList();
        }
        return keys.stream()
                .map(key -> key.substring(ChatRedisKeys.CHAT_MEMORY_PREFIX.length()))
                .collect(Collectors.toList());
    }

    /**
     * 查询会话消息
     *
     * 为什么：加载会话上下文用于多轮对话
     * 入参：会话 ID
     * 出参：消息列表
     */
    @Override
    public List<Message> findByConversationId(String conversationId) {
        
        
        String key = buildKey(conversationId);
        String payload = stringRedisTemplate.opsForValue().get(key);
        if (payload == null || payload.isEmpty()) {
            
            return Collections.emptyList();
        }
        try {
            List<RedisChatMessage> stored = objectMapper.readValue(payload, new TypeReference<List<RedisChatMessage>>() {});
            List<Message> messages = new ArrayList<>();
            for (RedisChatMessage message : stored) {
                
                MessageType type = MessageType.fromValue(message.getType());
                messages.add(toMessage(type, message.getContent()));
            }
            return messages;
        } catch (Exception e) {
            
            return Collections.emptyList();
        }
    }

    /**
     * 保存会话消息
     *
     * 为什么：持久化对话上下文并设置过期
     * 入参：会话 ID、消息列表
     * 出参：无
     */
    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        
        
        String key = buildKey(conversationId);
        List<RedisChatMessage> payload = new ArrayList<>();
        for (Message message : messages) {
            if (message == null) {
                
                continue;
            }
            RedisChatMessage stored = new RedisChatMessage();
            stored.setType(message.getMessageType().getValue());
            stored.setContent(resolveContent(message));
            payload.add(stored);
        }
        try {
            String json = objectMapper.writeValueAsString(payload);
            
            stringRedisTemplate.opsForValue().set(key, json, ttl);
        } catch (Exception e) {
            
        }
    }

    /**
     * 删除会话消息
     *
     * 为什么：会话清理时释放缓存
     * 入参：会话 ID
     * 出参：无
     */
    @Override
    public void deleteByConversationId(String conversationId) {
        
        
        stringRedisTemplate.delete(buildKey(conversationId));
    }

    /**
     * 构建 Redis Key
     *
     * 为什么：统一 Key 前缀，避免冲突
     */
    private String buildKey(String conversationId) {
        
        
        return ChatRedisKeys.CHAT_MEMORY_PREFIX + conversationId;
    }

    /**
     * 将存储结构还原为消息对象
     *
     * 为什么：保持消息类型语义
     */
    private Message toMessage(MessageType type, String content) {
        
        
        if (type == MessageType.USER) {
            return new UserMessage(content);
        }
        if (type == MessageType.ASSISTANT) {
            return new AssistantMessage(content);
        }
        if (type == MessageType.SYSTEM) {
            return new SystemMessage(content);
        }
        return new SystemMessage(content);
    }

    /**
     * 提取消息内容
     *
     * 为什么：仅存储文本，避免复杂对象序列化
     */
    private String resolveContent(Message message) {
        
        
        if (message instanceof AbstractMessage) {
            return ((AbstractMessage) message).getText();
        }
        return null;
    }

    /**
     * Redis 存储结构
     * 仅保存角色与内容，避免引入复杂对象序列化
     *
     * @author sxie
     */
      private static class RedisChatMessage {
        private String type;
        private String content;

        /**
         * 获取消息角色类型。
         *
         * @return 返回角色类型值。
         */
        public String getType() {
            return type;
        }

        /**
         * 设置消息角色类型。
         *
         * @param type 类型标识。
         */
        public void setType(String type) {
            this.type = type;
        }

        /**
         * 获取消息文本内容。
         *
         * @return 返回消息文本。
         */
        public String getContent() {
            return content;
        }

        /**
         * 设置消息文本内容。
         *
         * @param content 输入内容
         */
        public void setContent(String content) {
            this.content = content;
        }
    }
}
