package com.xbk.knowledge.application.fallback;

import com.xbk.knowledge.domain.model.entity.ModelConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 降级候选模型
 * 统一封装模型信息与降级语义
 *
 * 设计模式：值对象（Value Object）
 * 职责：应用层降级候选，用于表达是否为备用模型并降低判断成本
 * @author xiexu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FailoverCandidate {

    /**
     * 模型配置
     */
    private ModelConfig model;

    /**
     * 是否为备用模型
     */
    private boolean fallback;
}
