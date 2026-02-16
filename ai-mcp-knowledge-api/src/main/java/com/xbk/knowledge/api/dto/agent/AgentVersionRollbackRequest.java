package com.xbk.knowledge.api.dto.agent;

import com.xbk.knowledge.types.common.BaseRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * AgentVersion 回滚请求。
 
  * @author xiexu
  */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AgentVersionRollbackRequest extends BaseRequest {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "agentCode 不能为空")
    private String agentCode;

    @NotNull(message = "targetVersionId 不能为空")
    private Long targetVersionId;
}

