package com.xbk.knowledge.api.dto.xxl;

import com.xbk.knowledge.types.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * XXL 任务日志查询请求
 * 用于按任务与时间范围分页查询日志
 *
 * 职责：接口层 DTO，用于承载请求参数并保证传输边界稳定
 * @author sxie
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class XxlJobLogListRequest extends PageRequest {

    /**
     * 序列化版本号
     */
    private static final long serialVersionUID = 1L;

    /**
     * 任务 ID
     */
    private Long jobId;

    /**
     * 起始时间（格式yyyy-MM-dd HH:mm:ss）
     */
    private String startTime;

    /**
     * 结束时间（格式yyyy-MM-dd HH:mm:ss）
     */
    private String endTime;
}
