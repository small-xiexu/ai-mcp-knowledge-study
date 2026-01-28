import request from '@/utils/request'
import type { ModelConfig } from '@/types/entity'
import type { PageResult, PageRequest } from '@/types/api'

/**
 * 查询模型列表（分页）
 */
export const getModelList = (params: PageRequest & { modelName?: string }) =>
  request.get<PageResult<ModelConfig>>('/models', { params })

/**
 * 查询单个模型
 */
export const getModelById = (id: number) =>
  request.get<ModelConfig>(`/models/${id}`)

/**
 * 创建模型
 */
export const createModel = (data: Partial<ModelConfig>) =>
  request.post<ModelConfig>('/models', data)

/**
 * 更新模型
 */
export const updateModel = (id: number, data: Partial<ModelConfig>) =>
  request.put<ModelConfig>(`/models/${id}`, data)

/**
 * 删除模型
 */
export const deleteModel = (id: number) =>
  request.delete<void>(`/models/${id}`)

/**
 * 启用模型
 */
export const enableModel = (id: number) =>
  request.put<ModelConfig>(`/models/${id}/enable`)

/**
 * 禁用模型
 */
export const disableModel = (id: number) =>
  request.put<ModelConfig>(`/models/${id}/disable`)
