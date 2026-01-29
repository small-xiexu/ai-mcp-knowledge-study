package com.xbk.knowledge.domain.service;

import com.xbk.knowledge.domain.model.vo.CallMetrics;
import com.xbk.knowledge.domain.model.vo.ModelUsage;
import com.xbk.knowledge.domain.model.vo.ResponseTime;
import com.xbk.knowledge.domain.model.vo.SuccessRate;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 监控指标领域服务接口
 * 负责监控指标的业务逻辑处理
 *
 * 职责：领域服务接口，用于定义业务能力
 * @author xiexu
 */
public interface IMetricsDomainService {

    /**
     * 统计调用次数指标
     *
     * @param modelId   模型 ID（可选）
     * @param taskType  任务类型（可选）
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 调用次数统计
     */
    CallMetrics collectCallMetrics(Long modelId, String taskType,
                                    LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 统计成功率指标
     *
     * @param modelId   模型 ID（可选）
     * @param taskType  任务类型（可选）
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 成功率统计
     */
    SuccessRate collectSuccessRate(Long modelId, String taskType,
                                    LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 统计响应时间指标
     *
     * @param modelId   模型 ID（可选）
     * @param taskType  任务类型（可选）
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 响应时间统计
     */
    ResponseTime collectResponseTime(Long modelId, String taskType,
                                      LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 统计模型使用分布
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 模型使用分布列表
     */
    List<ModelUsage> collectModelUsage(LocalDateTime startTime, LocalDateTime endTime);
}
