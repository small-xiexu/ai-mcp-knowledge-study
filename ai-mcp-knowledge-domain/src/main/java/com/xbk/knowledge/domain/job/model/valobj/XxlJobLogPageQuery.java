package com.xbk.knowledge.domain.job.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * XXL 任务日志分页查询条件值对象
 * 统一承载任务与时间范围条件
 *
 * 职责：领域值对象，用于表达查询条件语义
 * @author sxie
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class XxlJobLogPageQuery {

    /**
     * 执行器 AppName
     *
     * 限定查询范围到指定执行器
     */
    private String appName;

    /**
     * 任务 ID
     *
     * 按任务过滤日志
     */
    private Long jobId;

    /**
     * 起始时间（格式yyyy-MM-dd HH:mm:ss）
     *
     * 限定日志查询时间范围
     */
    private String startTime;

    /**
     * 结束时间（格式yyyy-MM-dd HH:mm:ss）
     *
     * 限定日志查询时间范围
     */
    private String endTime;

    /**
     * 当前页码
     *
     * 分页查询需要页码
     */
    private Integer pageNum;

    /**
     * 每页大小
     *
     * 控制单次返回数量
     */
    private Integer pageSize;
}
