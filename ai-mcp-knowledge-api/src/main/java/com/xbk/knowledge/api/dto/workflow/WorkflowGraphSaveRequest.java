package com.xbk.knowledge.api.dto.workflow;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Workflow 图保存请求（全量替换 nodes/edges）。
 *
 * @author sxie
 */
@Data
public class WorkflowGraphSaveRequest {

    /**
     * Workflow 版本ID
     */
    @NotNull(message = "workflowVersionId 不能为空")
    private Long workflowVersionId;

    /**
     * 前端画布快照（用于回显），可为空。
     */
    private String graphJson;

    /**
     * 默认Config JSON
     */
    private String defaultConfigJson;

    private List<Node> nodes = new ArrayList<>();

    private List<Edge> edges = new ArrayList<>();

    @Data
    public static class Node {
        /**
         * 节点Key
         */
        @NotBlank(message = "nodeKey 不能为空")
        private String nodeKey;
        /**
         * 节点类型
         */
        @NotBlank(message = "nodeType 不能为空")
        private String nodeType;
        /**
         * 节点名称
         */
        private String nodeName;
        /**
         * config JSON
         */
        private String configJson;
        /**
         * positionX
         */
        private Integer positionX;
        /**
         * positionY
         */
        private Integer positionY;
    }

    @Data
    public static class Edge {
        /**
         * 来源键
         */
        @NotBlank(message = "sourceKey 不能为空")
        private String sourceKey;
        /**
         * 目标键
         */
        @NotBlank(message = "targetKey 不能为空")
        private String targetKey;
        /**
         * 连线类型
         */
        private String edgeType;
        /**
         * conditionExpr
         */
        private String conditionExpr;
    }
}

