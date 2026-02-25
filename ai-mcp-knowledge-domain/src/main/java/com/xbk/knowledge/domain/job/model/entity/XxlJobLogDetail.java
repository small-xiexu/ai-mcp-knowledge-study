package com.xbk.knowledge.domain.job.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * XXL 任务日志详情实体
 * 承载日志内容与游标信息
 *
 * 职责：领域实体，用于表达日志详情语义
 * @author sxie
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class XxlJobLogDetail {

    /**
     * 起始行号
     *
     * 支持增量拉取日志
     */
    private Integer fromLineNum;

    /**
     * 结束行号
     *
     * 标识本次读取的结束位置
     */
    private Integer toLineNum;

    /**
     * 日志内容
     *
     * 承载实际日志文本
     */
    private String logContent;

    /**
     * 是否结束
     *
     * 标识是否还有后续日志
     */
    private Boolean end;
}
