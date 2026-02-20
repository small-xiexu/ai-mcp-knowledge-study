package com.xbk.knowledge.api.dto.xxl;

import lombok.Data;

/**
 * XXL 任务下拉请求
 * 用于任务选项列表获取
 *
 * 职责：接口层 DTO，用于承载请求参数并保证传输边界稳定
 * @author sxie
 */
@Data
public class XxlJobOptionRequest {

    private static final long serialVersionUID = 1L;

    /**
     * 是否强制刷新缓存
     */
    private Boolean refresh;
}
