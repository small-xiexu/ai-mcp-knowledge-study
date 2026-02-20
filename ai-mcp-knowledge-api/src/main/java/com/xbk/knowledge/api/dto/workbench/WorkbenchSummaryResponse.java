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
        private String key;
        private String title;
        private String status;
        private String message;
        private String actionPath;
        private String actionLabel;
        private boolean writeAction;
    }
}
