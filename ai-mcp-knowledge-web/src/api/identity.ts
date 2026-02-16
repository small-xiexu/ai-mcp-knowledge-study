import request from '@/utils/request'
import type { PageResult } from '@/types/api'
import type {
  IdentityAuditEvent,
  IdentityPermission,
  IdentityRole,
  IdentityUser
} from '@/types/entity'

interface OffsetPageRequest {
  pageNum: number
  pageSize: number
}

const toOffsetPayload = <T extends OffsetPageRequest>(data: T) => {
  const { pageNum: rawPageNum, pageSize: rawPageSize, ...rest } = data
  const pageNum = rawPageNum > 0 ? rawPageNum : 1
  const pageSize = rawPageSize > 0 ? rawPageSize : 10
  return {
    ...rest,
    offset: (pageNum - 1) * pageSize,
    pageSize
  }
}

export interface UserListRequest extends OffsetPageRequest {
  username?: string
  status?: number
}

export const listIdentityUsers = (data: UserListRequest) =>
  request.post<PageResult<IdentityUser>>('/users/list', toOffsetPayload(data))

export const createIdentityUser = (data: {
  username: string
  displayName: string
  password: string
  email?: string
  mobile?: string
  status?: number
  superAdmin?: boolean
}) => request.post<IdentityUser>('/users/create', data)

export const updateIdentityUser = (data: {
  id: number
  displayName: string
  email?: string
  mobile?: string
  status: number
  superAdmin?: boolean
}) => request.post<IdentityUser>('/users/update', data)

export const resetIdentityUserPassword = (data: {
  userId: number
  password: string
}) => request.post<void>('/users/reset-password', data)

export const grantIdentityUserRoles = (data: {
  userId: number
  roleIds: number[]
}) => request.post<void>('/users/grant-roles', data)

export const getIdentityUserRoleIds = (data: { userId: number }) => request.post<number[]>('/users/role-ids', data)

export interface RoleListRequest extends OffsetPageRequest {
  roleCode?: string
  status?: number
}

export const listIdentityRoles = (data: RoleListRequest) =>
  request.post<PageResult<IdentityRole>>('/roles/list', toOffsetPayload(data))

export const createIdentityRole = (data: {
  roleCode: string
  roleName: string
  roleScope?: string
  status?: number
  remark?: string
}) => request.post<IdentityRole>('/roles/create', data)

export const updateIdentityRole = (data: {
  id: number
  roleName: string
  roleScope?: string
  status?: number
  remark?: string
}) => request.post<IdentityRole>('/roles/update', data)

export const grantIdentityRolePermissions = (data: {
  roleId: number
  permissionIds: number[]
}) => request.post<void>('/roles/grant-permissions', data)

export const getIdentityRolePermissionIds = (data: { roleId: number }) =>
  request.post<number[]>('/roles/permission-ids', data)

export interface PermissionListRequest extends OffsetPageRequest {
  resourceType?: string
  action?: string
  status?: number
}

export const listIdentityPermissions = (data: PermissionListRequest) =>
  request.post<PageResult<IdentityPermission>>('/permissions/list', toOffsetPayload(data))

export interface IdentityAuditEventListRequest extends OffsetPageRequest {
  operatorKeyword?: string
  eventType?: string
  resourceType?: string
  result?: number
}

export const listIdentityAuditEvents = (data: IdentityAuditEventListRequest) =>
  request.post<PageResult<IdentityAuditEvent>>('/audit/events/list', toOffsetPayload(data))
