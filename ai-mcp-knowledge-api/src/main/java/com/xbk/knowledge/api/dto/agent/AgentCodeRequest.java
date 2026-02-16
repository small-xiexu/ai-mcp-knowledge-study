package com.xbk.knowledge.api.dto.agent;

import com.xbk.knowledge.types.common.BaseRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Agent 详情查询请求（按 agentCode）。
 
  * @author xiexu
  */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AgentCodeRequest extends BaseRequest {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "agentCode 不能为空")
    private String agentCode;
}

