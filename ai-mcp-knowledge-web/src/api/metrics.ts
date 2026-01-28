import request from '@/utils/request'

/**
 * 调用次数统计 DTO
 */
export interface CallMetricsDTO {
  totalCalls: number
  successCalls: number
  failedCalls: number
  timeRange: string
}

/**
 * 成功率统计 DTO
 */
export interface SuccessRateDTO {
  modelId: number
  modelName: string
  totalCalls: number
  successCalls: number
  successRate: number
}

/**
 * 响应时间统计 DTO
 */
export interface ResponseTimeDTO {
  modelId: number
  modelName: string
  avgResponseTime: number
  minResponseTime: number
  maxResponseTime: number
}

/**
 * 模型使用分布 DTO
 */
export interface ModelUsageDTO {
  modelId: number
  modelName: string
  callCount: number
  percentage: number
}

/**
 * 获取调用次数统计
 */
export const getCallMetrics = (params?: { modelId?: number; startTime?: string; endTime?: string }) =>
  request.get<CallMetricsDTO>('/metrics/calls', { params })

/**
 * 获取成功率统计
 */
export const getSuccessRate = (params?: { startTime?: string; endTime?: string }) =>
  request.get<SuccessRateDTO[]>('/metrics/success-rate', { params })

/**
 * 获取响应时间统计
 */
export const getResponseTime = (params?: { startTime?: string; endTime?: string }) =>
  request.get<ResponseTimeDTO[]>('/metrics/response-time', { params })

/**
 * 获取模型使用分布
 */
export const getModelUsage = (params?: { startTime?: string; endTime?: string }) =>
  request.get<ModelUsageDTO[]>('/metrics/model-usage', { params })
