import request from '@/utils/request'

export interface PreheatResponse {
  targetType: string
  targetId: number
  mcpRefreshed: boolean
  toolCallbacksWarmed: boolean
  advisorsWarmed: boolean
  workflowValidated: boolean
  warnings?: string[]
}

export const preheatAgentVersion = (data: { agentVersionId: number; refreshMcp?: boolean }) =>
  request.post<PreheatResponse>('/preheat/agent-version', data)

export const preheatWorkflowVersion = (data: { workflowVersionId: number; refreshMcp?: boolean }) =>
  request.post<PreheatResponse>('/preheat/workflow-version', data)
