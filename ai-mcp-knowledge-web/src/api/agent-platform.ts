import request from '@/utils/request'
import type { PageResult, PageRequest } from '@/types/api'
import type { PlatformContractV1 } from '@/types/workflow'

export interface Agent {
  id: number
  agentCode: string
  agentName: string
  description?: string
  channel?: string
  status: string
  currentPublishedVersionId?: number
  createdAt?: string
  updatedAt?: string
}

export interface AgentVersion {
  id: number
  agentId: number
  versionNo: number
  state: string
  changeSummary?: string
  promptTemplateId?: number
  promptTemplateVersionNo?: number
  templateParamsJson?: string
  systemPromptSnapshot?: string
  workflowVersionId?: number
  outputContractVersion?: string
  outputContractOptionsJson?: string
  ragMode?: string
  defaultRagTagsJson?: string
  allowedRagTagsJson?: string
  allowedToolKeysJson?: string
  clientProfileId?: number
  clientChainJson?: string
  timeoutMs?: number
  maxTurns?: number
  temperature?: number
  repairRetryTimes?: number
  createdAt?: string
  updatedAt?: string
}

export interface PromptTemplate {
  id: number
  templateCode: string
  templateName: string
  versionNo: number
  state: string
  content: string
  variableSpecJson?: string
  createdAt?: string
  updatedAt?: string
}

export interface ApprovalRequest {
  id: number
  runId: string
  agentCode: string
  agentVersionId: number
  toolKey: string
  riskLevel: string
  status: string
  expireAt?: string
  createdAt?: string
  updatedAt?: string
}

export interface AgentSchedule {
  id: number
  agentId: number
  agentCode?: string
  cron: string
  enabled: boolean
  xxlJobId?: number
  payloadTemplateJson?: string
  createdAt?: string
  updatedAt?: string
}

// 统一使用 `src/types/workflow.d.ts` 中的 contract 定义（Agent/Workflow 都会返回 steps）。
export type { PlatformContractV1 }

export const listAgents = (data: PageRequest & { keyword?: string; status?: string }) =>
  request.post<PageResult<Agent>>('/agents/list', data)

export const getAgent = (agentCode: string) =>
  request.post<Agent>('/agents/get', { agentCode })

export const createAgent = (data: { agentCode: string; agentName: string; description?: string; channel?: string; status?: string }) =>
  request.post<Agent>('/agents/create', data)

export const updateAgent = (data: { agentCode: string; agentName: string; description?: string; channel?: string; status?: string }) =>
  request.post<Agent>('/agents/update', data)

export const removeAgent = (agentCode: string) =>
  request.post<void>('/agents/remove', { agentCode })

export const listAgentVersions = (data: { agentCode: string; pageNum: number; pageSize: number }) =>
  request.post<PageResult<AgentVersion>>('/agent-versions/list', data)

export const getAgentVersion = (id: number) =>
  request.post<AgentVersion>('/agent-versions/get', { id })

export const saveAgentVersionDraft = (data: any) =>
  request.post<AgentVersion>('/agent-versions/draft/save', data)

export const publishAgentVersion = (data: { agentCode: string; versionId: number }) =>
  request.post<AgentVersion>('/agent-versions/publish', data)

export const rollbackAgentVersion = (data: { agentCode: string; targetVersionId: number }) =>
  request.post<AgentVersion>('/agent-versions/rollback', data)

export const listTemplates = (data: PageRequest & { keyword?: string; state?: string }) =>
  request.post<PageResult<PromptTemplate>>('/templates/list', data)

export const getTemplate = (id: number) =>
  request.post<PromptTemplate>('/templates/get', { id })

export const createTemplate = (data: any) =>
  request.post<PromptTemplate>('/templates/create', data)

export const updateTemplate = (data: any) =>
  request.post<PromptTemplate>('/templates/update', data)

export const publishTemplate = (data: { id: number }) =>
  request.post<PromptTemplate>('/templates/publish', data)

export const archiveTemplate = (data: { id: number }) =>
  request.post<PromptTemplate>('/templates/archive', data)

export const listApprovals = (data: { status?: string; pageNum: number; pageSize: number }) =>
  request.post<PageResult<ApprovalRequest>>('/approvals/list', {
    status: data.status,
    offset: (Math.max(data.pageNum || 1, 1) - 1) * Math.max(data.pageSize || 20, 1),
    pageSize: data.pageSize
  })

export const getApproval = (id: number) =>
  request.post<ApprovalRequest>('/approvals/get', { id })

export const approveTool = (data: { id: number; decisionComment?: string }) =>
  request.post<PlatformContractV1>('/approvals/approve', data)

export const rejectTool = (data: { id: number; decisionComment?: string }) =>
  request.post<ApprovalRequest>('/approvals/reject', data)

export const listSchedules = (data: { agentCode?: string; enabled?: boolean; pageNum: number; pageSize: number }) =>
  request.post<PageResult<AgentSchedule>>('/schedules/list', data)

export const getSchedule = (id: number) =>
  request.post<AgentSchedule>('/schedules/get', { id })

export const createSchedule = (data: { agentCode: string; cron: string; enabled?: boolean; payloadTemplateJson?: string }) =>
  request.post<AgentSchedule>('/schedules/create', data)

export const updateSchedule = (data: { id: number; agentCode: string; cron: string; payloadTemplateJson?: string }) =>
  request.post<AgentSchedule>('/schedules/update', data)

export const enableSchedule = (id: number) =>
  request.post<AgentSchedule>('/schedules/enable', { id })

export const disableSchedule = (id: number) =>
  request.post<AgentSchedule>('/schedules/disable', { id })

export const removeSchedule = (id: number) =>
  request.post<void>('/schedules/remove', { id })

export const agentChat = (agentCode: string, data: { sessionId?: number; content: string; ragTagsJson?: string }) =>
  request.post<PlatformContractV1>(`/agents/${agentCode}/chat`, data)
