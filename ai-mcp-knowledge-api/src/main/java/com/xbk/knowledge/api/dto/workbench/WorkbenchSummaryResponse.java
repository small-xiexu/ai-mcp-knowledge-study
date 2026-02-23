package com.xbk.knowledge.api.dto.workbench;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 工作台汇总响应 DTO（方案B）。
 *
 * 职责：向前端提供一次请求拿到“多 Agent + 治理闭环”所需的关键指标与引导步骤。
 *
 * @author sxie
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkbenchSummaryResponse {

    /**
     * 模型
     */
    private ModelInfo model;

    /**
     * Agent
     */
    private AgentInfo agent;

    /**
     * 提示词
     */
    private PromptInfo prompt;

    /**
     * 工具
     */
    private ToolInfo tool;

    /**
     * 调度
     */
    private ScheduleInfo schedule;

    /**
     * knowledge
     */
    private KnowledgeInfo knowledge;

    /**
     * todo
     */
    private TodoInfo todo;

    /**
     * guideSteps
     */
    private List<GuideStep> guideSteps;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModelInfo {
        /**
         * 总数
         */
        private Long total;
        /**
         * 启用状态
         */
        private Long enabled;
        /**
         * 当前启用对话模型 ID
         */
        private Long activeChatModelId;
        /**
         * 当前启用向量模型 ID
         */
        private Long activeEmbeddingModelId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AgentInfo {
        /**
         * 总数
         */
        private Long total;
        /**
         * 已发布
         */
        private Long published;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PromptInfo {
        /**
         * 草稿数
         */
        private Long draft;
        /**
         * 已发布
         */
        private Long published;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToolInfo {
        /**
         * approvalsPending
         */
        private Long approvalsPending;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScheduleInfo {
        /**
         * 总数
         */
        private Long total;
        /**
         * 启用状态
         */
        private Long enabled;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KnowledgeInfo {
        /**
         * RAGTag数量
         */
        private Long ragTagCount;
        /**
         * RAGTask总数
         */
        private Long ragTaskTotal;
        /**
         * RAGTaskProcessing
         */
        private Long ragTaskProcessing;
        /**
         * RAGTaskFailedRecent
         */
        private Long ragTaskFailedRecent;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TodoInfo {
        /**
         * 是否限制超级管理员写操作
         */
        private boolean writeBlockedForSuperAdmin;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GuideStep {
        /**
         * 键
         */
        private String key;
        /**
         * 标题
         */
        private String title;
        /**
         * 状态
         */
        private String status;
        /**
         * 消息
         */
        private String message;
        /**
         * 操作路径
         */
        private String actionPath;
        /**
         * 操作文案
         */
        private String actionLabel;
        /**
         * 是否可写操作
         */
        private boolean writeAction;
    }
}
