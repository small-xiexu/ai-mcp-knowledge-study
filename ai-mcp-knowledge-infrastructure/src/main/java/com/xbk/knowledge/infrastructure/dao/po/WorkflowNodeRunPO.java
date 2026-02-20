package com.xbk.knowledge.infrastructure.dao.po;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Workflow 节点运行明细实体。
 *
 * 对应表：workflow_node_run
 *
 * @author sxie
 */
@TableName("workflow_node_run")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowNodeRunPO {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String runId;

    private String nodeKey;

    private String nodeType;

    private String nodeName;

    private String status;

    private Long modelIdUsed;

    private String modelNameUsed;

    private Integer promptTokens;

    private Integer completionTokens;

    private Integer totalTokens;

    private Integer toolCallCount;

    private Integer toolDeniedCount;

    private String inputDigest;

    private String outputDigest;

    private String outputText;

    private Integer outputTruncated;

    private Long approvalRequestId;

    private Long costMs;

    private String errorMessage;

    private LocalDateTime startedAt;

    private LocalDateTime endedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}

