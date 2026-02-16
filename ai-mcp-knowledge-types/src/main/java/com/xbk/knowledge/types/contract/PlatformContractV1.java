package com.xbk.knowledge.types.contract;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 平台标准输出结构（Platform Contract v1）。
 *
 * 约束：无论成功/失败，都必须返回可解析结构。
 
  * @author xiexu
  */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformContractV1 implements Serializable {

    private static final long serialVersionUID = 1L;

    private Meta meta;

    @Builder.Default
    private String answer = "";

    @Builder.Default
    private String uncertainty = "";

    @Builder.Default
    private List<Citation> citations = new ArrayList<>();

    @Builder.Default
    private List<ToolCall> toolCalls = new ArrayList<>();

    @Builder.Default
    private List<String> actionsNext = new ArrayList<>();

    /**
     * 运行明细（面向用户可见）。
     *
     * 说明：
     * - Agent/Workflow 均可填充
     * - outputText 可能被截断或脱敏
     */
    @Builder.Default
    private List<StepTrace> steps = new ArrayList<>();

    /**
     * SUCCESS/FAILED/PENDING_APPROVAL。
     */
    private String status;

    private Error error;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Meta implements Serializable {
        private static final long serialVersionUID = 1L;

        private String runId;
        private String agentCode;
        private Long agentVersionId;
        private Integer agentVersionNo;
        private Long orgId;

        private String modelUsed;
        private Long costMs;
        private Integer repairAttempts;

        // Workflow 相关（Workflow 场景可选）
        private Long workflowId;
        private String workflowCode;
        private Long workflowVersionId;
        private Integer workflowVersionNo;

        // 审批相关（PENDING_APPROVAL 时必填）
        private Long approvalRequestId;
        private String pendingToolKey;
        private String riskLevel;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Citation implements Serializable {
        private static final long serialVersionUID = 1L;

        private String title;
        private String snippet;
        private String source;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToolCall implements Serializable {
        private static final long serialVersionUID = 1L;

        private String toolKey;
        private String summary;
        private String resultSnippet;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Error implements Serializable {
        private static final long serialVersionUID = 1L;

        private String code;
        private String message;
        private String detail;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StepTrace implements Serializable {
        private static final long serialVersionUID = 1L;

        private String nodeKey;
        private String nodeType;
        private String nodeName;
        private String status;

        private Long costMs;

        private Integer promptTokens;
        private Integer completionTokens;
        private Integer totalTokens;

        private Integer toolCallCount;
        private Integer toolDeniedCount;

        private String inputDigest;
        private String outputDigest;

        private String outputText;
        private Boolean outputTruncated;

        private Long approvalRequestId;
        private String errorMessage;
    }
}
