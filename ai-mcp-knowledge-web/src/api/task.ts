import request from '@/utils/request'
import type { TaskType, TaskTypeRequest } from '@/types/entity'
import type { PageRequest, PageResult } from '@/types/api'

/**
 * 查询任务类型列表（分页）
 */
export const getTaskTypeList = (data: PageRequest) =>
  request.post<PageResult<TaskType>>('/task-types/list', data)

/**
 * 查询任务类型详情
 */
export const getTaskTypeById = (id: number) =>
  request.post<TaskType>('/task-types/get', { id })

/**
 * 按编码查询任务类型
 */
export const getTaskTypeByCode = (code: string) =>
  request.post<TaskType>('/task-types/get-by-code', { code })

/**
 * 创建任务类型
 */
export const createTaskType = (data: TaskTypeRequest) =>
  request.post<TaskType>('/task-types/create', data)

/**
 * 更新任务类型
 */
export const updateTaskType = (data: TaskTypeRequest) =>
  request.post<TaskType>('/task-types/update', data)

/**
 * 删除任务类型
 */
export const deleteTaskType = (id: number) =>
  request.post<void>('/task-types/delete', { id })
