package com.xbk.knowledge.api.dto.workflow;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Workflow 图保存请求（全量替换 nodes/edges）。
 
  * @author xiexu
  */
@Data
public class WorkflowGraphSaveRequest {

    @NotNull(message = "workflowVersionId 不能为空")
    private Long workflowVersionId;

    /**
     * 前端画布快照（用于回显），可为空。
     */
    private String graphJson;

    private String defaultConfigJson;

    private List<Node> nodes = new ArrayList<>();

    private List<Edge> edges = new ArrayList<>();

    @Data
    public static class Node {
        @NotBlank(message = "nodeKey 不能为空")
        private String nodeKey;
        @NotBlank(message = "nodeType 不能为空")
        private String nodeType;
        private String nodeName;
        private String configJson;
        private Integer positionX;
        private Integer positionY;
    }

    @Data
    public static class Edge {
        @NotBlank(message = "sourceKey 不能为空")
        private String sourceKey;
        @NotBlank(message = "targetKey 不能为空")
        private String targetKey;
        private String edgeType;
        private String conditionExpr;
    }
}

