import request from '@/utils/request'
import type { WorkbenchSummary } from '@/types/workbench'

/**
 * 获取工作台汇总信息（方案B：后端聚合）。
 */
export const getWorkbenchSummary = () =>
  request.post<WorkbenchSummary>('/workbench/summary', {})

