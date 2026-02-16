export interface PlatformContractV1StepTrace {
  nodeKey?: string
  nodeType?: string
  nodeName?: string
  status?: string
  costMs?: number
  promptTokens?: number
  completionTokens?: number
  totalTokens?: number
  toolCallCount?: number
  toolDeniedCount?: number
  inputDigest?: string
  outputDigest?: string
  outputText?: string
  outputTruncated?: boolean
  approvalRequestId?: number
  errorMessage?: string
}

export interface PlatformContractV1 {
  meta?: {
    runId?: string
    agentCode?: string
    agentVersionId?: number
    agentVersionNo?: number
    orgId?: number
    modelUsed?: string
    costMs?: number
    repairAttempts?: number
    workflowId?: number
    workflowCode?: string
    workflowVersionId?: number
    workflowVersionNo?: number
    approvalRequestId?: number
    pendingToolKey?: string
    riskLevel?: string
  }
  status: string
  answer: string
  uncertainty?: string
  citations?: Array<{ title?: string; snippet?: string; source?: string }>
  toolCalls?: Array<{ toolKey?: string; summary?: string; resultSnippet?: string }>
  actionsNext?: string[]
  steps?: PlatformContractV1StepTrace[]
  error?: { code?: string; message?: string; detail?: string }
}

