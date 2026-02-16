package com.xbk.knowledge.api.dto.advisor;

import com.xbk.knowledge.types.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Advisor 分页查询请求。
 
  * @author xiexu
  */
@Data
@EqualsAndHashCode(callSuper = true)
public class AdvisorQueryRequest extends PageRequest {

    private static final long serialVersionUID = 1L;

    private String keyword;

    private Boolean enabled;

    private String advisorType;
}

