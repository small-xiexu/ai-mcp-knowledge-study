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
 * Redis 会话记忆仓储实现。
 *
 * 职责：实现 ChatMemoryRepository，负责对话消息的读写与过期管理。
 *
 * @author sxie
 */
public class RedisChatMemoryRepository implements ChatMemoryRepository {

    /**
     * Redis 读写模板。
     */
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * JSON 序列化工具。
     */
    private final ObjectMapper objectMapper;

    /**
     * 会话过期时间。
     */
    private final Duration ttl;

    /**
     * 创建 Redis 会话记忆仓储。
     * 
     * @param stringRedisTemplate Redis 模板
     * @param objectMapper JSON 序列化器
     * @param ttl 会话过期时间
     */
    public RedisChatMemoryRepository(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper, Duration ttl) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.ttl = ttl;
    }

    /**
     * 查询所有会话 ID。
     * 
     * @return 会话 ID 列表
     */
    @Override
    public List<String> findConversationIds() {
        Set<String> keys = stringRedisTemplate.keys(ChatRedisKeys.CHAT_MEMORY_PREFIX + "*");
        if (keys.isEmpty()) {
            return Collections.emptyList();
        }
        return keys.stream().map(key -> key.substring(ChatRedisKeys.CHAT_MEMORY_PREFIX.length())).collect(Collectors.toList());
    }

    /**
     * 按会话 ID 查询消息列表。
     * 
     * @param conversationId 会话 ID
     * @return 消息列表
     */
    @Override
    public List<Message> findByConversationId(String conversationId) {
        String key = buildKey(conversationId);
        String payload = stringRedisTemplate.opsForValue().get(key);
        if (payload == null || payload.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            List<RedisChatMessage> stored = objectMapper.readValue(payload, new TypeReference<List<RedisChatMessage>>() {
            });
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
     * 保存会话消息并设置过期时间。
     * 
     * @param conversationId 会话 ID
     * @param messages 消息列表
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
     * 删除指定会话的消息。
     * 
     * @param conversationId 会话 ID
     */
    @Override
    public void deleteByConversationId(String conversationId) {
        stringRedisTemplate.delete(buildKey(conversationId));
    }

    /**
     * 构建 Redis Key。
     * 
     * @param conversationId 会话 ID
     * @return Redis Key
     */
    private String buildKey(String conversationId) {
        return ChatRedisKeys.CHAT_MEMORY_PREFIX + conversationId;
    }

    /**
     * 将存储结构转换为消息对象。
     * 
     * @param type 消息类型
     * @param content 消息内容
     * @return 消息对象
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
     * 提取消息文本内容。
     * 
     * @param message 消息对象
     * @return 消息文本
     */
    private String resolveContent(Message message) {
        if (message instanceof AbstractMessage) {
            return ((AbstractMessage) message).getText();
        }
        return null;
    }

    /**
     * Redis 存储结构。
     *
     * @author sxie
     */
    private static class RedisChatMessage {
        /**
         * 消息类型。
         */
        private String type;

        /**
         * 消息内容。
         */
        private String content;

        /**
         * 获取消息角色类型。
         * 
         * @return 角色类型值。
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
         * @return 消息文本。
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
