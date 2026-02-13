import request from '@/utils/request'
import type { PageResult } from '@/types/api'
import type {
  IdentityApiKey,
  IdentityApiKeyCreateResult,
  IdentityAuditEvent,
  IdentityOrg,
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
  tenantId?: string
  username?: string
  status?: number
}

export const listIdentityUsers = (data: UserListRequest) =>
  request.post<PageResult<IdentityUser>>('/users/list', toOffsetPayload(data))

export const createIdentityUser = (data: {
  tenantId?: string
  username: string
  displayName: string
  password: string
  email?: string
  mobile?: string
  status?: number
  superAdmin?: boolean
}) => request.post<IdentityUser>('/users/create', data)

export const grantIdentityUserRoles = (data: {
  userId: number
  tenantId?: string
  roleIds: number[]
}) => request.post<void>('/users/grant-roles', data)

export const getIdentityUserRoleIds = (data: { userId: number; tenantId?: string }) =>
  request.post<number[]>('/users/role-ids', data)

export interface RoleListRequest extends OffsetPageRequest {
  tenantId?: string
  roleCode?: string
  status?: number
}

export const listIdentityRoles = (data: RoleListRequest) =>
  request.post<PageResult<IdentityRole>>('/roles/list', toOffsetPayload(data))

export const createIdentityRole = (data: {
  tenantId?: string
  roleCode: string
  roleName: string
  roleScope?: string
  status?: number
  remark?: string
}) => request.post<IdentityRole>('/roles/create', data)

export const updateIdentityRole = (data: {
  id: number
  tenantId?: string
  roleName: string
  roleScope?: string
  status?: number
  remark?: string
}) => request.post<IdentityRole>('/roles/update', data)

export const grantIdentityRolePermissions = (data: {
  roleId: number
  tenantId?: string
  permissionIds: number[]
}) => request.post<void>('/roles/grant-permissions', data)

export const getIdentityRolePermissionIds = (data: { roleId: number; tenantId?: string }) =>
  request.post<number[]>('/roles/permission-ids', data)

export interface PermissionListRequest extends OffsetPageRequest {
  resourceType?: string
  action?: string
  status?: number
}

export const listIdentityPermissions = (data: PermissionListRequest) =>
  request.post<PageResult<IdentityPermission>>('/permissions/list', toOffsetPayload(data))

export const listIdentityOrgs = (data?: { tenantId?: string; status?: number }) =>
  request.post<IdentityOrg[]>('/orgs/list', data || {})

export const createIdentityOrg = (data: {
  tenantId?: string
  orgCode: string
  orgName: string
  parentId?: number
  orgPath?: string
  status?: number
  remark?: string
}) => request.post<IdentityOrg>('/orgs/create', data)

export const updateIdentityOrg = (data: {
  id: number
  tenantId?: string
  orgName: string
  parentId?: number
  orgPath?: string
  status?: number
  remark?: string
}) => request.post<IdentityOrg>('/orgs/update', data)

export const bindIdentityUserOrg = (data: {
  userId: number
  orgId: number
  tenantId?: string
}) => request.post<void>('/orgs/bind-user', data)

export interface ApiKeyListRequest extends OffsetPageRequest {
  tenantId?: string
  ownerUserId?: number
  status?: number
}

export const listIdentityApiKeys = (data: ApiKeyListRequest) =>
  request.post<PageResult<IdentityApiKey>>('/apikeys/list', toOffsetPayload(data))

export const createIdentityApiKey = (data?: {
  tenantId?: string
  ownerUserId?: number
  scopes?: string[]
  expireAt?: string
}) => request.post<IdentityApiKeyCreateResult>('/apikeys/create', data || {})

export const revokeIdentityApiKey = (data: { id: number; tenantId?: string }) =>
  request.post<void>('/apikeys/revoke', data)

export interface IdentityAuditEventListRequest extends OffsetPageRequest {
  tenantId?: string
  operatorId?: number
  eventType?: string
  resourceType?: string
  result?: number
}

export const listIdentityAuditEvents = (data: IdentityAuditEventListRequest) =>
  request.post<PageResult<IdentityAuditEvent>>('/audit/events/list', toOffsetPayload(data))
