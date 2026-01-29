package com.xbk.knowledge.api.dto.task;

import com.xbk.knowledge.types.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 任务类型查询请求
 * 用于分页查询任务类型列表
 *
 * 职责：接口层 DTO，用于承载请求/响应参数并保证传输边界稳定
 * @author xiexu
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TaskTypeQueryRequest extends PageRequest {

    private static final long serialVersionUID = 1L;
}
