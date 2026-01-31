package com.xbk.knowledge.application.fallback.core;

import com.xbk.knowledge.application.model.dto.AICallResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 模型调用结果封装
 * 增加状态语义，用于区分失败与跳过
 *
 * 设计模式：结果对象（Result Object）
 * 职责：应用层流程结果，用于驱动降级策略
 * @author xiexu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelCallOutcome {

    /**
     * 结果状态
     */
    private Status status;

    /**
     * 业务结果
     */
    private AICallResult result;

    /**
     * 结果状态枚举
     */
    public enum Status {
        SUCCESS,
        FAILED,
        SKIPPED
    }

    /**
     * 成功结果
     *
     * @param result 业务结果
     * @return 结果封装
     */
    public static ModelCallOutcome success(AICallResult result) {
        return ModelCallOutcome.builder()
                .status(Status.SUCCESS)
                .result(result)
                .build();
    }

    /**
     * 失败结果
     *
     * @param result 业务结果
     * @return 结果封装
     */
    public static ModelCallOutcome failed(AICallResult result) {
        return ModelCallOutcome.builder()
                .status(Status.FAILED)
                .result(result)
                .build();
    }

    /**
     * 跳过结果
     *
     * @param result 业务结果
     * @return 结果封装
     */
    public static ModelCallOutcome skipped(AICallResult result) {
        return ModelCallOutcome.builder()
                .status(Status.SKIPPED)
                .result(result)
                .build();
    }

    /**
     * 是否成功
     *
     * @return 是否成功
     */
    public boolean isSuccess() {
        return Status.SUCCESS == status;
    }

    /**
     * 是否跳过
     *
     * @return 是否跳过
     */
    public boolean isSkipped() {
        return Status.SKIPPED == status;
    }
}
