package com.xbk.knowledge.domain.model.vo.task;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 任务类型编码查询条件值对象
 * 统一承载任务类型编码查询条件
 *
 * 职责：领域值对象，用于表达查询条件语义
 * @author xiexu
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskTypeCodeQuery {

    /**
     * 任务类型编码
     */
    private String taskCode;
}
