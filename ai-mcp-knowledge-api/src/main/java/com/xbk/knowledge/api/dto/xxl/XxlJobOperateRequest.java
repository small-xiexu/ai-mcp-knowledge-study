package com.xbk.knowledge.api.dto.xxl;

import lombok.Data;

import jakarta.validation.constraints.NotNull;

/**
 * XXL 任务操作请求
 * 用于启动/停止/删除等操作
 *
 * 职责：接口层 DTO，用于承载请求参数并保证传输边界稳定
 * @author sxie
 */
@Data
public class XxlJobOperateRequest {

    /**
     * 序列化版本号
     */
    private static final long serialVersionUID = 1L;

    /**
     * 任务 ID
     */
    @NotNull(message = "任务 ID 不能为空")
    private Long id;
}
