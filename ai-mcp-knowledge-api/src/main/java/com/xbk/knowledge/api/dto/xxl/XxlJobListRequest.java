package com.xbk.knowledge.api.dto.xxl;

import com.xbk.knowledge.types.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * XXL 任务列表查询请求
 * 用于分页查询指定执行器下的任务
 *
 * 职责：接口层 DTO，用于承载请求参数并保证传输边界稳定
 * @author sxie
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class XxlJobListRequest extends PageRequest {

    private static final long serialVersionUID = 1L;

    /**
     * 执行器 AppName
     */
    private String appName;

    /**
     * 是否强制刷新缓存
     */
    private Boolean refresh;
}
