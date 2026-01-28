import request from '@/utils/request'
import type { AIRequest, AIResponse, ModelInfo } from '@/types/entity'

/**
 * 通用 AI 调用
 */
export const chat = (data: AIRequest) =>
  request.post<AIResponse>('/ai/chat', data)

/**
 * 按任务类型调用 AI
 */
export const chatByTaskType = (taskType: string, data: AIRequest) =>
  request.post<AIResponse>(`/ai/chat/by-task/${taskType}`, data)

/**
 * 获取可用模型列表
 */
export const getAvailableModels = () =>
  request.get<ModelInfo[]>('/ai/models/available')

/**
 * 获取推荐模型
 */
export const getRecommendedModel = (taskType?: string) =>
  request.get<ModelInfo>('/ai/models/recommended', { params: { taskType } })
