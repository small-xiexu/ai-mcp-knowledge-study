import request from '@/utils/request'
import type { PageResult, PageRequest } from '@/types/api'

export interface ClientProfileStep {
  id?: number
  clientProfileId?: number
  sequenceNo: number
  stepName?: string
  modelId: number
  systemPrompt?: string
  enableTools?: boolean
  allowedToolKeysJson?: string
  createdAt?: string
  updatedAt?: string
}

export interface ClientProfile {
  id: number
  clientCode: string
  clientName: string
  description?: string
  status: string
  createdAt?: string
  updatedAt?: string
  steps?: ClientProfileStep[]
}

export const listClientProfiles = (data: PageRequest & { keyword?: string; status?: string }) =>
  request.post<PageResult<ClientProfile>>('/client-profiles/list', data)

export const getClientProfile = (id: number) =>
  request.post<ClientProfile>('/client-profiles/get', { id })

export const saveClientProfile = (data: {
  id?: number
  clientCode: string
  clientName: string
  description?: string
  status?: string
  steps?: ClientProfileStep[]
}) => request.post<ClientProfile>('/client-profiles/save', data)

export const enableClientProfile = (id: number) =>
  request.post<ClientProfile>('/client-profiles/enable', { id })

export const disableClientProfile = (id: number) =>
  request.post<ClientProfile>('/client-profiles/disable', { id })

export const removeClientProfile = (id: number) =>
  request.post<void>('/client-profiles/remove', { id })
