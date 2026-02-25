package com.xbk.knowledge.api.dto.model;

import com.xbk.knowledge.types.enums.ModelType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 模型配置响应 DTO
 * 用于返回模型配置信息
 *
 * 职责：接口层 DTO，用于承载请求/响应参数并保证传输边界稳定
 * @author sxie
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelConfigResponse implements Serializable {

    /**
     * 序列化版本号
     */
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 模型名称
     */
    private String modelName;

    /**
     * 模型类型
     */
    private ModelType modelType;

    /**
     * API地址
     */
    private String baseUrl;

    /**
     * 对话补全路径
     */
    private String completionsPath;

    /**
     * 向量嵌入路径
     */
    private String embeddingsPath;

    /**
     * API Key
     */
    private String apiKey;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 是否启用工具调用
     */
    private Boolean toolEnabled;

    /**
     * Prompt 历史字符预算
     */
    private Integer maxPromptChars;

    /**
     * Prompt 历史消息条数预算
     */
    private Integer maxHistoryMessages;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 是否为当前激活的对话模型
     */
    private Boolean activeChat;

    /**
     * 是否为当前激活的嵌入模型
     */
    private Boolean activeEmbedding;
}
