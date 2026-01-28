import request from '@/utils/request'
import type { CallLog } from '@/types/entity'
import type { PageResult, PageRequest } from '@/types/api'

/**
 * 查询审计日志列表（分页）
 */
export const getAuditLogList = (params: PageRequest & { traceId?: string; modelId?: number }) =>
  request.get<PageResult<CallLog>>('/audit/logs', { params })
