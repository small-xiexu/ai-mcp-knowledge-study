import request from '@/utils/request'
import type { ConfigAudit } from '@/types/entity'
import type { PageResult, PageRequest } from '@/types/api'

/**
 * 查询审计日志列表（分页）
 */
export const getAuditLogList = (data: PageRequest & { tableName?: string }) =>
  request.post<PageResult<ConfigAudit>>('/audits/list', data)

/**
 * 查询审计表名列表
 */
export const getAuditTableNames = () =>
  request.post<string[]>('/audits/tables')
