package com.xbk.knowledge.config.data;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Redis 基础装配，统一 StringRedisTemplate 入口，便于后续扩展序列化策略。
 *
 * @author sxie
 */
@Configuration
public class RedisConfig {

    /**
     * StringRedisTemplate 仅处理字符串键值，适配当前聊天记忆存储。
     *
     * @param connectionFactory Redis 连接工厂
     * @return StringRedisTemplate
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }
}
