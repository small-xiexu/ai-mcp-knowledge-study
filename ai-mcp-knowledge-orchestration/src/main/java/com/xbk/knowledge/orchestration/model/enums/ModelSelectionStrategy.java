package com.xbk.knowledge.orchestration.model.enums;

/**
 * 模型选择策略枚举
 * 定义不同的模型选择策略
 *
 * @author xiexu
 */
public enum ModelSelectionStrategy {

    /**
     * 质量优先策略
     * 选择质量评分最高的模型
     */
    QUALITY_PRIORITY("质量优先", "选择质量评分最高的模型"),

    /**
     * 成本优先策略
     * 选择成本最低的模型
     */
    COST_PRIORITY("成本优先", "选择成本最低的模型"),

    /**
     * 速度优先策略
     * 选择响应速度最快的模型
     */
    SPEED_PRIORITY("速度优先", "选择响应速度最快的模型");

    /**
     * 策略名称
     */
    private final String name;

    /**
     * 策略描述
     */
    private final String description;

    ModelSelectionStrategy(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}
