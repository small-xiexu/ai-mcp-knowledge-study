import request from '@/utils/request'
import type { ModelConfig, ModelConfigRequest } from '@/types/entity'
import type { PageResult, PageRequest } from '@/types/api'

/**
 * 查询模型列表（分页）
 */
export const getModelList = (data: PageRequest) =>
  request.post<PageResult<ModelConfig>>('/models/list', data)

/**
 * 查询单个模型
 */
export const getModelById = (id: number) =>
  request.post<ModelConfig>('/models/get', { id })

/**
 * 创建模型
 */
export const createModel = (data: ModelConfigRequest) =>
  request.post<ModelConfig>('/models/create', data)

/**
 * 更新模型
 */
export const updateModel = (data: ModelConfigRequest) =>
  request.post<ModelConfig>('/models/update', data)

/**
 * 删除模型
 */
export const deleteModel = (id: number) =>
  request.post<void>('/models/delete', { id })

/**
 * 启用模型
 */
export const enableModel = (id: number) =>
  request.post<ModelConfig>('/models/enable', { id })

/**
 * 禁用模型
 */
export const disableModel = (id: number) =>
  request.post<ModelConfig>('/models/disable', { id })
