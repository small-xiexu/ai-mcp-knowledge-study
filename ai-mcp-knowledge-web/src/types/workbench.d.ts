export interface WorkbenchGuideStep {
  key: string
  title: string
  status: 'DONE' | 'TODO' | 'BLOCKED' | string
  message?: string
  actionPath?: string
  actionLabel?: string
  writeAction?: boolean
}

export interface WorkbenchSummary {
  model?: {
    total?: number
    enabled?: number
    activeChatModelId?: number | null
    activeEmbeddingModelId?: number | null
  }
  agent?: {
    total?: number
    published?: number
  }
  prompt?: {
    draft?: number
    published?: number
  }
  tool?: {
    approvalsPending?: number
  }
  schedule?: {
    total?: number
    enabled?: number
  }
  knowledge?: {
    ragTagCount?: number
    ragTaskTotal?: number
    ragTaskProcessing?: number
    ragTaskFailedRecent?: number
  }
  todo?: {
    writeBlockedForSuperAdmin?: boolean
  }
  guideSteps?: WorkbenchGuideStep[]
}
