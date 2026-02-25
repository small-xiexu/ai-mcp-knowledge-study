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
 * 对应表workflow_node_run
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

    /**
     * 主键 ID。
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 运行 ID。
     */
    private String runId;

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
     * 使用模型 ID。
     */
    private Long modelIdUsed;

    /**
     * 使用模型名称。
     */
    private String modelNameUsed;

    /**
     * 输入 token 数。
     */
    private Integer promptTokens;

    /**
     * 输出 token 数。
     */
    private Integer completionTokens;

    /**
     * 总 token 数。
     */
    private Integer totalTokens;

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
     * 输出全文。
     */
    private String outputText;

    /**
     * 输出是否截断（0/1）。
     */
    private Integer outputTruncated;

    /**
     * 审批请求 ID。
     */
    private Long approvalRequestId;

    /**
     * 节点耗时（毫秒）。
     */
    private Long costMs;

    /**
     * 错误信息。
     */
    private String errorMessage;

    /**
     * 开始时间。
     */
    private LocalDateTime startedAt;

    /**
     * 结束时间。
     */
    private LocalDateTime endedAt;

    /**
     * 创建时间。
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间。
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
