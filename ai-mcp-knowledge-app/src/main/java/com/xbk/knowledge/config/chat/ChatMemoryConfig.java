package com.xbk.knowledge.config.chat;

import com.xbk.knowledge.config.ChatHistoryProperties;
import com.xbk.knowledge.infrastructure.chatmemory.RedisChatMemoryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

/**
 * 聊天记忆配置
 * 装配 ChatMemory 与 RedisChatMemoryRepository
 *
 * @author xiexu
 */
@Configuration
public class ChatMemoryConfig {

    @Bean
    public ChatMemoryRepository chatMemoryRepository(StringRedisTemplate stringRedisTemplate,
                                                     ObjectMapper objectMapper,
                                                     ChatHistoryProperties properties) {
        Duration ttl = Duration.ofDays(properties.getRetentionDays());
        return new RedisChatMemoryRepository(stringRedisTemplate, objectMapper, ttl);
    }

    @Bean
    public ChatMemory chatMemory(ChatMemoryRepository chatMemoryRepository,
                                 ChatHistoryProperties properties) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(properties.getMemoryWindowSize())
                .build();
    }
}
