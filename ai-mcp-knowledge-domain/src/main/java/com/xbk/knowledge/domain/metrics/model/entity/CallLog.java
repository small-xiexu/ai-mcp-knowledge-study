package com.xbk.knowledge.domain.metrics.model.entity;

import com.xbk.knowledge.types.enums.CallStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 调用日志实体
 * 对应数据库表ai_call_log
 *
 * 职责：领域实体，用于承载核心业务状态与生命周期
 * @author sxie
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CallLog {

    /**
     * 主键ID
     *
     * 用于持久化唯一标识
     */
    private Long id;

    /**
     * 模型ID
     *
     * 定位调用使用的模型配置
     */
    private Long modelId;

    /**
     * 请求内容
     *
     * 保留请求上下文用于排查
     */
    private String requestContent;

    /**
     * 响应内容
     *
     * 保留响应用于回溯与分析
     */
    private String responseContent;

    /**
     * 使用token数
     *
     * 用于成本与限额统计
     */
    private Integer tokensUsed;

    /**
     * 响应时间（毫秒）
     *
     * 用于性能监控与告警
     */
    private Long responseTime;

    /**
     * 状态（SUCCESS/FAILED/FALLBACK）
     *
     * 用于成功率与降级统计
     */
    private CallStatus status;

    /**
     * 错误信息
     *
     * 记录失败原因便于排查
     */
    private String errorMessage;

    /**
     * 创建时间
     *
     * 用于时序分析与审计
     */
    private LocalDateTime createdAt;
}
