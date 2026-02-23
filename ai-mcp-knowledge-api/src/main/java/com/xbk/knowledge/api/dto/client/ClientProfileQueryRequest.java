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

    private static final long serialVersionUID = 1L;

    private String keyword;

    private String status;
}
