import request from '@/utils/request'
import type { PageResult, PageRequest } from '@/types/api'

export interface AgentEnhancer {
  id: number
  agentEnhancerCode: string
  agentEnhancerName: string
  agentEnhancerType: string
  enabled: number
  configJson?: string
  createdAt?: string
  updatedAt?: string
}

export interface AgentEnhancerBindingView {
  bindingId: number
  bindType: string
  bindTargetId: number
  agentEnhancerId: number
  orderNo: number
  bindingEnabled: number
  agentEnhancerCode: string
  agentEnhancerName: string
  agentEnhancerType: string
  agentEnhancerEnabled: number
  agentEnhancerConfigJson?: string
}

export const listAgentEnhancers = (data: PageRequest & { keyword?: string; enabled?: boolean; agentEnhancerType?: string }) =>
  request.post<PageResult<AgentEnhancer>>('/agent-enhancers/list', data)

export const getAgentEnhancer = (id: number) =>
  request.post<AgentEnhancer>('/agent-enhancers/get', { id })

export const saveAgentEnhancer = (data: { id?: number; agentEnhancerCode: string; agentEnhancerName: string; agentEnhancerType: string; enabled?: boolean; configJson?: string }) =>
  request.post<AgentEnhancer>('/agent-enhancers/save', data)

export const enableAgentEnhancer = (id: number) =>
  request.post<AgentEnhancer>('/agent-enhancers/enable', { id })

export const disableAgentEnhancer = (id: number) =>
  request.post<AgentEnhancer>('/agent-enhancers/disable', { id })

export const removeAgentEnhancer = (id: number) =>
  request.post<void>('/agent-enhancers/remove', { id })

export const listAgentEnhancerBindings = (data: { bindType: string; bindTargetId: number }) =>
  request.post<AgentEnhancerBindingView[]>('/agent-enhancers/bindings/list', data)

export const saveAgentEnhancerBindings = (data: {
  bindType: string
  bindTargetId: number
  items: Array<{ agentEnhancerId: number; orderNo?: number; enabled?: boolean }>
}) => request.post<void>('/agent-enhancers/bindings/save', data)
