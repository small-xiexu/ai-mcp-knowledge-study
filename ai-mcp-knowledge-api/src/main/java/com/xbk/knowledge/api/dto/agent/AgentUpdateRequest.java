package com.xbk.knowledge.api.dto.agent;

import com.xbk.knowledge.types.common.BaseRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Agent 更新参数（按 agentCode 定位）。
 *
 * @author sxie
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AgentUpdateRequest extends BaseRequest {

    /**
     * 序列化版本号
     */
    private static final long serialVersionUID = 1L;

    /**
     * Agent 编码
     */
    @NotBlank(message = "agentCode 不能为空")
    private String agentCode;

    /**
     * Agent 名称
     */
    @NotBlank(message = "agentName 不能为空")
    private String agentName;

    /**
     * 描述
     */
    private String description;

    /**
     * 调用通道：agent/chat_stream（可选）。
     */
    private String channel;

    /**
     * ENABLED/DISABLED（可选）。
     */
    private String status;
}
