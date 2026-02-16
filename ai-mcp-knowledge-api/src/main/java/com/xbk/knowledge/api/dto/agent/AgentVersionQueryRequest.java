package com.xbk.knowledge.api.dto.agent;

import com.xbk.knowledge.types.common.PageRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AgentVersion 列表查询请求。
 
  * @author xiexu
  */
@Data
@EqualsAndHashCode(callSuper = true)
public class AgentVersionQueryRequest extends PageRequest {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "agentCode 不能为空")
    private String agentCode;
}
