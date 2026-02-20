package com.xbk.knowledge.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 聊天历史配置
 * 用于控制上下文窗口大小与数据保留周期
 *
 * @author sxie
 */
@Data
@Component
@ConfigurationProperties(prefix = "chat.history")
public class ChatHistoryProperties {

    /**
     * 聊天记录保留天数
     */
    private int retentionDays = 7;

    /**
     * 对话上下文窗口大小
     */
    private int memoryWindowSize = 20;
}
