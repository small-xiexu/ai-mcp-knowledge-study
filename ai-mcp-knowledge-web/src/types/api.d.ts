/**
 * 通用响应结构
 */
export interface Result<T = any> {
  code: number
  message: string
  data: T
  success: boolean
}

/**
 * 分页响应结构
 */
export interface PageResult<T> {
  records: T[]
  total: number
  pageNum: number
  pageSize: number
  totalPages?: number
  hasNext?: boolean
  hasPrevious?: boolean
}

/**
 * 分页请求参数
 */
export interface PageRequest {
  pageNum: number
  pageSize: number
  sortField?: string
  sortOrder?: string
}
