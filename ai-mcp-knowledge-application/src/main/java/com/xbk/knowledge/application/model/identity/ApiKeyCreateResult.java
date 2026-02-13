package com.xbk.knowledge.application.model.identity;

import com.xbk.knowledge.domain.model.entity.SysApiKey;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API Key 创建结果模型。
 *
 * 职责：应用层模型，用于返回 API Key 与一次性密钥明文。
 *
 * @author xiexu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiKeyCreateResult {

    /**
     * 创建后的 API Key 实体。
     */
    private SysApiKey apiKey;

    /**
     * 一次性密钥明文。
     */
    private String plainSecret;
}
