package com.xbk.knowledge.api.dto.agent;

import com.xbk.knowledge.types.common.BaseRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Agent 运行入口请求（供调度/内部任务触发）。
 *
 * @author sxie
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AgentRuntimeInvokeRequest extends BaseRequest {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "content 不能为空")
    private String content;

    private Long sessionId;

    private String ragTagsJson;
}
