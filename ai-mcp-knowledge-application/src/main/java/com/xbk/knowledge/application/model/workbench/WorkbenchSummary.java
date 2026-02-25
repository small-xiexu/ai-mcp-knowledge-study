package com.xbk.knowledge.application.model.workbench;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 工作台汇总信息（多 Agent + 治理视角）。
 *
 * 职责：
 * - 聚合展示“多 Agent 平台”从配置到发布到治理的关键指标
 * - 为前端工作台提供“一次请求拿到全量状态”的数据模型（方案B）
 *
 * 说明：该模型为应用层聚合输出，不直接映射数据库表。
 *
 * @author sxie
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkbenchSummary {

    /**
     * 模型汇总信息。
     */
    private ModelInfo model;

    /**
     * Agent 汇总信息。
     */
    private AgentInfo agent;

    /**
     * Prompt 汇总信息。
     */
    private PromptInfo prompt;

    /**
     * 工具汇总信息。
     */
    private ToolInfo tool;

    /**
     * 调度汇总信息。
     */
    private ScheduleInfo schedule;

    /**
     * 知识库汇总信息。
     */
    private KnowledgeInfo knowledge;

    /**
     * 待办汇总信息。
     */
    private TodoInfo todo;

    /**
     * 引导步骤列表。
     */
    private List<GuideStep> guideSteps;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModelInfo {

        /**
         * 模型总数。
         */
        private Long total;

        /**
         * 启用模型数。
         */
        private Long enabled;

        /**
         * 当前激活对话模型 ID。
         */
        private Long activeChatModelId;

        /**
         * 当前激活向量模型 ID。
         */
        private Long activeEmbeddingModelId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AgentInfo {

        /**
         * Agent 总数。
         */
        private Long total;

        /**
         * 已发布 Agent 数。
         */
        private Long published;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PromptInfo {

        /**
         * 草稿 Prompt 数。
         */
        private Long draft;

        /**
         * 已发布 Prompt 数。
         */
        private Long published;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToolInfo {

        /**
         * 待审批工具调用数。
         */
        private Long approvalsPending;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScheduleInfo {

        /**
         * 调度总数。
         */
        private Long total;

        /**
         * 启用调度数。
         */
        private Long enabled;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KnowledgeInfo {

        /**
         * RAG 标签数。
         */
        private Long ragTagCount;

        /**
         * RAG 任务总数。
         */
        private Long ragTaskTotal;

        /**
         * 处理中 RAG 任务数。
         */
        private Long ragTaskProcessing;

        /**
         * 近期失败 RAG 任务数。
         */
        private Long ragTaskFailedRecent;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TodoInfo {

        /**
         * 超管是否被写操作门禁阻断。
         */
        private boolean writeBlockedForSuperAdmin;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GuideStep {
        /**
         * 步骤 key（前端用于渲染与跳转）。
         */
        private String key;

        /**
         * 标题。
         */
        private String title;

        /**
         * 状态DONE/TODO/BLOCKED。
         */
        private String status;

        /**
         * 辅助说明（为空则不展示）。
         */
        private String message;

        /**
         * 跳转路径（为空表示无跳转）。
         */
        private String actionPath;

        /**
         * 跳转按钮文案（为空表示不展示按钮）。
         */
        private String actionLabel;

        /**
         * 是否写操作步骤。
         */
        private boolean writeAction;
    }
}
