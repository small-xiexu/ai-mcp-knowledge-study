package com.xbk.knowledge.api.dto.xxl;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * XXL 任务日志详情响应
 * 用于返回日志内容与游标信息
 *
 * 职责：接口层 DTO，用于承载响应参数并保证传输边界稳定
 * @author sxie
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class XxlJobLogDetailResponse {

    /**
     * 起始行号
     */
    private Integer fromLineNum;

    /**
     * 结束行号
     */
    private Integer toLineNum;

    /**
     * 日志内容
     */
    private String logContent;

    /**
     * 是否结束
     */
    private Boolean end;
}
