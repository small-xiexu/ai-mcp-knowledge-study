/**
 * 模型能力
 */
export interface ModelCapability {
  maxInputTokens: number
  maxOutputTokens: number
  supportFunctionCalling: boolean
  supportVision: boolean
  supportStreaming: boolean
  qualityScore: number
}

/**
 * 模型配置（响应）
 */
export interface ModelConfig {
  id: number
  modelName: string
  modelType: string
  baseUrl: string
  enabled: boolean
  toolEnabled?: boolean
  priority: number
  capability?: ModelCapability
  createdAt: string
  updatedAt: string
  activeChat?: boolean
  activeEmbedding?: boolean
}

/**
 * 模型能力（请求）
 */
export interface ModelCapabilityRequest {
  maxTokens?: number
  temperature?: number
  topP?: number
  qualityScore?: number
  speedScore?: number
  costScore?: number
}

/**
 * 模型配置（请求）
 */
export interface ModelConfigRequest {
  id?: number
  modelName: string
  modelType: string
  apiKey: string
  baseUrl: string
  enabled?: boolean
  toolEnabled?: boolean
  priority?: number
  capability?: ModelCapabilityRequest
}

/**
 * 模型信息
 */
export interface ModelInfo {
  modelId: number
  modelName: string
  modelType: string
  qualityScore?: number
  enabled?: boolean
  capability?: ModelCapability
}

/**
 * AI 请求
 */
export interface AIRequest {
  content: string
  taskType?: string
  systemPrompt?: string
  parameters?: Record<string, any>
  strategy?: string
  streaming?: boolean
  modelId?: number
  sessionId?: number
  ragTags?: string[]
}

/**
 * AI 响应
 */
export interface AIResponse {
  content: string
  modelUsed: string
  tokensUsed?: number
  responseTime: number
  success: boolean
  errorMessage?: string
  fallback?: boolean
  retryCount?: number
}

/**
 * 聊天会话
 */
export interface ChatSession {
  id: number
  title: string
  modelId?: number
  ragTags?: string[]
  createdAt: string
  updatedAt: string
}

/**
 * 聊天消息
 */
export interface ChatMessage {
  id: number
  sessionId: number
  role: string
  content: string
  modelId?: number
  promptTokens?: number
  completionTokens?: number
  totalTokens?: number
  createdAt: string
}

/**
 * 会话创建请求
 */
export interface ChatSessionCreateRequest {
  title?: string
  modelId?: number
  ragTags?: string[]
}

/**
 * 会话更新请求
 */
export interface ChatSessionUpdateRequest {
  id?: number
  title?: string
  modelId?: number
  ragTags?: string[]
}

/**
 * 消息创建请求
 */
export interface ChatMessageCreateRequest {
  role: string
  content: string
  modelId?: number
  promptTokens?: number
  completionTokens?: number
  totalTokens?: number
}

/**
 * RAG 任务
 */
export interface RagTask {
  taskId: string
  type: string
  status: string
  progress: number
  message?: string
  ragTag?: string
  createdAt?: string
  updatedAt?: string
}

/**
 * 调用次数统计
 */
export interface CallMetricsDTO {
  totalCalls: number
  successCalls: number
  failedCalls: number
  fallbackCalls: number
}

/**
 * 成功率统计
 */
export interface SuccessRateDTO {
  totalCalls: number
  successCalls: number
  successRate: number
}

/**
 * 响应时间统计
 */
export interface ResponseTimeDTO {
  avgResponseTime: number
  minResponseTime: number
  maxResponseTime: number
}

/**
 * 模型使用分布
 */
export interface ModelUsageDTO {
  modelId: number
  callCount: number
  usageRate: number
}

/**
 * 审计记录
 */
export interface ConfigAudit {
  id: number
  tableName: string
  recordId: number
  operation: string
  oldValue?: string
  newValue?: string
  operator?: string
  createdAt: string
}

/**
 * 任务类型
 */
export interface TaskType {
  id: number
  taskName: string
  taskCode: string
  description?: string
  preferredModelId: number
  preferredModelName?: string
  fallbackModelIds?: string
  createdAt: string
  updatedAt: string
}

/**
 * 任务类型请求
 */
export interface TaskTypeRequest {
  id?: number
  taskName: string
  taskCode: string
  description?: string
  preferredModelId: number
  fallbackModelIds?: string
}

/**
 * MCP Server 配置（响应）
 */
export interface McpServerConfig {
  id: number
  serverName: string
  serverType: string
  enabled: boolean
  description?: string
  command?: string
  args?: string[]
  env?: Record<string, string>
  endpoint?: string
  sseEndpoint?: string
  headers?: Record<string, string>
  connectTimeoutMs?: number
  requestTimeoutMs?: number
  initTimeoutMs?: number
  running?: boolean
  createdAt?: string
  updatedAt?: string
}

/**
 * MCP Server 配置（请求）
 */
export interface McpServerConfigRequest {
  id?: number
  serverName: string
  serverType: string
  enabled?: boolean
  description?: string
  command?: string
  args?: string[]
  env?: Record<string, string>
  endpoint?: string
  sseEndpoint?: string
  headers?: Record<string, string>
  connectTimeoutMs?: number
  requestTimeoutMs?: number
  initTimeoutMs?: number
}
