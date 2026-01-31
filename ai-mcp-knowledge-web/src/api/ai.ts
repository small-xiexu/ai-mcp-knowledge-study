import request from '@/utils/request'
import type { AIRequest, AIResponse, ModelInfo } from '@/types/entity'

/**
 * 通用 AI 调用
 */
export const chat = (data: AIRequest) =>
  request.post<AIResponse>('/ai/chat', data)

/**
 * 获取可用模型列表
 */
export const getAvailableModels = () =>
  request.post<ModelInfo[]>('/ai/models')

/**
 * 获取推荐模型
 */
export const getRecommendedModel = (taskType?: string) =>
  request.post<ModelInfo>('/ai/models/recommend', { taskType })
