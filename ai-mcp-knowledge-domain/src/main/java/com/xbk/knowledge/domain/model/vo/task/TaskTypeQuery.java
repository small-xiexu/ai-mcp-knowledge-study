package com.xbk.knowledge.domain.model.vo.task;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 任务类型查询条件值对象
 * 统一承载按任务类型筛选的查询条件
 *
 * 职责：领域值对象，用于表达查询条件语义
 * @author xiexu
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskTypeQuery {

    /**
     * 任务类型
     */
    private String taskType;
}
