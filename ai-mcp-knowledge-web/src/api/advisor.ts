import request from '@/utils/request'
import type { PageResult, PageRequest } from '@/types/api'

export interface Advisor {
  id: number
  orgId: number
  advisorCode: string
  advisorName: string
  advisorType: string
  enabled: number
  configJson?: string
  createdAt?: string
  updatedAt?: string
}

export interface AdvisorBindingView {
  bindingId: number
  orgId: number
  bindType: string
  bindTargetId: number
  advisorId: number
  orderNo: number
  bindingEnabled: number
  advisorCode: string
  advisorName: string
  advisorType: string
  advisorEnabled: number
  advisorConfigJson?: string
}

export const listAdvisors = (data: PageRequest & { keyword?: string; enabled?: boolean; advisorType?: string }) =>
  request.post<PageResult<Advisor>>('/advisors/list', data)

export const getAdvisor = (id: number) =>
  request.post<Advisor>('/advisors/get', { id })

export const saveAdvisor = (data: { id?: number; advisorCode: string; advisorName: string; advisorType: string; enabled?: boolean; configJson?: string }) =>
  request.post<Advisor>('/advisors/save', data)

export const enableAdvisor = (id: number) =>
  request.post<Advisor>('/advisors/enable', { id })

export const disableAdvisor = (id: number) =>
  request.post<Advisor>('/advisors/disable', { id })

export const removeAdvisor = (id: number) =>
  request.post<void>('/advisors/remove', { id })

export const listAdvisorBindings = (data: { bindType: string; bindTargetId: number }) =>
  request.post<AdvisorBindingView[]>('/advisors/bindings/list', data)

export const saveAdvisorBindings = (data: {
  bindType: string
  bindTargetId: number
  items: Array<{ advisorId: number; orderNo?: number; enabled?: boolean }>
}) => request.post<void>('/advisors/bindings/save', data)

