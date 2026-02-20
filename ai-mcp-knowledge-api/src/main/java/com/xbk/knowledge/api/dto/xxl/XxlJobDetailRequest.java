package com.xbk.knowledge.api.dto.xxl;

import lombok.Data;

import jakarta.validation.constraints.NotNull;

/**
 * XXL 任务详情查询请求
 * 用于根据任务 ID 查询任务详情
 *
 * 职责：接口层 DTO，用于承载请求参数并保证传输边界稳定
 * @author sxie
 */
@Data
public class XxlJobDetailRequest {

    private static final long serialVersionUID = 1L;

    /**
     * 任务 ID
     */
    @NotNull(message = "任务 ID 不能为空")
    private Long id;
}
