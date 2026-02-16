import request from '@/utils/request'
import type { PageResult } from '@/types/api'
import type { PlatformContractV1 } from '@/types/workflow'

export interface Workflow {
  id: number
  orgId: number
  workflowCode: string
  workflowName: string
  description?: string
  status: string
  currentPublishedVersionId?: number | null
  createdAt?: string
  updatedAt?: string
}

export interface WorkflowVersion {
  id: number
  orgId: number
  workflowId: number
  versionNo: number
  state: string
  changeSummary?: string
  graphJson?: string | null
  defaultConfigJson?: string | null
  createdAt?: string
  updatedAt?: string
}

export interface WorkflowRun {
  id: number
  orgId: number
  runId: string
  workflowId: number
  workflowCode: string
  workflowVersionId: number
  triggerSource?: string
  operatorId?: number | null
  operatorType?: string | null
  sessionId?: number | null
  status: string
  currentNodeKey?: string | null
  costMs?: number | null
  errorMessage?: string | null
  startedAt?: string
  endedAt?: string | null
}

export interface WorkflowNodeRun {
  id: number
  orgId: number
  runId: string
  nodeKey: string
  nodeType: string
  nodeName?: string | null
  status: string
  modelIdUsed?: number | null
  modelNameUsed?: string | null
  promptTokens?: number | null
  completionTokens?: number | null
  totalTokens?: number | null
  toolCallCount?: number | null
  toolDeniedCount?: number | null
  inputDigest?: string | null
  outputDigest?: string | null
  outputText?: string | null
  outputTruncated?: number | null
  approvalRequestId?: number | null
  costMs?: number | null
  errorMessage?: string | null
  startedAt?: string
  endedAt?: string | null
}

export const listWorkflows = (data: { keyword?: string; offset: number; pageSize: number }) =>
  request.post<PageResult<Workflow>>('/workflows/list', data)

export const getWorkflow = (id: number) =>
  request.post<Workflow>('/workflows/get', { id })

export const createWorkflow = (data: { workflowCode: string; workflowName: string; description?: string }) =>
  request.post<Workflow>('/workflows/create', data)

export const updateWorkflow = (data: { id: number; workflowName?: string; description?: string; status?: string }) =>
  request.post<Workflow>('/workflows/update', data)

export const createWorkflowVersion = (data: { workflowId: number; changeSummary?: string }) =>
  request.post<WorkflowVersion>('/workflows/versions/create', data)

export const listWorkflowVersions = (data: { workflowId: number }) =>
  request.post<WorkflowVersion[]>('/workflows/versions/list', data)

export const getWorkflowVersion = (id: number) =>
  request.post<WorkflowVersion>('/workflows/versions/get', { id })

export const publishWorkflowVersion = (data: { workflowVersionId: number }) =>
  request.post<WorkflowVersion>('/workflows/versions/publish', data)

export interface WorkflowGraphNode {
  nodeKey: string
  nodeType: string
  nodeName?: string
  configJson?: string
  positionX?: number
  positionY?: number
}

export interface WorkflowGraphEdge {
  sourceKey: string
  targetKey: string
  edgeType?: string
  conditionExpr?: string
}

export const saveWorkflowGraph = (data: {
  workflowVersionId: number
  graphJson?: string
  defaultConfigJson?: string
  nodes: WorkflowGraphNode[]
  edges: WorkflowGraphEdge[]
}) =>
  request.post<WorkflowVersion>('/workflows/versions/save-graph', data)

export const runWorkflow = (workflowCode: string, data: { sessionId?: number; content: string; variablesJson?: string; workflowVersionId?: number }) =>
  request.post<PlatformContractV1>(`/workflows/${encodeURIComponent(workflowCode)}/run`, data)

export const listWorkflowRuns = (data: { status?: string; offset: number; pageSize: number }) =>
  request.post<PageResult<WorkflowRun>>('/workflows/runs/list', data)

export const getWorkflowRun = (runId: string) =>
  request.post<WorkflowRun>('/workflows/runs/get', { runId })

export const listWorkflowNodeRuns = (data: { runId: string }) =>
  request.post<WorkflowNodeRun[]>('/workflows/runs/nodes', data)
