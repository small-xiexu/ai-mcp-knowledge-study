package com.xbk.knowledge.api.dto.client;

import com.xbk.knowledge.types.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Client Profile 分页查询参数。
 *
 * @author sxie
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ClientProfileQueryRequest extends PageRequest {

    /**
     * 序列化版本号
     */
    private static final long serialVersionUID = 1L;

    /**
     * 关键字
     */
    private String keyword;

    /**
     * 状态
     */
    private String status;
}
