import request from '@/utils/request'
import type { PageRequest, PageResult } from '@/types/api'
import type {
  XxlJob,
  XxlJobCreateRequest,
  XxlJobDetail,
  XxlJobLog,
  XxlJobLogDetail,
  XxlJobLogListRequest,
  XxlJobTriggerRequest,
  XxlJobUpdateRequest
} from '@/types/entity'

/**
 * 查询任务列表（分页）
 */
export const getXxlJobList = (data: PageRequest & { appName?: string }) =>
  request.post<PageResult<XxlJob>>('/xxl/jobs/list', data)

/**
 * 查询任务下拉列表（缓存）
 */
export const getXxlJobOptions = (refresh = false) =>
  request.post<XxlJob[]>('/xxl/jobs/options', { refresh })

/**
 * 查询任务详情
 */
export const getXxlJobDetail = (id: number) =>
  request.post<XxlJobDetail>('/xxl/jobs/detail', { id })

/**
 * 创建任务
 */
export const createXxlJob = (data: XxlJobCreateRequest) =>
  request.post<string>('/xxl/jobs/create', data)

/**
 * 更新任务
 */
export const updateXxlJob = (data: XxlJobUpdateRequest) =>
  request.post<void>('/xxl/jobs/update', data)

/**
 * 删除任务
 */
export const removeXxlJob = (id: number) =>
  request.post<void>('/xxl/jobs/remove', { id })

/**
 * 启动任务
 */
export const startXxlJob = (id: number) =>
  request.post<void>('/xxl/jobs/start', { id })

/**
 * 停止任务
 */
export const stopXxlJob = (id: number) =>
  request.post<void>('/xxl/jobs/stop', { id })

/**
 * 手动触发任务
 */
export const triggerXxlJob = (data: XxlJobTriggerRequest) =>
  request.post<void>('/xxl/jobs/trigger', data)

/**
 * 查询日志列表（分页）
 */
export const getXxlJobLogList = (data: XxlJobLogListRequest) =>
  request.post<PageResult<XxlJobLog>>('/xxl/logs/list', data)

/**
 * 查询日志详情
 */
export const getXxlJobLogDetail = (logId: number, fromLineNum = 0) =>
  request.post<XxlJobLogDetail>('/xxl/logs/detail', { logId, fromLineNum })
