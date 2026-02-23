package com.xbk.knowledge.api.dto.xxl;

import lombok.Data;

import jakarta.validation.constraints.NotNull;

/**
 * XXL 任务日志详情请求
 * 用于按行拉取执行日志内容
 *
 * 职责：接口层 DTO，用于承载请求参数并保证传输边界稳定
 * @author sxie
 */
@Data
public class XxlJobLogDetailRequest {

    /**
     * 序列化版本号
     */
    private static final long serialVersionUID = 1L;

    /**
     * 日志 ID
     */
    @NotNull(message = "日志 ID 不能为空")
    private Long logId;

    /**
     * 起始行号
     */
    private Integer fromLineNum;
}
