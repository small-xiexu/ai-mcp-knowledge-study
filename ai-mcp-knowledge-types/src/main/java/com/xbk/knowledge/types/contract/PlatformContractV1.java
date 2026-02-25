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
 * 约束无论成功/失败，都必须返回可解析结构。
 *
 * @author sxie
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformContractV1 implements Serializable {

    /**
     * 序列化版本号。
     */
    private static final long serialVersionUID = 1L;

    /**
     * 元信息。
     */
    private Meta meta;

    /**
     * 主回答内容。
     */
    @Builder.Default
    private String answer = "";

    /**
     * 不确定性说明。
     */
    @Builder.Default
    private String uncertainty = "";

    /**
     * 引用列表。
     */
    @Builder.Default
    private List<Citation> citations = new ArrayList<>();

    /**
     * 工具调用摘要列表。
     */
    @Builder.Default
    private List<ToolCall> toolCalls = new ArrayList<>();

    /**
     * 下一步行动建议列表。
     */
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

    /**
     * 错误信息对象。
     */
    private Error error;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Meta implements Serializable {
        /**
         * 序列化版本号。
         */
        private static final long serialVersionUID = 1L;

        /**
         * 运行ID。
         */
        private String runId;

        /**
         * Agent 编码。
         */
        private String agentCode;

        /**
         * Agent 版本ID。
         */
        private Long agentVersionId;

        /**
         * Agent 版本号。
         */
        private Integer agentVersionNo;

        /**
         * 使用的模型标识。
         */
        private String modelUsed;

        /**
         * 总耗时（毫秒）。
         */
        private Long costMs;

        /**
         * 修复重试次数。
         */
        private Integer repairAttempts;

        /**
         * Workflow ID（可选）。
         */
        private Long workflowId;

        /**
         * Workflow 编码（可选）。
         */
        private String workflowCode;

        /**
         * Workflow 版本ID（可选）。
         */
        private Long workflowVersionId;

        /**
         * Workflow 版本号（可选）。
         */
        private Integer workflowVersionNo;

        /**
         * 审批请求ID（待审批时必填）。
         */
        private Long approvalRequestId;

        /**
         * 待审批工具标识（待审批时必填）。
         */
        private String pendingToolKey;

        /**
         * 风险等级（待审批时必填）。
         */
        private String riskLevel;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Citation implements Serializable {
        /**
         * 序列化版本号。
         */
        private static final long serialVersionUID = 1L;

        /**
         * 引用标题。
         */
        private String title;

        /**
         * 引用片段。
         */
        private String snippet;

        /**
         * 引用来源。
         */
        private String source;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToolCall implements Serializable {
        /**
         * 序列化版本号。
         */
        private static final long serialVersionUID = 1L;

        /**
         * 工具标识。
         */
        private String toolKey;

        /**
         * 调用摘要。
         */
        private String summary;

        /**
         * 结果片段。
         */
        private String resultSnippet;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Error implements Serializable {
        /**
         * 序列化版本号。
         */
        private static final long serialVersionUID = 1L;

        /**
         * 错误码。
         */
        private String code;

        /**
         * 错误消息。
         */
        private String message;

        /**
         * 错误详情。
         */
        private String detail;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StepTrace implements Serializable {
        /**
         * 序列化版本号。
         */
        private static final long serialVersionUID = 1L;

        /**
         * 节点键。
         */
        private String nodeKey;

        /**
         * 节点类型。
         */
        private String nodeType;

        /**
         * 节点名称。
         */
        private String nodeName;

        /**
         * 节点状态。
         */
        private String status;

        /**
         * 节点耗时（毫秒）。
         */
        private Long costMs;

        /**
         * 工具调用次数。
         */
        private Integer toolCallCount;

        /**
         * 工具拒绝次数。
         */
        private Integer toolDeniedCount;

        /**
         * 输入摘要。
         */
        private String inputDigest;

        /**
         * 输出摘要。
         */
        private String outputDigest;

        /**
         * 输出文本（可能截断）。
         */
        private String outputText;

        /**
         * 输出是否已截断。
         */
        private Boolean outputTruncated;

        /**
         * 关联审批请求ID。
         */
        private Long approvalRequestId;

        /**
         * 错误消息。
         */
        private String errorMessage;
    }
}
