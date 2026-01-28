package com.xbk.knowledge.api;

import com.xbk.knowledge.types.common.Result;
import com.xbk.knowledge.api.dto.CallMetricsDTO;
import com.xbk.knowledge.api.dto.ModelUsageDTO;
import com.xbk.knowledge.api.dto.ResponseTimeDTO;
import com.xbk.knowledge.api.dto.SuccessRateDTO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 监控统计服务接口
 * 定义监控统计的 API 契约
 *
 * @author xiexu
 */
public interface IMetricsService {

    /**
     * 调用次数统计
     *
     * @param modelId   模型ID（可选）
     * @param taskType  任务类型（可选）
     * @param startTime 开始时间（可选）
     * @param endTime   结束时间（可选）
     * @return 调用次数统计
     */
    Result<CallMetricsDTO> getCallMetrics(Long modelId, String taskType,
                                          LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 成功率统计
     *
     * @param modelId   模型ID（可选）
     * @param taskType  任务类型（可选）
     * @param startTime 开始时间（可选）
     * @param endTime   结束时间（可选）
     * @return 成功率统计
     */
    Result<SuccessRateDTO> getSuccessRate(Long modelId, String taskType,
                                          LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 响应时间统计
     *
     * @param modelId   模型ID（可选）
     * @param taskType  任务类型（可选）
     * @param startTime 开始时间（可选）
     * @param endTime   结束时间（可选）
     * @return 响应时间统计
     */
    Result<ResponseTimeDTO> getResponseTime(Long modelId, String taskType,
                                            LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 模型使用分布
     *
     * @param startTime 开始时间（可选）
     * @param endTime   结束时间（可选）
     * @return 模型使用分布
     */
    Result<List<ModelUsageDTO>> getModelUsage(LocalDateTime startTime, LocalDateTime endTime);
}
