import request from '@/utils/request'
import type {
  CallMetricsDTO,
  ModelUsageDTO,
  ResponseTimeDTO,
  SuccessRateDTO
} from '@/types/entity'

export interface MetricsQueryRequest {
  modelId?: number
  startTime?: string
  endTime?: string
}

export interface ModelUsageQueryRequest {
  startTime?: string
  endTime?: string
}

/**
 * 获取调用次数统计
 */
export const getCallMetrics = (data: MetricsQueryRequest = {}) =>
  request.post<CallMetricsDTO>('/metrics/calls', data)

/**
 * 获取成功率统计
 */
export const getSuccessRate = (data: MetricsQueryRequest = {}) =>
  request.post<SuccessRateDTO>('/metrics/success-rate', data)

/**
 * 获取响应时间统计
 */
export const getResponseTime = (data: MetricsQueryRequest = {}) =>
  request.post<ResponseTimeDTO>('/metrics/response-time', data)

/**
 * 获取模型使用分布
 */
export const getModelUsage = (data: ModelUsageQueryRequest = {}) =>
  request.post<ModelUsageDTO[]>('/metrics/model-usage', data)
