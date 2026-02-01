package com.xbk.knowledge.infrastructure.chatmemory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * @author xiexu
 */
public class RedisChatMemoryRepository implements ChatMemoryRepository {

    private static final String KEY_PREFIX = "chat:memory:";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final Duration ttl;

    public RedisChatMemoryRepository(StringRedisTemplate stringRedisTemplate,
                                     ObjectMapper objectMapper,
                                     Duration ttl) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.ttl = ttl;
    }

    @Override
    public List<String> findConversationIds() {
        Set<String> keys = stringRedisTemplate.keys(KEY_PREFIX + "*");
        if (keys.isEmpty()) {
            return Collections.emptyList();
        }
        return keys.stream()
                .map(key -> key.substring(KEY_PREFIX.length()))
                .collect(Collectors.toList());
    }

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
            // ignore serialization errors to avoid影响主流程
        }
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        stringRedisTemplate.delete(buildKey(conversationId));
    }

    private String buildKey(String conversationId) {
        return KEY_PREFIX + conversationId;
    }

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
     * @author xiexu
     */
    private static class RedisChatMessage {
        private String type;
        private String content;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }
}
