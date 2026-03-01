import request from '@/utils/request'
import type { PageRequest, PageResult } from '@/types/api'
import type {
  GatewayCredential,
  GatewayInstance,
  GatewayTool,
  SaveGatewayToolRequest,
  ToolDebugResult,
  ToolOption,
  ModelOption,
  ModelToolBindingState
} from '@/types/gateway'

export const listGatewayInstances = (data: PageRequest) =>
  request.post<PageResult<GatewayInstance>>('/gateway/manage/instances/list', data)

export const saveGatewayInstance = (data: GatewayInstance) =>
  request.post<GatewayInstance>('/gateway/manage/instances/save', data)

export const deleteGatewayInstance = (id: number) =>
  request.post<void>('/gateway/manage/instances/delete', { id })

export const listGatewayTools = (data: {
  gatewayId: string
  pageNum: number
  pageSize: number
  toolNameKeyword?: string
  toolDescriptionKeyword?: string
  status?: number
}) =>
  request.post<PageResult<GatewayTool>>('/gateway/manage/tools/list', data)

export const refreshGatewayTools = (data: {
  gatewayId?: string
  toolId?: number
}) =>
  request.post<{
    gatewayId: string
    refreshedCount: number
    successCount: number
    failedCount: number
    details: Array<{
      toolId: number
      toolName: string
      httpMethod: string
      httpUrl: string
      reachable: boolean
      message?: string
      error?: string
    }>
  }>('/gateway/manage/tools/refresh', data)

export const getGatewayTool = (id: number) =>
  request.post<any>('/gateway/manage/tools/get', { id })

export const saveGatewayTool = (data: SaveGatewayToolRequest) =>
  request.post<GatewayTool>('/gateway/manage/tools/save', data)

export const deleteGatewayTool = (id: number) =>
  request.post<void>('/gateway/manage/tools/delete', { id })

export const enableGatewayTool = (id: number) =>
  request.post<void>('/gateway/manage/tools/enable', { id })

export const disableGatewayTool = (id: number) =>
  request.post<void>('/gateway/manage/tools/disable', { id })

export const debugGatewayTool = (data: { gatewayId: string; toolName: string; arguments: Record<string, any> }) =>
  request.post<ToolDebugResult>('/gateway/manage/tools/debug', data)

export const listEnabledGatewayTools = () =>
  request.post<ToolOption[]>('/gateway/manage/tools/all-enabled')

export const listEnabledModels = () =>
  request.post<ModelOption[]>('/gateway/manage/models/enabled')

export const getModelToolBindings = (modelId: number) =>
  request.post<ModelToolBindingState>('/gateway/manage/bindings/model/get', { modelId })

export const saveModelToolBindings = (modelId: number, toolIds: number[]) =>
  request.post<void>('/gateway/manage/bindings/model/save', { modelId, toolIds })

export const listGatewayCredentials = (data: {
  gatewayId: string
  pageNum: number
  pageSize: number
  status?: number
  apiKeyKeyword?: string
}) => request.post<PageResult<GatewayCredential>>('/gateway/manage/auth/list', data)

export const saveGatewayCredential = (data: {
  id?: number
  gatewayId: string
  apiKey?: string
  rateLimit?: number
  expireTime?: string
  status?: number
}) => request.post<GatewayCredential>('/gateway/manage/auth/save', data)

export const enableGatewayCredential = (id: number) =>
  request.post<void>('/gateway/manage/auth/enable', { id })

export const disableGatewayCredential = (id: number) =>
  request.post<void>('/gateway/manage/auth/disable', { id })
