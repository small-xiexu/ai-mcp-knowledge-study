package com.xbk.knowledge.domain.job.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * XXL 任务分页查询条件值对象
 * 统一承载执行器与分页参数
 *
 * 职责：领域值对象，用于表达查询条件语义
 * @author sxie
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class XxlJobPageQuery {

    /**
     * 执行器 AppName
     *
     * 为什么：限定查询范围到指定执行器
     */
    private String appName;

    /**
     * 当前页码
     *
     * 为什么：分页查询需要页码
     */
    private Integer pageNum;

    /**
     * 每页大小
     *
     * 为什么：控制单次返回数量
     */
    private Integer pageSize;
}
