package com.xbk.knowledge.infrastructure.redis.key;

/**
 * 聊天相关 Redis Key 定义
 *
 * 职责：统一聊天领域 Redis Key 命名
 * @author sxie
 */
public final class ChatRedisKeys {

    /**
     * 聊天记忆 Key 前缀
     * 统一话存储命名，支持按前缀查询
     */
    public static final String CHAT_MEMORY_PREFIX = "chat:memory:";

    private ChatRedisKeys() {
    }
}
