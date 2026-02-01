package com.xbk.knowledge.infrastructure.redis.key;

/**
 * 聊天相关 Redis Key 定义
 *
 * 职责：统一聊天领域 Redis Key 命名
 * @author xiexu
 */
public final class ChatRedisKeys {

    /**
     * 聊天记忆 Key 前缀
     * 为什么：统一会话存储命名，支持按前缀查询
     */
    public static final String CHAT_MEMORY_PREFIX = "chat:memory:";

    private ChatRedisKeys() {
        // 目的：工具类不允许实例化
    }
}
