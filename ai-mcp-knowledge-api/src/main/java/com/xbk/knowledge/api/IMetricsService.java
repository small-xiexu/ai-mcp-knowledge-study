package com.xbk.knowledge.api;

import com.xbk.knowledge.types.common.Result;
import com.xbk.knowledge.api.dto.metrics.CallMetricsDTO;
import com.xbk.knowledge.api.dto.metrics.MetricsQueryRequest;
import com.xbk.knowledge.api.dto.metrics.ModelUsageDTO;
import com.xbk.knowledge.api.dto.metrics.ModelUsageQueryRequest;
import com.xbk.knowledge.api.dto.metrics.ResponseTimeDTO;
import com.xbk.knowledge.api.dto.metrics.SuccessRateDTO;

import java.util.List;
import jakarta.validation.Valid;

/**
 * 监控统计服务接口
 * 定义监控统计的 API 契约
 *
 * 职责：接口契约，用于规范 Trigger 层对外服务
 * @author xiexu
 */
public interface IMetricsService {

    /**
     * 调用次数统计
     *
     * @param request 监控统计查询请求
     * @return 调用次数统计
     */
    Result<CallMetricsDTO> getCallMetrics(@Valid MetricsQueryRequest request);

    /**
     * 成功率统计
     *
     * @param request 监控统计查询请求
     * @return 成功率统计
     */
    Result<SuccessRateDTO> getSuccessRate(@Valid MetricsQueryRequest request);

    /**
     * 响应时间统计
     *
     * @param request 监控统计查询请求
     * @return 响应时间统计
     */
    Result<ResponseTimeDTO> getResponseTime(@Valid MetricsQueryRequest request);

    /**
     * 模型使用分布
     *
     * @param request 模型使用情况查询请求
     * @return 模型使用分布
     */
    Result<List<ModelUsageDTO>> getModelUsage(@Valid ModelUsageQueryRequest request);
}
