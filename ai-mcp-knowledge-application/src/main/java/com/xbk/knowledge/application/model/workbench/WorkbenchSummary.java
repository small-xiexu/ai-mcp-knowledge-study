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

    private ModelInfo model;

    private AgentInfo agent;

    private PromptInfo prompt;

    private ToolInfo tool;

    private ScheduleInfo schedule;

    private KnowledgeInfo knowledge;

    private TodoInfo todo;

    private List<GuideStep> guideSteps;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModelInfo {
        private Long total;
        private Long enabled;
        private Long activeChatModelId;
        private Long activeEmbeddingModelId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AgentInfo {
        private Long total;
        private Long published;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PromptInfo {
        private Long draft;
        private Long published;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToolInfo {
        private Long approvalsPending;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScheduleInfo {
        private Long total;
        private Long enabled;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KnowledgeInfo {
        private Long ragTagCount;
        private Long ragTaskTotal;
        private Long ragTaskProcessing;
        private Long ragTaskFailedRecent;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TodoInfo {
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
         * 状态：DONE/TODO/BLOCKED。
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
         * 是否写操作步骤（用于超管未选 scope 时标红）。
         */
        private boolean writeAction;
    }
}
